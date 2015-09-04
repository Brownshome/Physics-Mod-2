package brownshome.physicsmod.blocks;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.creativetab.CreativeTabs;
import brownshome.physicsmod.PhysicsMod;

public class BlockThruster extends Block {
	public static final String NAME = "thruster";
	
	public BlockThruster() {
		super(Material.iron);
		
		setHardness(0.3F);
		setStepSound(Block.soundTypeMetal);
		setUnlocalizedName(PhysicsMod.MODID + "_" + NAME);
		setCreativeTab(CreativeTabs.tabBlock);
	}
	
	public String getName() {
		return NAME;
	}
}