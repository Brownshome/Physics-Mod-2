package brownshome.physicsmod.network;

import javax.vecmath.Vector3f;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import brownshome.physicsmod.storage.SegmentWorldClient;
import brownshome.physicsmod.storage.SegmentWorldServer;

import com.bulletphysics.linearmath.Transform;

public class SegmentStatePacket implements IMessage {
	Transform transform = new Transform();
	Vector3f velocity = new Vector3f();
	Vector3f angularVelocity = new Vector3f();
	int id;
	
	public SegmentStatePacket() {}
	
	public SegmentStatePacket(SegmentWorldServer world) {
		id = world.getIDNumber();
		transform = world.getTransform();
		world.getRigidBody().getLinearVelocity(velocity);
		world.getRigidBody().getAngularVelocity(angularVelocity);
	}
	
	@Override
	public void fromBytes(ByteBuf buf) {
		id = buf.readInt();
		
		transform.basis.m00 = buf.readFloat();
		transform.basis.m01 = buf.readFloat();
		transform.basis.m02 = buf.readFloat();
		transform.basis.m10 = buf.readFloat();
		transform.basis.m11 = buf.readFloat();
		transform.basis.m12 = buf.readFloat();
		transform.basis.m20 = buf.readFloat();
		transform.basis.m21 = buf.readFloat();
		transform.basis.m22 = buf.readFloat();
		
		transform.origin.x = buf.readFloat();
		transform.origin.y = buf.readFloat();
		transform.origin.z = buf.readFloat();
		
		velocity.set(buf.readFloat(), buf.readFloat(), buf.readFloat());
		angularVelocity.set(buf.readFloat(), buf.readFloat(), buf.readFloat());
	}

	@Override
	public void toBytes(ByteBuf buf) {
		buf.writeInt(id);
		
		buf.writeFloat(transform.basis.m00);
		buf.writeFloat(transform.basis.m01);
		buf.writeFloat(transform.basis.m02);
		buf.writeFloat(transform.basis.m10);
		buf.writeFloat(transform.basis.m11);
		buf.writeFloat(transform.basis.m12);
		buf.writeFloat(transform.basis.m20);
		buf.writeFloat(transform.basis.m21);
		buf.writeFloat(transform.basis.m22);
		
		buf.writeFloat(transform.origin.x);
		buf.writeFloat(transform.origin.y);
		buf.writeFloat(transform.origin.z);
		
		buf.writeFloat(velocity.x);
		buf.writeFloat(velocity.y);
		buf.writeFloat(velocity.z);
		
		buf.writeFloat(angularVelocity.x);
		buf.writeFloat(angularVelocity.y);
		buf.writeFloat(angularVelocity.z);
	}

	public static class Handler implements IMessageHandler<SegmentStatePacket, IMessage> {

		@Override
		public IMessage onMessage(SegmentStatePacket message, MessageContext ctx) {
			Minecraft.getMinecraft().addScheduledTask(() -> {processMessage(message);});
			return null;
		}

		public void processMessage(SegmentStatePacket message) {
			SegmentWorldClient.getWorld(message.id).serverUpdate(message.transform, message.velocity, message.angularVelocity);
		}
	}
}
