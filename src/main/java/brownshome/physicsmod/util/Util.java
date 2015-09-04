package brownshome.physicsmod.util;

import javax.vecmath.Vector3f;

import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;

public class Util {
	public static Vector3f toBulletVec3f(Vec3i vec) {
		return new Vector3f(vec.getX(), vec.getY(), vec.getZ());
	}
	
	public static Vec3i toMCVec3i(Vector3f vec) {
		return new Vec3i(vec.x, vec.y, vec.z);
	}

	public static BlockPos toMCBlockPos(Vector3f vec) {
		return new BlockPos(vec.x, vec.y, vec.z);
	}

	public static Vector3f toBulletVec3f(Vec3 vec) {
		return new Vector3f((float) vec.xCoord, (float) vec.yCoord, (float) vec.zCoord);
	}

	public static Vec3 toMCVec3(Vector3f vec) {
		return new Vec3(vec.x, vec.y, vec.z);
	}
}
