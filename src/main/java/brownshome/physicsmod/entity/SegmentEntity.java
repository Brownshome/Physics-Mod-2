package brownshome.physicsmod.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.World;
import brownshome.physicsmod.storage.SegmentWorldServer;

//this is only used for ticks and storage, rendering is done through the world renderer
public class SegmentEntity extends Entity {
	World containedWorld;
	
	public SegmentEntity(World world) {
		super(world instanceof SegmentWorldServer ? ((SegmentWorldServer) world).getParent() : world);

		containedWorld = world;
		
		MinecraftServer.getServer().getConfigurationManager().sendChatMsg(new ChatComponentText("World Entity Created: " + world));
	}
	
	@Override
	public void onEntityUpdate() {
		super.onEntityUpdate();
		
		/*
		containedWorld.tick();
		containedWorld.updateEntities();
		*/
	}
	
	@Override
	protected void entityInit() {
		
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tagCompund) {
		
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tagCompound) {
		
	}
}
