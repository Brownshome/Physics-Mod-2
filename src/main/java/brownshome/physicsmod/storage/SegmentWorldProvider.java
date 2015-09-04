package brownshome.physicsmod.storage;

import net.minecraft.world.World;
import net.minecraft.world.WorldProvider;
import net.minecraft.world.chunk.IChunkProvider;
import brownshome.physicsmod.storage.ISegment;

public class SegmentWorldProvider extends WorldProvider {
	World parent;
	
	protected void registerWorldChunkManager() {
		super.registerWorldChunkManager();
		parent = ((ISegment) worldObj).getParent();
	}
	
	@Override
	public String getDimensionName() {
		return "SEG" + dimensionId;
	}

	@Override
	public String getInternalNameSuffix() {
		return "SEG" + dimensionId;
	}

	public String getSaveFolder() {
		if(parent.provider.getDimensionId() == 0) 
			return "physics/SEG" + ((ISegment) worldObj).getIDNumber();
		else
			return parent.provider.getSaveFolder() + "/physics/SEG" + ((ISegment) worldObj).getIDNumber();
    }
	
	public IChunkProvider createChunkGenerator() {
        return new ChunkProviderEmpty(worldObj);
    }
}
