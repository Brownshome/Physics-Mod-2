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
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import brownshome.physicsmod.entity.PlayerGhost;
import brownshome.physicsmod.storage.ISegment;
import brownshome.physicsmod.storage.SegmentWorldClient;

public class ClientWrapperPacket implements IMessage {
		World world;
		Packet packet;

		public ClientWrapperPacket(Packet packet, SegmentWorldClient world) {
			this.packet = packet;
			this.world = world;
		}

		public ClientWrapperPacket() {}

		@Override
		public void fromBytes(ByteBuf buf) {
			world = DimensionManager.getWorld(buf.readInt());

			try {
				packet = EnumConnectionState.PLAY.getPacket(EnumPacketDirection.SERVERBOUND, buf.readInt());
				packet.readPacketData(new PacketBuffer(buf));
			} catch (InstantiationException | IllegalAccessException | IOException e) {
				throw new DecoderException(e);
			}
		}

		@Override
		public void toBytes(ByteBuf buf) {
			buf.writeInt(((ISegment) world).getIDNumber());
			buf.writeInt((EnumConnectionState.PLAY.getPacketId(EnumPacketDirection.SERVERBOUND, packet)));
			
			try {
				packet.writePacketData(new PacketBuffer(buf));
			} catch (IOException e) {
				throw new RuntimeException("Error in encoding packet: " + e);
			}
		}
		
		public static class Handler implements IMessageHandler<ClientWrapperPacket, IMessage> {
			@Override
			public IMessage onMessage(ClientWrapperPacket message, MessageContext ctx) {

				try {
					PlayerGhost ghost = null;
					for(Object player : message.world.playerEntities)
						if(((PlayerGhost) player).getPlayer() == ctx.getServerHandler().playerEntity) {
							ghost = (PlayerGhost) player;
							break;
						}
					
					assert ghost != null : "Non regestered player on server side: " + ctx.getServerHandler().playerEntity.getName();
					
					message.packet.processPacket(ghost.playerNetServerHandler);
				} catch(ThreadQuickExitException ex) {
					; //this is normal, just moving things to the main minecraft thread
				}
				
				return null;
			}
		}
	}

