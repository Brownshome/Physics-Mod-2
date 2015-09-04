
package brownshome.physicsmod;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.init.Blocks;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent.Action;
import net.minecraftforge.event.entity.player.PlayerOpenContainerEvent;
import net.minecraftforge.event.world.BlockEvent.NeighborNotifyEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.eventhandler.Event.Result;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;

import org.apache.logging.log4j.Logger;

import brownshome.physicsmod.blocks.BlockThruster;
import brownshome.physicsmod.entity.PlayerGhost;
import brownshome.physicsmod.entity.SegmentEntity;
import brownshome.physicsmod.network.ClientWrapperPacket;
import brownshome.physicsmod.network.SegmentStatePacket;
import brownshome.physicsmod.network.ServerWrapperPacket;
import brownshome.physicsmod.render.SegmentRenderGlobal;
import brownshome.physicsmod.server.TogglePhysicsCommand;
import brownshome.physicsmod.storage.ISegment;
import brownshome.physicsmod.storage.SegmentWorldClient;
import brownshome.physicsmod.storage.SegmentWorldProvider;
import brownshome.physicsmod.storage.SegmentWorldServer;

@Mod(modid = PhysicsMod.MODID, version = PhysicsMod.VERSION)

public class PhysicsMod {
	public static final String MODID = "physicsMod";
	public static final String VERSION = "0.1";

	public static final int PHYSICS_ENTITY = 0;
	public static SimpleNetworkWrapper channel;
	public static int PHYSICS_PROVIDER_ID;
	
	@Mod.Instance(MODID)
	public static PhysicsMod INSTANCE;

	@SidedProxy(clientSide = "brownshome.physicsmod.client.ClientProxy", serverSide = "brownshome.physicsmod.server.CommonProxy")
	public static CommonProxy proxy;
	public static Logger logger = FMLCommonHandler.instance().getFMLLogger();
	
	public static final Block THRUSTER = new BlockThruster();
	
	@Mod.EventHandler
	public void init(FMLInitializationEvent event) {
		FMLCommonHandler.instance().bus().register(INSTANCE);
		MinecraftForge.EVENT_BUS.register(INSTANCE);
		
		channel = NetworkRegistry.INSTANCE.newSimpleChannel("PhysicsMod|WorldData");
		channel.registerMessage(ServerWrapperPacket.Handler.class, ServerWrapperPacket.class, 0, Side.CLIENT);
		channel.registerMessage(ClientWrapperPacket.Handler.class, ClientWrapperPacket.class, 1, Side.SERVER);
		channel.registerMessage(SegmentStatePacket.Handler.class, SegmentStatePacket.class, 2, Side.CLIENT);

		EntityRegistry.registerModEntity(SegmentEntity.class, "Physics Segment", PHYSICS_ENTITY, INSTANCE, 64, 10, true);

		PHYSICS_PROVIDER_ID = DimensionManager.getNextFreeDimId();
		DimensionManager.registerProviderType(PHYSICS_PROVIDER_ID, SegmentWorldProvider.class, false);
		
		GameRegistry.registerBlock(THRUSTER, BlockThruster.NAME);
		proxy.doSidedInit();
	}

	@SubscribeEvent
	public void onClientTick(TickEvent.ClientTickEvent event) {
		if(Minecraft.getMinecraft().theWorld != null)
			if(event.phase == Phase.START) {
				SegmentWorldClient.tickSegmentsPre();
				SegmentWorldClient.updatePhysMesh(Minecraft.getMinecraft().theWorld.provider.getDimensionId());
				SegmentWorldClient.stepBulletWorld(Minecraft.getMinecraft().theWorld.provider.getDimensionId());
			} else {
				SegmentWorldClient.tickSegmentsPost();
			}
	}
	
	@SubscribeEvent
	public void onServerWorldTick(TickEvent.WorldTickEvent event) {
		if(event.phase == Phase.END && !(event.world instanceof ISegment)) {
			if(event.world.provider.getDimensionId() == 0) {
				SegmentWorldServer.updatePhysMesh(event.world.provider.getDimensionId());
				SegmentWorldServer.stepBulletWorld(event.world.provider.getDimensionId());
			}
		}
	}

	@SubscribeEvent
	public void entityForceUpdate(EntityEvent.CanUpdate event) {
		if(event.entity instanceof PlayerGhost)
			event.canUpdate = true;
	}
	
	@SubscribeEvent
	public void onPlayerContainerCheck(PlayerOpenContainerEvent event) {
		event.setResult(Result.ALLOW); //TODO fix
	}
	
	@SubscribeEvent
	public void onClick(PlayerInteractEvent event) {
		if(event.action == Action.RIGHT_CLICK_AIR && event.world instanceof WorldClient) {
			event.setCanceled(SegmentWorldClient.rightClick());
		}
	}

	@Mod.EventHandler
	public void serverLoad(FMLServerStartingEvent event) {
		event.registerServerCommand(new TogglePhysicsCommand());
	}
	
	@SubscribeEvent
	public void updateHook(NeighborNotifyEvent event) {    	
		if(event.world.isRemote)
			return;
		
		if(!event.world.getChunkFromBlockCoords(event.pos).isPopulated())
			return;

		switch(SepperationHandler.state) {
		case MOVING:
			event.setCanceled(true);
		case NOTIFYING:
			return;
		case IDLE:
		}

		Block b = event.world.getBlockState(event.pos).getBlock();

		if(b != Blocks.air && b != Blocks.bedrock) {
			if(event.world instanceof SegmentWorldServer)
				;//SepperationHandler.testSepperationSegment(event.pos, (SegmentWorldServer) event.world);
			else
				SepperationHandler.testSepperation(event.pos, event.world);
		}

		for(EnumFacing dir : event.getNotifiedSides()) {
			b = event.world.getBlockState(event.pos.offset(dir)).getBlock();

			if(b == Blocks.air || b == Blocks.bedrock)
				continue;

			if(event.world instanceof SegmentWorldServer)
				;//SepperationHandler.testSepperationSegment(event.pos.offset(dir), (SegmentWorldServer) event.world);
			else
				SepperationHandler.testSepperation(event.pos.offset(dir), event.world); //basically a double up of on neighbour notify
		}
	}
}
