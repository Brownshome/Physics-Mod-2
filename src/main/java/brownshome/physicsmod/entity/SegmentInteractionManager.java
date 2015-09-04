package brownshome.physicsmod.entity;

import net.minecraft.server.management.ItemInWorldManager;
import net.minecraft.world.World;

public class SegmentInteractionManager extends ItemInWorldManager {
	public SegmentInteractionManager(World worldIn) {
		super(worldIn);
	}
	
	public boolean isCreative() {
		return ((PlayerGhost) thisPlayerMP).getPlayer().theItemInWorldManager.isCreative();
	}
}
