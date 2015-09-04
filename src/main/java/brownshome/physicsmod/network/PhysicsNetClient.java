package brownshome.physicsmod.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.Packet;
import brownshome.physicsmod.PhysicsMod;
import brownshome.physicsmod.storage.SegmentWorldClient;

public class PhysicsNetClient extends NetHandlerPlayClient {
	public PhysicsNetClient(SegmentWorldClient world) {
		super(Minecraft.getMinecraft(), null, Minecraft.getMinecraft().myNetworkManager, null);
		clientWorldController = world;
	}

	public void addToSendQueue(Packet packet) {
		PhysicsMod.channel.sendToServer(new ClientWrapperPacket(packet, (SegmentWorldClient) clientWorldController));
	}
}
