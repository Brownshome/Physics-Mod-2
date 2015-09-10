package brownshome.physicsmod.blocks;

import java.util.Random;

import javax.vecmath.Vector3f;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyDirection;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.Vec3i;
import net.minecraft.world.World;
import brownshome.physicsmod.PhysicsMod;
import brownshome.physicsmod.storage.ISegment;

public class BlockThruster extends Block {
	public static final String NAME = "thruster";
	public static final PropertyDirection FACING = PropertyDirection.create("facing");
	public static final PropertyBool ON = PropertyBool.create("triggered");

	double thrustStrength;

	public BlockThruster(double strength) {
		super(Material.iron);

		thrustStrength = strength;
		setHardness(0.3F);
		setStepSound(Block.soundTypeMetal);
		setUnlocalizedName(PhysicsMod.MODID + "_" + NAME);
		setCreativeTab(CreativeTabs.tabBlock);
		setDefaultState(blockState.getBaseState().withProperty(ON, false).withProperty(FACING, EnumFacing.NORTH));
	}

	public int getMetaFromState(IBlockState state) {
		boolean on = (Boolean) state.getValue(ON);
		int side = ((EnumFacing) state.getValue(FACING)).getIndex();

		return side + (on ? 6 : 0);
	}

	public IBlockState getStateFromMeta(int meta) {
		IBlockState state = getDefaultState();
		
		if(meta >= 6) {
			state.withProperty(ON, true);
			state.withProperty(FACING, EnumFacing.VALUES[meta - 6]);
		} else {
			state.withProperty(ON, false);
			state.withProperty(FACING, EnumFacing.VALUES[meta]);
		}
		
		return state;
	}

	public String getName() {
		return NAME;
	}

	protected BlockState createBlockState() {
		return new BlockState(this, new IProperty[] {FACING, ON});
	}

	public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
		boolean powered = worldIn.isBlockPowered(pos);
		IBlockState state = getDefaultState().withProperty(FACING, getFacingFromEntity(worldIn, pos, placer)).withProperty(ON, powered);
		
		if(powered && worldIn instanceof ISegment)
			worldIn.scheduleUpdate(pos, this, 1);
		
		return state;
	}

	public void updateTick(World worldIn, BlockPos pos, IBlockState state, Random rand) {
		if(worldIn.isBlockPowered(pos) && worldIn instanceof ISegment) {
			worldIn.scheduleUpdate(pos, this, 1);
			((ISegment) worldIn).addForce(pos, getForce(worldIn, pos, state));
		}
	}
	
	public Vector3f getForce(World worldIn, BlockPos pos, IBlockState state) {
		Vec3i vec = ((EnumFacing) state.getValue(FACING)).getOpposite().getDirectionVec();
		return new Vector3f((float) (vec.getX() * thrustStrength), (float) (vec.getY() * thrustStrength), (float) (vec.getZ() * thrustStrength));
	}

	public void onNeighborBlockChange(World worldIn, BlockPos pos, IBlockState state, Block neighborBlock) {
		boolean powered = worldIn.isBlockPowered(pos);
		
		if (!worldIn.isRemote)
			worldIn.setBlockState(pos, state.withProperty(ON, powered));
		
		if(powered && worldIn instanceof ISegment)
			worldIn.scheduleUpdate(pos, this, 1);
	}

	public static EnumFacing getFacingFromEntity(World worldIn, BlockPos clickedBlock, EntityLivingBase entityIn) {
		if (MathHelper.abs((float)entityIn.posX - (float)clickedBlock.getX()) < 2.0F && MathHelper.abs((float)entityIn.posZ - (float)clickedBlock.getZ()) < 2.0F) {
			double d0 = entityIn.posY + (double)entityIn.getEyeHeight();

			if (d0 - (double)clickedBlock.getY() > 2.0D) {
				return EnumFacing.UP;
			}

			if ((double)clickedBlock.getY() - d0 > 0.0D) {
				return EnumFacing.DOWN;
			}
		}

		return entityIn.getHorizontalFacing().getOpposite();
	}
}