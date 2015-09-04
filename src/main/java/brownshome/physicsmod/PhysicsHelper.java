package brownshome.physicsmod;

import javax.vecmath.Vector3f;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;

import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.StaticPlaneShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DefaultMotionState;

public class PhysicsHelper {
	public static DynamicsWorld createBulletWorld(int dim) {
		DefaultCollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
		CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);
		BroadphaseInterface broadphase = new DbvtBroadphase();
		ConstraintSolver sol = new SequentialImpulseConstraintSolver();
		
		DynamicsWorld world = new DiscreteDynamicsWorld(dispatcher, broadphase, sol, collisionConfiguration);
		world.setGravity(new Vector3f(0f, -9f, 0f));
		
		return world;
	}

	public static CollisionShape getCollisionShape(BlockPos pos, IBlockState newState) {
		return new BoxShape(new Vector3f(.5f, .5f, .5f));
	}
}
