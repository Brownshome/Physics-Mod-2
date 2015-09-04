package brownshome.physicsmod.render;

import javax.vecmath.Vector3f;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import brownshome.physicsmod.PhysicsMod;

import com.bulletphysics.linearmath.IDebugDraw;

public class DebugSegmentDrawer extends IDebugDraw {
	public static final WorldRenderer DRAWER = Tessellator.getInstance().getWorldRenderer();
	
	@Override
	public void drawLine(Vector3f from, Vector3f to, Vector3f colour) {
		Vector3f player = new Vector3f((float) Minecraft.getMinecraft().thePlayer.posX, (float) Minecraft.getMinecraft().thePlayer.posY, (float) Minecraft.getMinecraft().thePlayer.posZ);
		Vector3f f = new Vector3f(from);
		f.sub(player);
		Vector3f t = new Vector3f(to);
		f.sub(player);
		
		DRAWER.setColorOpaque_F(colour.x, colour.y, colour.z);
		DRAWER.startDrawing(GL11.GL_LINES);
		DRAWER.addVertex(f.x, f.y, f.z);
		DRAWER.addVertex(t.x, t.y, t.z);
		DRAWER.finishDrawing();
	}

	@Override
	public void drawContactPoint(Vector3f PointOnB, Vector3f normalOnB, float distance, int lifeTime, Vector3f color) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void reportErrorWarning(String warningString) {
		PhysicsMod.logger.error(warningString);
	}

	@Override
	public void draw3dText(Vector3f location, String textString) {

	}

	int debugMode = 0;
	@Override
	public void setDebugMode(int debugMode) {
		this.debugMode = debugMode;
	}

	@Override
	public int getDebugMode() {
		return debugMode;
	}

}
