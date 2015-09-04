package brownshome.physicsmod.network;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;

import java.io.IOException;

import net.minecraft.network.EnumConnectionState;
import net.minecraft.network.EnumPacketDirection;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import brownshome.physicsmod.storage.SegmentWorldClient;
import brownshome.physicsmod.storage.SegmentWorldServer;

public class ServerWrapperPacket implements IMessage {
	World world;
	Packet packet;

	public ServerWrapperPacket(Packet packet, SegmentWorldServer world) {
		this.packet = packet;
		this.world = world;
	}

	public ServerWrapperPacket() {}

	@Override
	public void fromBytes(ByteBuf buf) {
		world = SegmentWorldClient.getWorld(buf.readInt());

		try {
			packet = EnumConnectionState.PLAY.getPacket(EnumPacketDirection.CLIENTBOUND, buf.readInt());
			packet.readPacketData(new PacketBuffer(buf));
		} catch (InstantiationException | IllegalAccessException | IOException e) {
			throw new DecoderException(e);
		}
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(((SegmentWorldServer) world).getIDNumber());
		buf.writeInt((EnumConnectionState.PLAY.getPacketId(EnumPacketDirection.CLIENTBOUND, packet)));
		
		try {
			packet.writePacketData(new PacketBuffer(buf));
		} catch (IOException e) {
			throw new RuntimeException("Error in encoding packet: " + e);
		}
	}
	
	public static class Handler implements IMessageHandler<ServerWrapperPacket, IMessage> {
		@Override
		public IMessage onMessage(ServerWrapperPacket message, MessageContext ctx) {

			try {
				message.packet.processPacket(((SegmentWorldClient) message.world).getNetHandler());
			} catch(ThreadQuickExitException ex) {
				; //this is normal, just moving things to the main minecraft thread
			}
			
			return null;
		}
	}
}
