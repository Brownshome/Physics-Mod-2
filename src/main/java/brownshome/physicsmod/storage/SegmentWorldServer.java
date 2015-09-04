package brownshome.physicsmod.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.vecmath.Vector3f;

import net.minecraft.block.Block;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockWall;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Blocks;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ReportedException;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraftforge.common.DimensionManager;
import brownshome.physicsmod.PhysicsHelper;
import brownshome.physicsmod.PhysicsMod;
import brownshome.physicsmod.entity.PlayerGhost;
import brownshome.physicsmod.network.SegmentStatePacket;

import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

public class SegmentWorldServer extends WorldServer implements ISegment {
	public static Map<Integer, DynamicsWorld> bulletWorlds = new HashMap<>();
	
	static float lastTime = Float.NaN;
	static WorldServer parentTmp;
	public static boolean physTick = false;
	
	public static void updatePhysMesh(int dim) {
		getBulletWorld(dim);
		
		//TODO update world mesh, possibly using callbacks to block placement
	}
	
	public static DynamicsWorld getBulletWorld(int dim) {
		if(!bulletWorlds.containsKey(dim))
			bulletWorlds.put(dim, PhysicsHelper.createBulletWorld(dim));
		
		return bulletWorlds.get(dim);
	}
	
	public static void stepBulletWorld(int dim) {

		if(Float.isNaN(lastTime))
			lastTime = Minecraft.getSystemTime() / 1000f;
		
		float time = Minecraft.getSystemTime() / 1000f;
		
		if(physTick)
			getBulletWorld(dim).stepSimulation(time - lastTime, 10);
		
		lastTime = time;
	}
	
	DummyEntityTracker dummyTracker;
	WorldServer parent;
	RigidBody body;
	BlockPos root;
	List<BlockPos> blocks = new ArrayList<>();
	boolean bodyAdded = false;
	boolean reAddBody = false;
	
	/** This is a map of all entities that might collide with the segment, if they do there positions are adjusted. If the entity has it's poisition
	goverend by the segment the boolean will be true. This will occur if the segment and the entity are moving at similar speeds, are off the ground
	and are close to one another */
	Map<Entity, Boolean> checkedEntities = new HashMap<>();
	
	public List<BlockPos> getBlocks() { return blocks; }

	private static int setDimensionUp(WorldServer parent) {
		int ID = DimensionManager.getNextFreeDimId();
		DimensionManager.registerDimension(ID, PhysicsMod.PHYSICS_PROVIDER_ID);
		parentTmp = parent;
		return ID;
	}

	public SegmentWorldServer(WorldServer parent, BlockPos rootPos) {
		super(parent.getMinecraftServer(), parent.getSaveHandler(), parent.getWorldInfo(), setDimensionUp(parent), parent.theProfiler);
		
		addWorldAccess(new SegmentWorldManager(getMinecraftServer(), this));

		spawnHostileMobs = false;
		spawnPeacefulMobs = false;
		disableLevelSaving = true;
		dummyTracker = new DummyEntityTracker(parent);
		body = createJBulletBody(rootPos.getX(), rootPos.getY(), rootPos.getZ());
		root = new BlockPos(0, 128, 0);
	}

	public boolean setBlockState(BlockPos pos, IBlockState state, int flag) {
		((SegmentServerChunkProvider) getChunkProvider()).activateChunk(pos.getX() >> 4, pos.getZ() >> 4);
		boolean b = super.setBlockState(pos, state, flag);
		addCube(pos, state);
		reAddBody = true;
		
		if(!bodyAdded)
			getBulletWorld().addRigidBody(getRigidBody());
		
		bodyAdded = true;

		return b;
	}

	protected IChunkProvider createChunkProvider() {
		IChunkLoader ichunkloader = saveHandler.getChunkLoader(provider);
		theChunkProviderServer = new SegmentServerChunkProvider(this, ichunkloader, provider.createChunkGenerator());
		return theChunkProviderServer;
	}

	public void sendUpdatePacket() {
		PhysicsMod.channel.sendToAll(new SegmentStatePacket(this));
	}
	
	/** Creates a ghost player and adds it to this segment */
	public void createGhost(EntityPlayerMP player) {
		PlayerGhost ghost = new PlayerGhost(player, this);
		spawnEntityInWorld(ghost);
		getPlayerManager().addPlayer(ghost);
	}
	
	public World init() {
		super.init();
		
		for(Object player: parent.playerEntities) {
			createGhost((EntityPlayerMP) player);
		}
		
		return this;
	}
	
	public EntityTracker getEntityTracker() {
		return dummyTracker;
	}

	@Override
	public World getParent() {
		if(parent == null)
			parent = parentTmp;
		
		parentTmp = null;
		assert parent != null : "Access of parent too early";
		
		return parent;
	}

	@Override
	public RigidBody getRigidBody() {
		return body;
	}
	
	public DynamicsWorld getBulletWorld() {
		return getBulletWorld(getParent().provider.getDimensionId());
	}
	
	public void tick() {
		super.tick();
		sendUpdatePacket();
		
		if(reAddBody) {
			getBulletWorld().removeRigidBody(body);
			getBulletWorld().addRigidBody(body);
			reAddBody = false;
		}
		
		//populateCheckedEntities();
		//adjustEntities();
	}
	
	@Override
	public int getIDNumber() {
		return provider.getDimensionId();
	}

	public void updatePlayerPos(EntityPlayerMP player, PlayerGhost playerMPGhost) {
		Transform t = getTransform();
		t.inverse();
		Vector3f vec = new Vector3f((float) player.posX, (float) player.posY, (float) player.posZ);
		t.transform(vec);
		playerMPGhost.setPositionAndRotation(vec.x, vec.y, vec.z, 0.0f, 0.0f);
	}

	//START ENTITY WORLD//
	
	/** Added / removes / updates true false flag of the checkedEntities box */
	public void populateCheckedEntities() {
		checkedEntities.clear();
		
		for(Object e : getParent().getEntities(EntityPlayer.class, e -> true))
			checkedEntities.put((Entity) e, false);
	}
	
	public Map<Entity, Boolean> getCheckedEntities() {
		return checkedEntities;
	}
	
	//END ENTITY WORLD//
	
	public BlockPos getRoot() {
		return root;
	}
	
	public void playSoundEffect(double x, double y, double z, String soundName, float volume, float pitch) {
        Vec3 pos = segToWorld(x, y, z);
        getParent().playSoundEffect(pos.xCoord, pos.yCoord, pos.zCoord, soundName, volume, pitch);
    }
}
