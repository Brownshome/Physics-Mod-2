package brownshome.physicsmod.storage;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.world.WorldServer;

public class DummyEntityTracker extends EntityTracker {
	public DummyEntityTracker(WorldServer server) {
		super(server);
	}

	public void updateTrackedEntities() {
		//do nothing
	}
	
	public void addEntityToTracker(Entity p_72785_1_, int p_72785_2_, final int p_72785_3_, boolean p_72785_4_) {
		//also do nothing
	}
}
