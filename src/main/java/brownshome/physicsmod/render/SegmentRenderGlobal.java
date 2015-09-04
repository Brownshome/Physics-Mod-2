package brownshome.physicsmod.render;

import java.nio.FloatBuffer;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.vecmath.Vector3f;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.DestroyBlockProgress;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.ViewFrustum;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.chunk.ListChunkFactory;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.chunk.VboChunkFactory;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.projectile.EntityWitherSkull;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import brownshome.physicsmod.storage.ISegment;
import brownshome.physicsmod.storage.SegmentWorldClient;

import com.bulletphysics.linearmath.Transform;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class SegmentRenderGlobal extends RenderGlobal {
	public SegmentRenderGlobal() {
		super(Minecraft.getMinecraft());

		if (this.vboEnabled) {
			this.renderContainer = new SegmentVboRenderList();
		} else {
			this.renderContainer = new SegmentRenderList();
		}
	}

	@Override
	public void loadRenderers() {
		if (this.theWorld != null) {
			this.displayListEntitiesDirty = true;
			Blocks.leaves.setGraphicsLevel(this.mc.gameSettings.fancyGraphics);
			Blocks.leaves2.setGraphicsLevel(this.mc.gameSettings.fancyGraphics);
			this.renderDistanceChunks = this.mc.gameSettings.renderDistanceChunks;
			boolean flag = this.vboEnabled;
			this.vboEnabled = OpenGlHelper.useVbo();

			if (flag && !this.vboEnabled) {
				this.renderContainer = new SegmentRenderList();
				this.renderChunkFactory = new ListChunkFactory();
			} else if (!flag && this.vboEnabled) {
				this.renderContainer = new SegmentVboRenderList();
				this.renderChunkFactory = new VboChunkFactory();
			}

			if (flag != this.vboEnabled) {
				this.generateStars();
				this.generateSky();
				this.generateSky2();
			}

			if (this.viewFrustum != null) {
				this.viewFrustum.deleteGlResources();
			}

			this.stopChunkUpdates();
			this.viewFrustum = new ViewFrustum(this.theWorld, this.mc.gameSettings.renderDistanceChunks, this, this.renderChunkFactory);

			if (this.theWorld != null) {
				Entity entity = this.mc.getRenderViewEntity();

				if (entity != null) {
					this.viewFrustum.updateChunkPositions(entity.posX, entity.posZ);
				}
			}

			this.renderEntitiesStartupCounter = 2;
		}
	}

	//made changes to support multiple view frustrums for the different world objects
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void setupTerrain(Entity viewEntity, double partialTicks, ICamera camera, int frameCount, boolean playerSpectator) {
		if (this.mc.gameSettings.renderDistanceChunks != this.renderDistanceChunks) {
			this.loadRenderers();
		}

		Collection<SegmentWorldClient> worlds = SegmentWorldClient.getWorlds();
		ViewFrustum[] frustums = new ViewFrustum[worlds.size() + 1];
		int i = 0;
		frustums[i++] = viewFrustum;

		for(SegmentWorldClient segment : worlds)
			frustums[i++] = segment.getViewFrustum();

		double d1 = viewEntity.posX - this.frustumUpdatePosX;
		double d2 = viewEntity.posY - this.frustumUpdatePosY;
		double d3 = viewEntity.posZ - this.frustumUpdatePosZ;
		//TODO add refresh for adding segments
		if (true || frustumUpdatePosChunkX != viewEntity.chunkCoordX || this.frustumUpdatePosChunkY != viewEntity.chunkCoordY || this.frustumUpdatePosChunkZ != viewEntity.chunkCoordZ || d1 * d1 + d2 * d2 + d3 * d3 > 16.0D) {
			this.frustumUpdatePosX = viewEntity.posX;
			this.frustumUpdatePosY = viewEntity.posY;
			this.frustumUpdatePosZ = viewEntity.posZ;
			this.frustumUpdatePosChunkX = viewEntity.chunkCoordX;
			this.frustumUpdatePosChunkY = viewEntity.chunkCoordY;
			this.frustumUpdatePosChunkZ = viewEntity.chunkCoordZ;

			for(ViewFrustum vf : frustums) {
				if(vf.world instanceof ISegment) {
					Vec3 vec = ((ISegment) vf.world).worldToSeg(viewEntity.posX, viewEntity.posY, viewEntity.posZ);
					vf.updateChunkPositions(vec.xCoord, vec.zCoord);
				} else {
					vf.updateChunkPositions(viewEntity.posX, viewEntity.posZ);
				}
			}
		}

		double d4 = viewEntity.lastTickPosX + (viewEntity.posX - viewEntity.lastTickPosX) * partialTicks;
		double d5 = viewEntity.lastTickPosY + (viewEntity.posY - viewEntity.lastTickPosY) * partialTicks;
		double d6 = viewEntity.lastTickPosZ + (viewEntity.posZ - viewEntity.lastTickPosZ) * partialTicks;
		this.renderContainer.initialize(d4, d5, d6);

		if (this.debugFixedClippingHelper != null) {
			Frustum frustum = new Frustum(this.debugFixedClippingHelper);
			frustum.setPosition(this.debugTerrainFrustumPosition.x, this.debugTerrainFrustumPosition.y, this.debugTerrainFrustumPosition.z);
			camera = frustum;
		}

		BlockPos blockpos = new BlockPos(MathHelper.floor_double(d4) / 16 * 16, MathHelper.floor_double(d5) / 16 * 16, MathHelper.floor_double(d6) / 16 * 16);
		this.displayListEntitiesDirty = this.displayListEntitiesDirty || !this.chunksToUpdate.isEmpty() || viewEntity.posX != this.lastViewEntityX || viewEntity.posY != this.lastViewEntityY || viewEntity.posZ != this.lastViewEntityZ || (double)viewEntity.rotationPitch != this.lastViewEntityPitch || (double)viewEntity.rotationYaw != this.lastViewEntityYaw;
		this.lastViewEntityX = viewEntity.posX;
		this.lastViewEntityY = viewEntity.posY;
		this.lastViewEntityZ = viewEntity.posZ;
		this.lastViewEntityPitch = (double)viewEntity.rotationPitch;
		this.lastViewEntityYaw = (double)viewEntity.rotationYaw;
		boolean flag1 = this.debugFixedClippingHelper != null;

		if (!flag1 && (this.displayListEntitiesDirty || true))
		{
			this.displayListEntitiesDirty = false;
			this.renderInfos = Lists.newArrayList();

			for(ViewFrustum vf : frustums) {
				Vec3 eyePos;

				if(vf.world instanceof ISegment) {
					eyePos = ((ISegment) vf.world).worldToSeg(d4, d5, d6);
				} else {
					eyePos = new Vec3(d4, d5, d6);
				}

				BlockPos eyeBlockPos = new BlockPos(eyePos);
				RenderChunk renderchunk = vf.getRenderChunk(eyeBlockPos);

				LinkedList linkedlist = Lists.newLinkedList();
				boolean flag2 = this.mc.renderChunksMany;

				//populate renderInfos, for each Segment
				if (renderchunk == null)
				{
					int j = eyeBlockPos.getY() > 0 ? 248 : 8;

					for (int k = -this.renderDistanceChunks; k <= this.renderDistanceChunks; ++k)
					{
						for (int l = -this.renderDistanceChunks; l <= this.renderDistanceChunks; ++l)
						{
							RenderChunk renderchunk1 = vf.getRenderChunk(new BlockPos((k << 4) + 8, j, (l << 4) + 8));

							AxisAlignedBB bb = null;

							if(renderchunk1 != null)
								if(vf.world instanceof ISegment) {
									bb = ((ISegment) vf.world).BBsegToWorld(renderchunk1.boundingBox);
								} else {
									bb = renderchunk1.boundingBox;
								}

							if (renderchunk1 != null && ((ICamera)camera).isBoundingBoxInFrustum(bb))
							{
								renderchunk1.setFrameIndex(frameCount);
								linkedlist.add(new RenderGlobal.ContainerLocalRenderInformation(renderchunk1, (EnumFacing)null, 0, null));
							}
						}
					}
				}
				else
				{
					boolean flag3 = false;

					RenderGlobal.ContainerLocalRenderInformation containerlocalrenderinformation2 = new RenderGlobal.ContainerLocalRenderInformation(renderchunk, (EnumFacing)null, 0, null);
					Set set1 = this.getVisibleFacings(eyeBlockPos, vf.world);

					if (!set1.isEmpty() && set1.size() == 1)
					{
						Vector3f vec = getViewVector(viewEntity, partialTicks);

						if(vf.world instanceof ISegment) {
							((ISegment) vf.world).worldToSegDirection(vec);
						}

						EnumFacing enumfacing = EnumFacing.getFacingFromVector(vec.x, vec.y, vec.z).getOpposite();
						set1.remove(enumfacing);
					}

					if (set1.isEmpty())
					{
						flag3 = true;
					}
					
					if (flag3 && !playerSpectator)
					{
						this.renderInfos.add(containerlocalrenderinformation2);
					}
					else
					{
						if (playerSpectator && vf.world.getBlockState(eyeBlockPos).getBlock().isOpaqueCube())
						{
							flag2 = false;
						}

						renderchunk.setFrameIndex(frameCount);
						linkedlist.add(containerlocalrenderinformation2);
					}
				}

				//find visable chunks
				while (!linkedlist.isEmpty())
				{
					RenderGlobal.ContainerLocalRenderInformation container = (RenderGlobal.ContainerLocalRenderInformation)linkedlist.poll();
					RenderChunk renderchunk3 = container.renderChunk;
					EnumFacing enumfacing2 = container.facing;
					BlockPos blockpos2 = renderchunk3.getPosition();
					this.renderInfos.add(container);
					EnumFacing[] aenumfacing = EnumFacing.values();
					int i1 = aenumfacing.length;

					for (int j1 = 0; j1 < i1; ++j1)
					{
						EnumFacing enumfacing1 = aenumfacing[j1];
						RenderChunk renderchunk2 = this.getRenderChunkOffset(eyeBlockPos, blockpos2, enumfacing1, vf);

						AxisAlignedBB bb = null;

						if(renderchunk2 != null)
							if(vf.world instanceof ISegment) {
								bb = ((ISegment) vf.world).BBsegToWorld(renderchunk2.boundingBox);
							} else {
								bb = renderchunk2.boundingBox;
							}

						if ((!flag2 || !container.setFacing.contains(enumfacing1.getOpposite())) && (!flag2 || enumfacing2 == null || renderchunk3.getCompiledChunk().isVisible(enumfacing2.getOpposite(), enumfacing1)) && renderchunk2 != null && renderchunk2.setFrameIndex(frameCount) && ((ICamera)camera).isBoundingBoxInFrustum(bb))
						{
							RenderGlobal.ContainerLocalRenderInformation newContainer = new RenderGlobal.ContainerLocalRenderInformation(renderchunk2, enumfacing1, container.counter + 1, null);
							newContainer.setFacing.addAll(container.setFacing);
							newContainer.setFacing.add(enumfacing1);
							linkedlist.add(newContainer);
						}
					}
				}
			}
		}

		if (this.debugFixTerrainFrustum)
		{
			this.fixTerrainFrustum(d4, d5, d6);
			this.debugFixTerrainFrustum = false;
		}

		this.renderDispatcher.clearChunkUpdates();
		Set set = this.chunksToUpdate;
		this.chunksToUpdate = Sets.newLinkedHashSet();
		Iterator iterator = this.renderInfos.iterator();

		//build chunks
		while (iterator.hasNext())
		{
			RenderGlobal.ContainerLocalRenderInformation container = (RenderGlobal.ContainerLocalRenderInformation)iterator.next();
			RenderChunk renderchunk3 = container.renderChunk;

			if (renderchunk3.isNeedsUpdate() || renderchunk3.isCompileTaskPending() || set.contains(renderchunk3) || renderchunk3.world instanceof ISegment)
			{
				this.displayListEntitiesDirty = true;

				BlockPos blockPos2 = blockpos;
				if(renderchunk3.world instanceof ISegment) {
					Vec3 v = ((ISegment) renderchunk3.world).worldToSeg(blockpos.getX() + 0.5, blockpos.getY() + 0.5, blockpos.getZ() + 0.5);
					blockPos2 = new BlockPos(v);
				}
				
				if (this.isPositionInRenderChunk(blockPos2, container.renderChunk)) //TODO take into account transformations and such at creation of blockpos
				{
					this.renderDispatcher.updateChunkNow(renderchunk3);
					renderchunk3.setNeedsUpdate(false);
				}
				else
				{
					this.chunksToUpdate.add(renderchunk3);
				}
			}
		}

		this.chunksToUpdate.addAll(set);
	}

	@SuppressWarnings("rawtypes")
	public void renderEntities(Entity p_180446_1_, ICamera p_180446_2_, float partialTicks) {
		int pass = net.minecraftforge.client.MinecraftForgeClient.getRenderPass();
		if (this.renderEntitiesStartupCounter > 0)
		{
			if (pass > 0) return;
			--this.renderEntitiesStartupCounter;
		}
		else
		{
			double d0 = p_180446_1_.prevPosX + (p_180446_1_.posX - p_180446_1_.prevPosX) * (double)partialTicks;
			double d1 = p_180446_1_.prevPosY + (p_180446_1_.posY - p_180446_1_.prevPosY) * (double)partialTicks;
			double d2 = p_180446_1_.prevPosZ + (p_180446_1_.posZ - p_180446_1_.prevPosZ) * (double)partialTicks;
			this.theWorld.theProfiler.startSection("prepare");
			TileEntityRendererDispatcher.instance.cacheActiveRenderInfo(this.theWorld, this.mc.getTextureManager(), this.mc.fontRendererObj, this.mc.getRenderViewEntity(), partialTicks);

			this.renderManager.cacheActiveRenderInfo(this.theWorld, this.mc.fontRendererObj, this.mc.getRenderViewEntity(), this.mc.pointedEntity, this.mc.gameSettings, partialTicks);
			if (pass == 0) // no indentation to shrink patch
			{
				this.countEntitiesTotal = 0;
				this.countEntitiesRendered = 0;
				this.countEntitiesHidden = 0;
			}
			Entity entity1 = this.mc.getRenderViewEntity();
			double d3 = entity1.lastTickPosX + (entity1.posX - entity1.lastTickPosX) * (double)partialTicks;
			double d4 = entity1.lastTickPosY + (entity1.posY - entity1.lastTickPosY) * (double)partialTicks;
			double d5 = entity1.lastTickPosZ + (entity1.posZ - entity1.lastTickPosZ) * (double)partialTicks;
			TileEntityRendererDispatcher.staticPlayerX = d3; //TODO adjust these
			TileEntityRendererDispatcher.staticPlayerY = d4;
			TileEntityRendererDispatcher.staticPlayerZ = d5;
			this.renderManager.setRenderPosition(d3, d4, d5);
			this.mc.entityRenderer.enableLightmap();
			this.theWorld.theProfiler.endStartSection("global");
			List list = this.theWorld.getLoadedEntityList();
			if (pass == 0) // no indentation to shrink patch
			{
				this.countEntitiesTotal = list.size();
			}
			int i;
			Entity entity2;

			for (i = 0; i < this.theWorld.weatherEffects.size(); ++i)
			{
				entity2 = (Entity)this.theWorld.weatherEffects.get(i);
				if (!entity2.shouldRenderInPass(pass)) continue;
				++this.countEntitiesRendered;

				if (entity2.isInRangeToRender3d(d0, d1, d2))
				{
					this.renderManager.renderEntitySimple(entity2, partialTicks);
				}
			}

			if (this.isRenderEntityOutlines())
			{
				GlStateManager.depthFunc(519);
				GlStateManager.disableFog();
				this.entityOutlineFramebuffer.framebufferClear();
				this.entityOutlineFramebuffer.bindFramebuffer(false);
				this.theWorld.theProfiler.endStartSection("entityOutlines");
				RenderHelper.disableStandardItemLighting();
				this.renderManager.setRenderOutlines(true);

				for (i = 0; i < list.size(); ++i)
				{
					entity2 = (Entity)list.get(i);
					if (!entity2.shouldRenderInPass(pass)) continue;
					boolean flag = this.mc.getRenderViewEntity() instanceof EntityLivingBase && ((EntityLivingBase)this.mc.getRenderViewEntity()).isPlayerSleeping();
					boolean flag1 = entity2.isInRangeToRender3d(d0, d1, d2) && (entity2.ignoreFrustumCheck || p_180446_2_.isBoundingBoxInFrustum(entity2.getEntityBoundingBox()) || entity2.riddenByEntity == this.mc.thePlayer) && entity2 instanceof EntityPlayer;

					if ((entity2 != this.mc.getRenderViewEntity() || this.mc.gameSettings.thirdPersonView != 0 || flag) && flag1)
					{
						this.renderManager.renderEntitySimple(entity2, partialTicks);
					}
				}

				this.renderManager.setRenderOutlines(false);
				RenderHelper.enableStandardItemLighting();
				GlStateManager.depthMask(false);
				this.entityOutlineShader.loadShaderGroup(partialTicks);
				GlStateManager.depthMask(true);
				this.mc.getFramebuffer().bindFramebuffer(false);
				GlStateManager.enableFog();
				GlStateManager.depthFunc(515);
				GlStateManager.enableDepth();
				GlStateManager.enableAlpha();
			}

			this.theWorld.theProfiler.endStartSection("entities");
			Iterator iterator = this.renderInfos.iterator();
			RenderGlobal.ContainerLocalRenderInformation containerlocalrenderinformation;

			while (iterator.hasNext())
			{
				containerlocalrenderinformation = (RenderGlobal.ContainerLocalRenderInformation)iterator.next();
				Chunk chunk = this.theWorld.getChunkFromBlockCoords(containerlocalrenderinformation.renderChunk.getPosition());
				Iterator iterator2 = chunk.getEntityLists()[containerlocalrenderinformation.renderChunk.getPosition().getY() / 16].iterator();

				while (iterator2.hasNext())
				{
					Entity entity3 = (Entity)iterator2.next();
					if (!entity3.shouldRenderInPass(pass)) continue;
					boolean flag2 = this.renderManager.shouldRender(entity3, p_180446_2_, d0, d1, d2) || entity3.riddenByEntity == this.mc.thePlayer;

					if (flag2)
					{
						boolean flag3 = this.mc.getRenderViewEntity() instanceof EntityLivingBase ? ((EntityLivingBase)this.mc.getRenderViewEntity()).isPlayerSleeping() : false;

						if (entity3 == this.mc.getRenderViewEntity() && this.mc.gameSettings.thirdPersonView == 0 && !flag3 || entity3.posY >= 0.0D && entity3.posY < 256.0D && !this.theWorld.isBlockLoaded(new BlockPos(entity3)))
						{
							continue;
						}

						++this.countEntitiesRendered;
						this.renderManager.renderEntitySimple(entity3, partialTicks);
					}

					if (!flag2 && entity3 instanceof EntityWitherSkull)
					{
						this.mc.getRenderManager().renderWitherSkull(entity3, partialTicks);
					}
				}
			}

			this.theWorld.theProfiler.endStartSection("blockentities");
			RenderHelper.enableStandardItemLighting();
			iterator = this.renderInfos.iterator();
			TileEntity tileentity;

			while (iterator.hasNext())
			{	
				containerlocalrenderinformation = (RenderGlobal.ContainerLocalRenderInformation)iterator.next();

				Iterator iterator1 = containerlocalrenderinformation.renderChunk.getCompiledChunk().getTileEntities().iterator();

				double entityX = TileEntityRendererDispatcher.instance.entityX;
				double entityY = TileEntityRendererDispatcher.instance.entityY;
				double entityZ = TileEntityRendererDispatcher.instance.entityZ;

				while (iterator1.hasNext())
				{
					tileentity = (TileEntity)iterator1.next();

					AxisAlignedBB bb;

					if(containerlocalrenderinformation.renderChunk.world instanceof ISegment) {
						bb = ((ISegment) containerlocalrenderinformation.renderChunk.world).BBsegToWorld(tileentity.getRenderBoundingBox());

						Vec3 p = ((ISegment) containerlocalrenderinformation.renderChunk.world).worldToSeg(entityX, entityY, entityZ);
						TileEntityRendererDispatcher.instance.entityX = p.xCoord;
						TileEntityRendererDispatcher.instance.entityY = p.yCoord;
						TileEntityRendererDispatcher.instance.entityZ = p.zCoord;
					} else {
						bb = tileentity.getRenderBoundingBox();
						TileEntityRendererDispatcher.instance.entityX = entityX;
						TileEntityRendererDispatcher.instance.entityY = entityY;
						TileEntityRendererDispatcher.instance.entityZ = entityZ;
					}

					if (!tileentity.shouldRenderInPass(pass) || !p_180446_2_.isBoundingBoxInFrustum(bb)) continue;

					if(containerlocalrenderinformation.renderChunk.world instanceof ISegment) {
						GlStateManager.pushMatrix();
						GlStateManager.translate((float) -TileEntityRendererDispatcher.staticPlayerX, (float) -TileEntityRendererDispatcher.staticPlayerY, (float) -TileEntityRendererDispatcher.staticPlayerZ);
						
						Transform trans = ((ISegment) tileentity.getWorld()).getTransform();
						FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
						float[] array = new float[16];
						trans.getOpenGLMatrix(array);
						buffer.put(array).flip();
						
						GlStateManager.multMatrix(buffer);
						
						GlStateManager.translate((float) TileEntityRendererDispatcher.staticPlayerX, (float) TileEntityRendererDispatcher.staticPlayerY, (float) TileEntityRendererDispatcher.staticPlayerZ);
					}
					
					TileEntityRendererDispatcher.instance.renderTileEntity(tileentity, partialTicks, -1);
					
					if(containerlocalrenderinformation.renderChunk.world instanceof ISegment)
						GlStateManager.popMatrix();
				}

				TileEntityRendererDispatcher.instance.entityX = entityX;
				TileEntityRendererDispatcher.instance.entityY = entityY;
				TileEntityRendererDispatcher.instance.entityZ = entityZ;
			}

			this.preRenderDamagedBlocks();
			iterator = this.damagedBlocks.values().iterator();

			while (iterator.hasNext())
			{
				DestroyBlockProgress destroyblockprogress = (DestroyBlockProgress)iterator.next();
				BlockPos blockpos = destroyblockprogress.getPosition();
				tileentity = this.theWorld.getTileEntity(blockpos);

				if (tileentity instanceof TileEntityChest)
				{
					TileEntityChest tileentitychest = (TileEntityChest)tileentity;

					if (tileentitychest.adjacentChestXNeg != null)
					{
						blockpos = blockpos.offset(EnumFacing.WEST);
						tileentity = this.theWorld.getTileEntity(blockpos);
					}
					else if (tileentitychest.adjacentChestZNeg != null)
					{
						blockpos = blockpos.offset(EnumFacing.NORTH);
						tileentity = this.theWorld.getTileEntity(blockpos);
					}
				}

				AxisAlignedBB bb;

				if(theWorld instanceof ISegment) {
					bb = ((ISegment) theWorld).BBsegToWorld(tileentity.getRenderBoundingBox());
				} else {
					bb = tileentity.getRenderBoundingBox();
				}

				if (tileentity != null && tileentity.shouldRenderInPass(pass) && tileentity.canRenderBreaking() && p_180446_2_.isBoundingBoxInFrustum(bb))
				{
					TileEntityRendererDispatcher.instance.renderTileEntity(tileentity, partialTicks, destroyblockprogress.getPartialBlockDamage());
				}
			}

			this.postRenderDamagedBlocks();
			this.mc.entityRenderer.disableLightmap();
			this.mc.mcProfiler.endSection();
		}
	}

	public Set<?> getVisibleFacings(BlockPos p_174978_1_, World world)
	{
		VisGraph visgraph = new VisGraph();
		BlockPos blockpos1 = new BlockPos(p_174978_1_.getX() >> 4 << 4, p_174978_1_.getY() >> 4 << 4, p_174978_1_.getZ() >> 4 << 4);
		Chunk chunk = world.getChunkFromBlockCoords(blockpos1);
		Iterator<?> iterator = BlockPos.getAllInBoxMutable(blockpos1, blockpos1.add(15, 15, 15)).iterator();

		while (iterator.hasNext())
		{
			BlockPos.MutableBlockPos mutableblockpos = (BlockPos.MutableBlockPos)iterator.next();

			if (chunk.getBlock(mutableblockpos).isOpaqueCube())
			{
				visgraph.func_178606_a(mutableblockpos);
			}
		}

		return visgraph.func_178609_b(p_174978_1_);
	}

	public RenderChunk getRenderChunkOffset(BlockPos p_174973_1_, BlockPos p_174973_2_, EnumFacing p_174973_3_, ViewFrustum vf)
	{
		BlockPos blockpos2 = p_174973_2_.offset(p_174973_3_, 16);
		return MathHelper.abs_int(p_174973_1_.getX() - blockpos2.getX()) > this.renderDistanceChunks * 16 ? null : (blockpos2.getY() >= 0 && blockpos2.getY() < 256 ? (MathHelper.abs_int(p_174973_1_.getZ() - blockpos2.getZ()) > this.renderDistanceChunks * 16 ? null : vf.getRenderChunk(blockpos2)) : null);
	}

	public void drawBlockDamageTexture(Tessellator p_174981_1_, WorldRenderer p_174981_2_, Entity p_174981_3_, float partialTicks) {
		//hook to draw the block outlines
		{
			GlStateManager.disableBlend();
			drawSegmentBlockOutlines(partialTicks);
			GlStateManager.enableBlend();
        	GlStateManager.tryBlendFuncSeparate(770, 1, 1, 0);
		}
        
		super.drawBlockDamageTexture(p_174981_1_, p_174981_2_, p_174981_3_, partialTicks);
	}

	public void drawSegmentBlockOutlines(float partialTicks) {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;

		for(SegmentWorldClient world : SegmentWorldClient.getWorlds())
			if (/* flag && */ world.getRay() != null && !player.isInsideOfMaterial(Material.water)) {
				GlStateManager.disableAlpha();

				if (!net.minecraftforge.client.ForgeHooksClient.onDrawBlockHighlight(this, Minecraft.getMinecraft().thePlayer, world.getRay(), 0, player.getHeldItem(), partialTicks))
					drawSelectionBox(world, player, world.getRay(), 0, partialTicks);
				GlStateManager.enableAlpha();
			}
	}

	public void drawSelectionBox(SegmentWorldClient world, EntityPlayer player, MovingObjectPosition ray, int type, float partialTicks) {
		if (type == 0 && ray.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
			GlStateManager.enableBlend();
			GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
			GlStateManager.color(0.0F, 0.0F, 0.0F, 0.4F);
			GL11.glLineWidth(2.0F);
			GlStateManager.disableTexture2D();
			GlStateManager.depthMask(false);
			float expansion = 0.002F;
			BlockPos blockpos = ray.getBlockPos();
			Block block = world.getBlockState(blockpos).getBlock();

			if (block.getMaterial() != Material.air) {
				block.setBlockBoundsBasedOnState(world, blockpos);
				double d0 = player.lastTickPosX + (player.posX - player.lastTickPosX) * (double)partialTicks;
				double d1 = player.lastTickPosY + (player.posY - player.lastTickPosY) * (double)partialTicks;
				double d2 = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * (double)partialTicks;
				
				Transform playerOffset = new Transform();
				playerOffset.setIdentity();
				playerOffset.origin.set((float) -d0, (float) -d1, (float) -d2);
				playerOffset.mul(world.getTransform());

				FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
				float[] array = new float[16];
				playerOffset.getOpenGLMatrix(array);
				buffer.put(array).flip();
				
				GlStateManager.pushMatrix();
				GlStateManager.multMatrix(buffer);
				
				drawOutlinedBoundingBox(block.getSelectedBoundingBox(this.theWorld, blockpos).expand(expansion, expansion, expansion), -1);
				GlStateManager.popMatrix();
			}

			GlStateManager.depthMask(true);
			GlStateManager.enableTexture2D();
			GlStateManager.disableBlend();
		}
	}
}
