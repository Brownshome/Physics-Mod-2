package brownshome.physicsmod.network;

import net.minecraft.network.NetHandlerPlayServer;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.server.MinecraftServer;
import brownshome.physicsmod.PhysicsMod;
import brownshome.physicsmod.entity.PlayerGhost;
import brownshome.physicsmod.storage.SegmentWorldServer;

//handles sending of packets pertaining directly to segments
public class SegmentNetHandlerServer extends NetHandlerPlayServer {
	public SegmentNetHandlerServer(PlayerGhost playerIn) {
		super(MinecraftServer.getServer(), new NetworkManager(null), playerIn);
	}

	public void sendPacket(final Packet packetIn) {
		PhysicsMod.channel.sendTo(new ServerWrapperPacket(packetIn, (SegmentWorldServer) playerEntity.worldObj), ((PlayerGhost) playerEntity).getPlayer());
	}
}
