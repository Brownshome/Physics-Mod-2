package brownshome.physicsmod.storage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import javax.vecmath.Matrix3f;
import javax.vecmath.Vector3f;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.texture.ITickable;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import brownshome.physicsmod.PhysicsHelper;
import brownshome.physicsmod.network.PhysicsNetClient;

import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;

@SideOnly(Side.CLIENT)
public class SegmentWorldClient extends WorldClient implements ISegment {
	public static float VELOCITY_SNAP_THRESH = Float.MAX_VALUE;
	public static float ANGULAR_VEL_SNAP_THRESH = Float.MAX_VALUE;
	public static float POSITION_SNAP_THRESH = Float.MAX_VALUE;
	public static float ORIENTATION_SNAP_THRESH = Float.MAX_VALUE;
	
	public static int FRAMES_TO_ADJUST = 10;
	
	static Map<Integer, SegmentWorldClient> WORLDS = new HashMap<>();
	public static Map<Integer, DynamicsWorld> bulletWorlds = new HashMap<>();
	static float lastTime = Float.NaN;
	public static boolean physTick = false;
	public static boolean link = false;
	
	public static void rayCast() {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		Vec3 eyePos = player.getPositionEyes(1.0f);
		Vec3 lookVec = player.getLook(1.0f);

		double dist = Minecraft.getMinecraft().playerController.getBlockReachDistance();
		Vec3 to = eyePos.addVector(lookVec.xCoord * dist, lookVec.yCoord * dist, lookVec.zCoord * dist);

		double min = Double.POSITIVE_INFINITY;
		SegmentWorldClient hitWorld = null;
		
		if(Minecraft.getMinecraft().objectMouseOver != null && Minecraft.getMinecraft().objectMouseOver.typeOfHit != MovingObjectType.MISS)
			min = Minecraft.getMinecraft().objectMouseOver.hitVec.squareDistanceTo(eyePos);
		
		for(SegmentWorldClient world : getWorlds()) {
			Vec3 e = world.worldToSeg(eyePos);
			
			world.rayTrace = world.rayTraceBlocks(world.worldToSeg(eyePos), world.worldToSeg(to), false, false, true);
			
			if(world.rayTrace.typeOfHit == MovingObjectType.BLOCK) {
				double d = e.squareDistanceTo(world.rayTrace.hitVec);
			
				if(d < min) {
					hitWorld = world;
					min = d;
				}
			}
			
			world.rayTrace.typeOfHit = MovingObjectType.MISS;
		}
		
		if(hitWorld != null) {
			Minecraft.getMinecraft().objectMouseOver.typeOfHit = MovingObjectType.MISS;
			hitWorld.rayTrace.typeOfHit = MovingObjectType.BLOCK;
		}
	}
	
	static boolean hacked = false;
	@SuppressWarnings("unchecked")
	public static void tickSegmentsPre() {
		if(!hacked) {
			
			//a bit of a hack, the renderEngine.tick() just happened to be just where I wanted this to be
			Minecraft.getMinecraft().renderEngine.listTickables.add((ITickable) SegmentWorldClient::rayCast);
			hacked = true;
		}
		
		getWorlds().forEach(SegmentWorldClient::tick);
	}

	public static void tickSegmentsPost() {
		for(SegmentWorldClient world : getWorlds()) {
			world.postTick();
		}
	}
	
	public static boolean rightClick() {
		PlayerControllerMP playerController = Minecraft.getMinecraft().playerController;
		EntityPlayerSP player = Minecraft.getMinecraft().thePlayer;
		ItemStack heldItem = player.getHeldItem();
		boolean cancel = false;

		for(SegmentWorldClient world : getWorlds()) {
			if(world.getRay().typeOfHit == MovingObjectType.BLOCK) {
				BlockPos blockpos = world.getRay().getBlockPos();

				if (!world.isAirBlock(blockpos)) {
					int i = heldItem != null ? heldItem.stackSize : 0;

					boolean result = !net.minecraftforge.event.ForgeEventFactory.onPlayerInteract(player, net.minecraftforge.event.entity.player.PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK, world, blockpos, world.getRay().sideHit).isCanceled();
					if (result) {
						if (world.handleRightClick(playerController, player, heldItem, blockpos, world.getRay().sideHit, world.getRay().hitVec)) {
							cancel = true;
							player.swingItem();
						}
					}

					if (heldItem == null)
						break;

					if (heldItem.stackSize == 0) {
						player.inventory.mainInventory[player.inventory.currentItem] = null;
					} else if (heldItem.stackSize != i || playerController.isInCreativeMode()) {
						Minecraft.getMinecraft().entityRenderer.itemRenderer.resetEquippedProgress();
					}

					break;
				}
			}
		}

		return cancel;
	}
	
	public static void updatePhysMesh(int dim) {
		getBulletWorld(dim);
		
		//TODO update world mesh, possibly using callbacks to block placement
	}
	
	public static void stepBulletWorld(int dim) {		
		if(Float.isNaN(lastTime))
			lastTime = Minecraft.getSystemTime() / 1000f;
		
		float time = Minecraft.getSystemTime() / 1000f;
		
		if(physTick)
			getBulletWorld(dim).stepSimulation(time - lastTime, 10);
		
		lastTime = time;
	}
	
	WorldClient parent;
	RigidBody body;
	int id;
	ViewFrustum viewFrustum;
	PhysicsNetClient netHandler;
	MovingObjectPosition rayTrace;
	boolean hadFirstUpdate = false;
	
	//variables to handle server updates
	Vector3f serverVelocityDelta;
	Vector3f serverAngleVelocityDelta;
	Vector3f serverPositionDelta;
	Matrix3f serverOrientationDelta;
	int framesSince = 0;
	
	//physics update
	List<AxisAlignedBB> invalidAreas = new ArrayList<>();
	List<BlockPos> blocks = new ArrayList<>();
	boolean bodyAdded = false;
	
	Map<Entity, Boolean> checkedEntities = new HashMap<>();
	
	public List<BlockPos> getBlocks() { return blocks; }

	public SegmentWorldClient(WorldClient parent, int id) {
		super(null, new WorldSettings(parent.getWorldInfo()), 0, parent.getDifficulty(), parent.theProfiler);
		netHandler = new PhysicsNetClient(this);
		this.id = id;
		this.parent = parent;
		body = createJBulletBody(0, 0, 0);
	}

	public boolean handleRightClick(PlayerControllerMP controller, EntityPlayerSP player, ItemStack heldItem, BlockPos blockPos, EnumFacing face, Vec3 impact) {
		controller.syncCurrentPlayItem();
		float f = (float)(impact.xCoord - (double)blockPos.getX());
		float f1 = (float)(impact.yCoord - (double)blockPos.getY());
		float f2 = (float)(impact.zCoord - (double)blockPos.getZ());
		boolean flag = false;

		if (controller.getCurrentGameType() != WorldSettings.GameType.SPECTATOR) {
			if (heldItem != null && heldItem.getItem() != null && heldItem.getItem().onItemUseFirst(heldItem, player, this, blockPos, face, f, f1, f2)) {
				return true;
			}

			IBlockState iblockstate = getBlockState(blockPos);

			if ((!player.isSneaking() || player.getHeldItem() == null || player.getHeldItem().getItem().doesSneakBypassUse(this, blockPos, player))) {
				flag = iblockstate.getBlock().onBlockActivated(this, blockPos, iblockstate, player, face, f, f1, f2);
			}

			if (!flag && heldItem != null && heldItem.getItem() instanceof ItemBlock) {
				ItemBlock itemblock = (ItemBlock)heldItem.getItem();

				if (!itemblock.canPlaceBlockOnSide(this, blockPos, face, player, heldItem))
					return false;
			}
		}

		netHandler.addToSendQueue(new C08PacketPlayerBlockPlacement(blockPos, face.getIndex(), player.inventory.getCurrentItem(), f, f1, f2));

		if (!flag && controller.getCurrentGameType() != WorldSettings.GameType.SPECTATOR) {
			if (heldItem == null)
				return false;

			if (controller.isInCreativeMode()) {
				int i = heldItem.getMetadata();
				int j = heldItem.stackSize;
				boolean flag1 = heldItem.onItemUse(player, this, blockPos, face, f, f1, f2);
				heldItem.setItemDamage(i);
				heldItem.stackSize = j;
				return flag1;
			} else {
				if (!heldItem.onItemUse(player, this, blockPos, face, f, f1, f2)) 
					return false;
				
				if (heldItem.stackSize <= 0)
					net.minecraftforge.event.ForgeEventFactory.onPlayerDestroyItem(player, heldItem);
				
				return true;
			}
		} else {
			return true;
		}
	}
	
	@SideOnly(Side.CLIENT)
	public ViewFrustum getViewFrustum() {
		if(viewFrustum == null)
			viewFrustum = new ViewFrustum(this, Minecraft.getMinecraft().gameSettings.renderDistanceChunks, Minecraft.getMinecraft().renderGlobal, Minecraft.getMinecraft().renderGlobal.renderChunkFactory);

		return viewFrustum;
	}

	//ENTITY CODE
	
	/** Added / removes / updates true false flag of the checkedEntities box */
	public void populateCheckedEntities() {
		checkedEntities.clear();
		
		for(Object e : getParent().getEntities(EntityPlayer.class, e -> true))
			checkedEntities.put((Entity) e, false);
	}
	
	public Map<Entity, Boolean> getCheckedEntities() {
		return checkedEntities;
	}
	
	//END ENTITY CODE
	
	public MovingObjectPosition getRay() {
		return rayTrace;
	}

	@Override
	public World getParent() {
		return parent;
	}
	
	public NetHandlerPlayClient getNetHandler() {
		return netHandler;
	}

	@Override
	public RigidBody getRigidBody() {
		return body;
	}

	@Override
	public int getIDNumber() {
		return id;
	}
	
	public void invalidateBlockReceiveRegion(int x0, int y0, int z0, int x1, int y1, int z1) {
		invalidAreas.add(new AxisAlignedBB(x0, y0, z0, x1, y1, z1));
	}
	
	/*public boolean setBlockState(BlockPos pos, IBlockState newState, int flags) {
		boolean b = super.setBlockState(pos, newState, flags);
		if(newState.getBlock() != Blocks.air && added.add(pos)) 
			addCube(pos, newState);
		
		return b;
	}*/
	
	public static SegmentWorldClient getWorld(int ID) {
		assert ID > 1 : "Illegal ID value for segment";
		
		SegmentWorldClient world = null;
		
		world = WORLDS.get(ID);
		
		if(world == null) {
			world = new SegmentWorldClient(Minecraft.getMinecraft().theWorld, ID);
			WORLDS.put(ID, world);
		}
		
		return world;
	}
	
	public boolean checkNoEntityCollision(AxisAlignedBB boundingBox, Entity p_72917_2_) {
		if(!getParent().checkNoEntityCollision(BBsegToWorld(boundingBox), p_72917_2_))
			return false;
		
		List<?> list = this.getEntitiesWithinAABBExcludingEntity((Entity)null, boundingBox);
		
		for (int i = 0; i < list.size(); ++i) {
			Entity entity1 = (Entity)list.get(i);
			
			if (!entity1.isDead && entity1.preventEntitySpawning && entity1 != p_72917_2_ && (p_72917_2_ == null || p_72917_2_.ridingEntity != entity1 && p_72917_2_.riddenByEntity != entity1)) {
				return false;
			}
		}
		
		return true;
	}
	
	public static Collection<SegmentWorldClient> getWorlds() {
		return WORLDS.values().stream().filter(w -> w.hadFirstUpdate).collect(Collectors.toList());
	}
	
	public static DynamicsWorld getBulletWorld(int dim) {
		if(!bulletWorlds.containsKey(dim))
			bulletWorlds.put(dim, PhysicsHelper.createBulletWorld(dim));
		
		return bulletWorlds.get(dim);
	}
	
	@Override
	public DynamicsWorld getBulletWorld() {
		return getBulletWorld(getParent().provider.getDimensionId());
	}
	
	public void serverUpdate(Transform position, Vector3f velocity, Vector3f angularVelocity) {
		DefaultMotionState state = (DefaultMotionState) body.getMotionState();
		Transform invCOM = new Transform();
		invCOM.inverse(state.centerOfMassOffset);
		position.mul(invCOM);
		
		if(!hadFirstUpdate) {
			body.setWorldTransform(position);
			body.setLinearVelocity(velocity);
			body.setAngularVelocity(angularVelocity);
			
			hadFirstUpdate = true;
		} else {
			if(!link)
				return;
			
			Transform clientTrans = body.getWorldTransform(new Transform());
			Vector3f clientVel = body.getLinearVelocity(new Vector3f());
			Vector3f clientAngularVel = body.getAngularVelocity(new Vector3f());
			
			Vector3f tmp = new Vector3f();
			//TODO adjust for time of arival
			
			tmp.sub(velocity, clientVel);
			if(tmp.lengthSquared() > VELOCITY_SNAP_THRESH) {
				body.setLinearVelocity(velocity);
				serverVelocityDelta = null;
			} else {
				serverVelocityDelta = new Vector3f(tmp);
				serverVelocityDelta.scale(1f / FRAMES_TO_ADJUST);
			}
			
			tmp.sub(angularVelocity, clientAngularVel);
			if(tmp.lengthSquared() > ANGULAR_VEL_SNAP_THRESH) {
				body.setAngularVelocity(angularVelocity);
				serverAngleVelocityDelta = null;
			} else {
				serverAngleVelocityDelta = new Vector3f(tmp);
				serverAngleVelocityDelta.scale(1f / FRAMES_TO_ADJUST);
			}
			
			Transform t = new Transform();
			tmp.sub(position.origin, clientTrans.origin);
			if(tmp.lengthSquared() > POSITION_SNAP_THRESH) {
				body.getWorldTransform(t);
				t.origin.set(position.origin);
				body.setWorldTransform(t);
				serverPositionDelta = null;
			} else {
				serverPositionDelta = new Vector3f(tmp);
				serverPositionDelta.scale(1f / FRAMES_TO_ADJUST);
			}
			
			if(!position.basis.epsilonEquals(clientTrans.basis, ORIENTATION_SNAP_THRESH)) {
				body.getWorldTransform(t);
				t.basis.set(position.basis);
				body.setWorldTransform(t);
				
				serverOrientationDelta = null;
			} else {
				Matrix3f m = new Matrix3f();
				m.invert(clientTrans.basis);
				m.mul(position.basis);
				
				m.mul(1f / FRAMES_TO_ADJUST);
				Matrix3f id = new Matrix3f();
				id.setIdentity();
				id.mul((FRAMES_TO_ADJUST - 1f) / FRAMES_TO_ADJUST);
				m.add(id);
				
				serverOrientationDelta = m;
			}
		}
		
		body.getMotionState().setWorldTransform(body.getWorldTransform(new Transform()));
	}
	
	public void postTick() {
		populateCheckedEntities();
		adjustEntities();
	}
	
	HashSet<BlockPos> added = new HashSet<>();//TODO
	public void tick() {
		super.tick();
		
		List<BlockPos> toAdd = Collections.synchronizedList(new ArrayList<>());
		
		AtomicBoolean update = new AtomicBoolean(false);
		invalidAreas.parallelStream().forEach(c -> {
			for(Object o : BlockPos.getAllInBox(new BlockPos(c.minX, c.minY, c.minZ), new BlockPos(c.maxX, c.maxY, c.maxZ))) {
				BlockPos pos = (BlockPos) o;
				IBlockState state = getBlockState(pos);
				Block block = state.getBlock();
				
				if(!block.isAir(this, pos) && added.add(pos)) {
					update.set(true);
					toAdd.add(pos);
				}
			}
		});
		
		invalidAreas.clear();
		
		for(BlockPos p : toAdd) {
			addCube(p, getBlockState(p));
			
			if(!bodyAdded)
				getBulletWorld().addRigidBody(getRigidBody());
		
			bodyAdded = true;
		}
		
		RigidBody body = getRigidBody();
		
		Vector3f tmp = new Vector3f();
		
		framesSince++;
		if(serverVelocityDelta != null) {
			body.getLinearVelocity(tmp);
			tmp.add(serverVelocityDelta);
			getRigidBody().setLinearVelocity(tmp);
			
			if(framesSince == FRAMES_TO_ADJUST)
				serverVelocityDelta = null;
		}
	
		if(serverAngleVelocityDelta != null) {
			body.getAngularVelocity(tmp);
			tmp.add(serverAngleVelocityDelta);
			getRigidBody().setAngularVelocity(tmp);
			
			if(framesSince == FRAMES_TO_ADJUST)
				serverAngleVelocityDelta = null;
		}
		
		Transform t = new Transform();
		if(serverPositionDelta != null) {
			body.getWorldTransform(t);
			t.origin.add(serverPositionDelta);
			body.setWorldTransform(t);
			
			if(framesSince == FRAMES_TO_ADJUST)
				serverPositionDelta = null;
		}
		
		if(serverOrientationDelta != null) {
			body.getWorldTransform(t);
			t.basis.mul(serverOrientationDelta);
			t.basis.normalizeCP();
			body.setWorldTransform(t);
			
			if(framesSince == FRAMES_TO_ADJUST)
				serverOrientationDelta = null;
		}
		
		//TODO find out why this is neccesarry
		if(update.get()) {
			getBulletWorld().removeRigidBody(body);
			getBulletWorld().addRigidBody(body);
		}
	}
}