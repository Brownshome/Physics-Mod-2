package brownshome.physicsmod.storage;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.vecmath.AxisAngle4f;
import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import javax.vecmath.Vector3f;

import scala.actors.threadpool.Arrays;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import brownshome.physicsmod.PhysicsHelper;

import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.dispatch.CollisionWorld.ClosestConvexResultCallback;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.CompoundShape;
import com.bulletphysics.collision.shapes.CompoundShapeChild;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;

public interface ISegment {
	public World getParent();
	
	/** The transform that takes a point in Seg space and moves it to World space */
	public default Transform getTransform() {
		return new Transform(((DefaultMotionState) getRigidBody().getMotionState()).graphicsWorldTrans);
	};
	
	public RigidBody getRigidBody();
	
	public default Vec3 worldToSeg(Vec3 vec) {
		return worldToSeg(vec.xCoord, vec.yCoord, vec.zCoord);
	}
	
	public default Vec3 segToWorld(Vec3 vec) {
		return segToWorld(vec.xCoord, vec.yCoord, vec.zCoord);
	}
	
	public default Vec3 segToWorld(double x, double y, double z) {
		Vector3f v = new Vector3f((float) x, (float) y, (float) z);
		getTransform().transform(v);
		return new Vec3(v.x, v.y, v.z);
	};
	
	public default Vec3 worldToSeg(double x, double y, double z) {
		Vector3f v = new Vector3f((float) x, (float) y, (float) z);
		Transform t = getTransform();
		t.inverse();
		t.transform(v);
		
		return new Vec3(v.x, v.y, v.z);
	};
	
	public Map<Entity, Boolean> getCheckedEntities();
	
	public default void adjustEntities() {
		for(Entry<Entity, Boolean> entry : getCheckedEntities().entrySet()) {
			Entity entity = entry.getKey();
			boolean relativeToSeg = entry.getValue();
			
			if(relativeToSeg) {
				//TODO
			} else
				adjustEntity(entity);
		}
	}
	
	/** Checks collisions with this entity and moves it if nessecary */
	public default void adjustEntity(Entity entity) {
		Vector3f prevPos = new Vector3f((float) entity.lastTickPosX, (float) entity.lastTickPosY, (float) entity.lastTickPosZ);
		Vector3f pos = new Vector3f((float) entity.posX, (float) entity.posY, (float) entity.posZ);
		
		BoxShape box = new BoxShape(new Vector3f(entity.width * .5f, entity.height * .5f, entity.width * .5f));
		
		Transform from = new Transform();
		Transform to = new Transform();
		
		from.setIdentity();
		to.setIdentity();
		
		from.origin.set(prevPos);
		to.origin.set(pos);
		
		to.origin.y += entity.height * 0.5f;
		from.origin.y += entity.height * 0.5f;
		
		ClosestConvexResultCallback callback = new CollisionWorld.ClosestConvexResultCallback(from.origin, to.origin);
		getBulletWorld().convexSweepTest(box, from, to, callback);
		
		if(callback.hasHit()) {
			getParent().spawnParticle(EnumParticleTypes.WATER_SPLASH, callback.hitPointWorld.x, callback.hitPointWorld.y, callback.hitPointWorld.z, callback.hitNormalWorld.x, callback.hitNormalWorld.y, callback.hitNormalWorld.z);
			for(Object p : getParent().getPlayers(EntityPlayer.class, e -> true)) {
				((EntityPlayer) p).addChatMessage(new ChatComponentText("Contact"));
			}
			
			Vector3f delta = new Vector3f();
			delta.sub(pos, prevPos);
			float proj = delta.dot(callback.hitNormalWorld);
			delta.scale(proj, callback.hitNormalWorld);
			pos.sub(delta);
			
			entity.setPosition(pos.x, pos.y, pos.z);
			
			Vector3f motion = new Vector3f((float) entity.motionX, (float) entity.motionY, (float) entity.motionZ);
			delta.scale(motion.dot(callback.hitNormalWorld), callback.hitNormalWorld);
			motion.sub(delta);
			
			entity.motionX = motion.x;
			entity.motionY = motion.y;
			entity.motionZ = motion.z;
		}
	};
	
	public default AxisAlignedBB BBworldToSeg(AxisAlignedBB aabb) {
		float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
		float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
		
		Transform t = getTransform();
		t.inverse();
		
		Vector3f v = new Vector3f();
		
		v.set((float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		v.set((float) aabb.maxX, (float) aabb.maxY, (float) aabb.minZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		v.set((float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		v.set((float) aabb.maxX, (float) aabb.minY, (float) aabb.minZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		v.set((float) aabb.minX, (float) aabb.maxY, (float) aabb.maxZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		v.set((float) aabb.minX, (float) aabb.maxY, (float) aabb.minZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		v.set((float) aabb.minX, (float) aabb.minY, (float) aabb.maxZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		v.set((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
	};
	
	public default AxisAlignedBB BBsegToWorld(AxisAlignedBB aabb) {
		float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
		float maxX = Float.MIN_VALUE, maxY = Float.MIN_VALUE, maxZ = Float.MIN_VALUE;
		
		Transform t = getTransform();
		Vector3f v = new Vector3f();
		
		v.set((float) aabb.maxX, (float) aabb.maxY, (float) aabb.maxZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		v.set((float) aabb.maxX, (float) aabb.maxY, (float) aabb.minZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		v.set((float) aabb.maxX, (float) aabb.minY, (float) aabb.maxZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		v.set((float) aabb.maxX, (float) aabb.minY, (float) aabb.minZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		v.set((float) aabb.minX, (float) aabb.maxY, (float) aabb.maxZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		v.set((float) aabb.minX, (float) aabb.maxY, (float) aabb.minZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		v.set((float) aabb.minX, (float) aabb.minY, (float) aabb.maxZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		v.set((float) aabb.minX, (float) aabb.minY, (float) aabb.minZ);
		t.transform(v);
		minX = Math.min(minX, v.x);
		minY = Math.min(minY, v.y);
		minZ = Math.min(minZ, v.z);
		maxX = Math.max(maxX, v.x);
		maxY = Math.max(maxY, v.y);
		maxZ = Math.max(maxZ, v.z);
		
		return new AxisAlignedBB(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	public default RigidBody createJBulletBody(float x, float y, float z) {
		CompoundShape shape = new CompoundShape();
		
		Vector3f inertia = new Vector3f(0, 0, 0);
		
		Transform t = new Transform();
		t.setIdentity();
		t.origin.set(x, y, z);
		t.basis.rotY((float) Math.PI / 2);
		
		RigidBody b = new RigidBody(1f, new DefaultMotionState(t), shape, inertia);
		b.setSleepingThresholds(0, 0);

		return b;
	}

	static float BLOCK_INERTIA = 1 / 6f;
	
	public default void recalculateInertia(List<BlockPos> positions, float[] mass) {
		Vector3f center = new Vector3f();
		
		int i = 0;
		float total = 0;
		for(BlockPos pos : positions) {
			center.x += pos.getX() * mass[i];
			center.y += pos.getY() * mass[i];
			center.z += pos.getZ() * mass[i];
			total += mass[i++];
		}
		
		center.scale(1 / total);
		
		Vector3f inertia = new Vector3f();
		
		float x, y, z, m;
		i = 0;
		
		for(BlockPos pos : positions) {
			m = mass[i++];
			x = pos.getX() - center.x;
			y = pos.getY() - center.y;
			z = pos.getZ() - center.z;
			
			x *= x;
			y *= y;
			z *= z;
			
			inertia.x += (y + z) * m + BLOCK_INERTIA * m;
			inertia.y += (x + y) * m + BLOCK_INERTIA * m;
			inertia.z += (x + y) * m + BLOCK_INERTIA * m;
		}
		
		RigidBody body = getRigidBody();
		DefaultMotionState state = (DefaultMotionState) body.getMotionState();
		
		CompoundShape compound = (CompoundShape) body.getCollisionShape();
		List<CompoundShapeChild> list = compound.getChildList();
		
		i = 0;
		for(CompoundShapeChild child : list) {
			child.transform.origin.set(positions.get(i).getX() - center.x, positions.get(i).getY() - center.y, positions.get(i).getZ() - center.z);
			i++;
		}
		
		compound.recalculateLocalAabb();
		
		center.x += 0.5f;
		center.y += 0.5f;
		center.z += 0.5f;
		
		Transform W = body.getWorldTransform(new Transform());
		W.origin.add(center);
		W.origin.add(state.centerOfMassOffset.origin);
		body.setWorldTransform(W);
		
		center.scale(-1);
		
		state.centerOfMassOffset.origin.set(center);
		
		body.setMassProps(total, inertia);
		
		state.setWorldTransform(W);
	}
	
	public default void addCube(BlockPos pos, IBlockState newState) {
		RigidBody body = getRigidBody();
		
		getBlocks().add(pos);
		
		CollisionShape shape = PhysicsHelper.getCollisionShape(pos, newState);
		CompoundShape cont  = (CompoundShape) body.getCollisionShape();
		Transform t = new Transform();
		t.setIdentity();
		cont.addChildShape(t, shape);
		
		float[] masses = new float[cont.getNumChildShapes()];
		Arrays.fill(masses, 1f);
		
		recalculateInertia(getBlocks(), masses);
	};

	public List<BlockPos> getBlocks();
	
	public default void worldToSegDirection(Vector3f vec) {
		Transform t = getTransform();
		t.inverse();
		t.basis.transform(vec);
	}
	
	public default void segToWorldDirection(Vector3f vec) {
		Transform t = getTransform();
		t.basis.transform(vec);
	}
	
	/** Used to find the server save file */
	public int getIDNumber();

	public DynamicsWorld getBulletWorld();

	public default void addForce(BlockPos pos, Vector3f force) {
		RigidBody body = getRigidBody();
		DefaultMotionState motionState = (DefaultMotionState) body.getMotionState();
		Vector3f COM = motionState.centerOfMassOffset.origin;
		
		Transform trans = body.getWorldTransform(new Transform());
		
		Vector3f p = new Vector3f(COM);
		p.x += pos.getX() + .5f;
		p.y += pos.getY() + .5f;
		p.z += pos.getZ() + .5f;
		
		trans.basis.transform(p);
		trans.basis.transform(force);
		
		force.scale(0.05f); //1 divided by twenty
		getRigidBody().applyImpulse(force, p);
	};
}
