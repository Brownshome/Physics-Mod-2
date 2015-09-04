package brownshome.physicsmod.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.item.Item;
import brownshome.physicsmod.CommonProxy;
import brownshome.physicsmod.PhysicsMod;
import brownshome.physicsmod.blocks.BlockThruster;
import brownshome.physicsmod.render.SegmentRenderGlobal;

public class ClientProxy extends CommonProxy {
	public void doSidedInit() {
		Minecraft.getMinecraft().renderGlobal = new SegmentRenderGlobal(); //VERY HACK, don't hate on me :P
		
		RenderItem renderItem = Minecraft.getMinecraft().getRenderItem();
		
		renderItem.getItemModelMesher().register(
			Item.getItemFromBlock(PhysicsMod.THRUSTER), 
			0, 
			new ModelResourceLocation(PhysicsMod.MODID + ":" + BlockThruster.NAME, "inventory")
		);
	}
}
