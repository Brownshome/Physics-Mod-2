package brownshome.physicsmod.render;

import java.nio.FloatBuffer;

import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.VboRenderList;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.util.BlockPos;

import org.lwjgl.BufferUtils;

import brownshome.physicsmod.storage.ISegment;

import com.bulletphysics.linearmath.Transform;

public class SegmentVboRenderList extends VboRenderList {
	double x;
	double y;
	double z;
	
	public void preRenderChunk(RenderChunk chunk) {
		if(chunk.world instanceof ISegment) {
			GlStateManager.translate(-x, -y, -z);
			
			Transform trans = ((ISegment) chunk.world).getTransform();
			FloatBuffer buffer = BufferUtils.createFloatBuffer(16);
			float[] array = new float[16];
			trans.getOpenGLMatrix(array);
			buffer.put(array).flip();
			
			GlStateManager.multMatrix(buffer);
			
			BlockPos blockpos = chunk.getPosition();
	        GlStateManager.translate(blockpos.getX(), blockpos.getY(), blockpos.getZ());
		} else {
			super.preRenderChunk(chunk);
		}
	}
	
	public void initialize(double x, double y, double z) {
		super.initialize(x, y, z);
		
	}
}
