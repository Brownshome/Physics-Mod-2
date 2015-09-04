package brownshome.physicsmod;

import static net.minecraft.util.EnumFacing.DOWN;
import static net.minecraft.util.EnumFacing.HORIZONTALS;
import static net.minecraft.util.EnumFacing.UP;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.minecraft.block.Block;
import net.minecraft.block.BlockAir;
import net.minecraft.block.BlockAnvil;
import net.minecraft.block.BlockBanner;
import net.minecraft.block.BlockBanner.BlockBannerHanging;
import net.minecraft.block.BlockBanner.BlockBannerStanding;
import net.minecraft.block.BlockBarrier;
import net.minecraft.block.BlockBasePressurePlate;
import net.minecraft.block.BlockBeacon;
import net.minecraft.block.BlockBed;
import net.minecraft.block.BlockBrewingStand;
import net.minecraft.block.BlockBush;
import net.minecraft.block.BlockButton;
import net.minecraft.block.BlockCactus;
import net.minecraft.block.BlockCake;
import net.minecraft.block.BlockCarpet;
import net.minecraft.block.BlockCauldron;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockCocoa;
import net.minecraft.block.BlockDaylightDetector;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockDragonEgg;
import net.minecraft.block.BlockEnchantmentTable;
import net.minecraft.block.BlockEndPortal;
import net.minecraft.block.BlockEndPortalFrame;
import net.minecraft.block.BlockEnderChest;
import net.minecraft.block.BlockFarmland;
import net.minecraft.block.BlockFence;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockFire;
import net.minecraft.block.BlockFlowerPot;
import net.minecraft.block.BlockGlass;
import net.minecraft.block.BlockHopper;
import net.minecraft.block.BlockIce;
import net.minecraft.block.BlockLadder;
import net.minecraft.block.BlockLever;
import net.minecraft.block.BlockPane;
import net.minecraft.block.BlockPistonBase;
import net.minecraft.block.BlockPistonExtension;
import net.minecraft.block.BlockPistonMoving;
import net.minecraft.block.BlockPortal;
import net.minecraft.block.BlockRailBase;
import net.minecraft.block.BlockRedstoneDiode;
import net.minecraft.block.BlockRedstoneWire;
import net.minecraft.block.BlockReed;
import net.minecraft.block.BlockSkull;
import net.minecraft.block.BlockSlab;
import net.minecraft.block.BlockSlab.EnumBlockHalf;
import net.minecraft.block.BlockSlime;
import net.minecraft.block.BlockSnow;
import net.minecraft.block.BlockStairs;
import net.minecraft.block.BlockStandingSign;
import net.minecraft.block.BlockTorch;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.BlockTripWire;
import net.minecraft.block.BlockTripWireHook;
import net.minecraft.block.BlockVine;
import net.minecraft.block.BlockWall;
import net.minecraft.block.BlockWallSign;
import net.minecraft.block.BlockWeb;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.Vec3i;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import brownshome.physicsmod.storage.SegmentWorldServer;

public class SepperationHandler {
	public static enum State {
		MOVING, NOTIFYING, IDLE
	}
	
	public static final int MAX_SIZE_SEARCH = 1000;
	public static State state = State.IDLE;
	
	private static int dot(EnumFacing dir, Vec3i v) {
		Vec3i a = dir.getDirectionVec();
		
		return a.getX() * v.getX() + a.getY() * v.getY() + a.getZ() * v.getZ();
	}
	
	public static boolean testSepperationSegment(BlockPos position, SegmentWorldServer container) {
		BlockPos root = container.getRoot();
		HashSet<BlockPos> contents = new HashSet<>();
		ArrayDeque<BlockPos> todo = new ArrayDeque<>();
		
		todo.add(position);
		contents.add(position);
		
		while(todo.size() != 0) {
			if(contents.size() > MAX_SIZE_SEARCH)
				return false;
			
			BlockPos block = todo.pollLast();
			
			EnumFacing[] order = EnumFacing.values();
			Arrays.sort(order, (x, y) -> dot((EnumFacing) x, block) - dot((EnumFacing) y, block));
			
			//test connectivity
			for(EnumFacing dir : order) {
				if(isConnected(block, block.offset(dir), container)) {
					if(block.offset(dir).equals(root)) {
						return false; //this block is connected to the bedrock layer
					}
					
					if(!contents.contains(block.offset(dir))) {
						todo.add(block.offset(dir));
						contents.add(block.offset(dir));
					}
				}
			}
		}
		
		//The block is sepperate from the main group
		createSegment(contents, container);
		return true;
	}
	
	public static boolean testSepperation(BlockPos position, World container) {
		HashSet<BlockPos> contents = new HashSet<>();
		ArrayDeque<BlockPos> todo = new ArrayDeque<>();
		
		todo.add(position);
		contents.add(position);
		
		while(todo.size() != 0) {
			if(contents.size() > MAX_SIZE_SEARCH)
				return false;
			
			BlockPos block = todo.pollLast();
			
			//test connectivity
			
			if(isConnected(block, block.up(), container)) {
				if(Block.isEqualTo(container.getBlockState(block.up()).getBlock(), Blocks.bedrock)) {
					return false; //this block is connected to the bedrock layer
				}
				
				if(!contents.contains(block.up())) {
					todo.add(block.up());
					contents.add(block.up());
				}
			}
			
			for(EnumFacing dir : HORIZONTALS) {
				if(isConnected(block, block.offset(dir), container)) {
					if(Block.isEqualTo(container.getBlockState(block.offset(dir)).getBlock(), Blocks.bedrock)) {
						return false; //this block is connected to the bedrock layer
					}
					
					if(!contents.contains(block.offset(dir))) {
						todo.add(block.offset(dir));
						contents.add(block.offset(dir));
					}
				}
			}
			
			if(isConnected(block, block.down(), container)) {
				if(Block.isEqualTo(container.getBlockState(block.down()).getBlock(), Blocks.bedrock)) {
					return false; //this block is connected to the bedrock layer
				}
				
				if(!contents.contains(block.down())) {
					todo.add(block.down());
					contents.add(block.down());
				}
			}
		}
		
		//The block is sepperate from the main group
		createSegment(contents, container);
		return true;
	}
	
	/** Creates a free segment from the set blocks in container and removes it from the container */
	public static void createSegment(Set<BlockPos> blocks, World container) {
		BlockPos rootOffset = blocks.iterator().next().add(0, -128, 0);
		SegmentWorldServer segment = new SegmentWorldServer((WorldServer) container, rootOffset);
		segment.init();
		
		/* PhysicsWorldEntity entity = new PhysicsWorldEntity(segment);
		entity.forceSpawn = true;
		entity.setLocationAndAngles(rootOffset.getX(), rootOffset.getY(), rootOffset.getZ(), 0, 0);
		container.spawnEntityInWorld(entity); */
		
		state = State.MOVING;
		ArrayList<BlockPos> pass2 = new ArrayList<>(); //for torches as without ASM there is not much I can do about there dropping in mid air -_-
		
		for(BlockPos p : blocks) {
			IBlockState state = container.getBlockState(p);

			if(state.getBlock() instanceof BlockTorch) {
				pass2.add(p);
				continue;
			}
				
			segment.setBlockState(p.subtract(rootOffset), state);
			
			//handle tile entites, overrighting the one created the line above
			TileEntity te = container.getTileEntity(p);
			
			if(te != null) {
				segment.removeTileEntity(p.subtract(rootOffset)); //we delete this ourselves as if the world code does it it deletes our new entity
				container.removeTileEntity(p);
				
				te.validate(); //prepare for addition
				
				segment.setTileEntity(p.subtract(rootOffset), te);
				te.markDirty();
			}
			
			container.setBlockToAir(p);
		}
		
		for(BlockPos p : pass2) {
			segment.setBlockState(p.subtract(rootOffset), container.getBlockState(p));
			container.setBlockToAir(p);
		}
		
		//update position dependant TEs that ignore pos data
		for(BlockPos p : blocks) {
			TileEntity te = segment.getTileEntity(p.subtract(rootOffset));
			if(te == null) continue;
			te.updateContainingBlockInfo();
		}
		
		state = State.NOTIFYING;
		
		for(BlockPos p : blocks) {
			segment.notifyBlockOfStateChange(p.subtract(rootOffset), segment.getBlockState(p.subtract(rootOffset)).getBlock());
			container.notifyNeighborsOfStateChange(p, segment.getBlockState(p.subtract(rootOffset)).getBlock());
			segment.notifyNeighborsOfStateChange(p.subtract(rootOffset), segment.getBlockState(p.subtract(rootOffset)).getBlock());
		}
			
		state = State.IDLE;
	}
	
	static boolean inPresentOnSide(BlockPos pos, IBlockAccess container, EnumFacing face) {
		if(container.isSideSolid(pos, face, false))
			return true;
		
		IBlockState state = container.getBlockState(pos);
		Block block = state.getBlock();
		
		if(1 == 1)
			if(!(block instanceof BlockAir))
				return true;
			else
				return false;
		
		//a switch statement would be better here but is not as safe, sorry to Martin about the ifs :P
		
		//TODO insert custom mod block handlers here
		if(block instanceof BlockAnvil) return face == DOWN;
		if(block instanceof BlockBannerStanding) return face == DOWN;
		if(block instanceof BlockBannerHanging) return face == state.getValue(BlockBanner.FACING);
		if(block instanceof BlockBarrier) return true;
		if(block instanceof BlockBasePressurePlate) return face == DOWN;
		if(block instanceof BlockBeacon) return true;
		if(block instanceof BlockBed) return face == DOWN;
		if(block instanceof BlockBrewingStand) return face == DOWN;
		if(block instanceof BlockBush) return face == DOWN;
		if(block instanceof BlockButton) return face == state.getValue(BlockButton.FACING);
		if(block instanceof BlockCactus) return face == DOWN || face == UP;
		if(block instanceof BlockCake) return face == DOWN;
		if(block instanceof BlockCarpet) return face == DOWN;
		if(block instanceof BlockCauldron) return true;
		if(block instanceof BlockChest) return true;
		if(block instanceof BlockCocoa) return face == state.getValue(BlockDirectional.FACING);
		if(block instanceof BlockDaylightDetector) return face != UP;
		if(block instanceof BlockDoor) return face == DOWN;
		if(block instanceof BlockDragonEgg) return face == DOWN;
		if(block instanceof BlockEnchantmentTable) return face != UP;
		if(block instanceof BlockEnderChest) return true;
		if(block instanceof BlockEndPortal) return face != UP && face != DOWN;
		if(block instanceof BlockEndPortalFrame) return face != UP;
		if(block instanceof BlockFarmland) return face != UP;
		if(block instanceof BlockFence) { //TODO error
			switch(face) {
				case UP:
				case DOWN:
					return true;
					
				case NORTH:
					return (Boolean) state.getValue(BlockFence.NORTH);
					
				case SOUTH:
					return (Boolean) state.getValue(BlockFence.SOUTH);
					
				case WEST:
					return (Boolean) state.getValue(BlockFence.WEST);
					
				case EAST:
					return (Boolean) state.getValue(BlockFence.EAST);
			}
		}
		if(block instanceof BlockFenceGate) return face != state.getValue(BlockFenceGate.FACING) && face != ((EnumFacing) state.getValue(BlockFenceGate.FACING)).getOpposite();
		if(block instanceof BlockFire) return face == DOWN;
		if(block instanceof BlockFlowerPot) return face == DOWN;
		if(block instanceof BlockGlass) return true;
		if(block instanceof BlockHopper) return face == UP || face == state.getValue(BlockHopper.FACING);
		if(block instanceof BlockIce) return true;
		if(block instanceof BlockLadder) return face == UP || face == DOWN || face == state.getValue(BlockLadder.FACING);
		if(block instanceof BlockLever) return face == state.getValue(BlockLever.FACING);
		if(block instanceof BlockPane) {
			switch(face) {
				case UP:
				case DOWN:
					return true;
					
				case NORTH:
					return (Boolean) state.getValue(BlockPane.NORTH);
					
				case SOUTH:
					return (Boolean) state.getValue(BlockPane.SOUTH);
					
				case WEST:
					return (Boolean) state.getValue(BlockPane.WEST);
					
				case EAST:
					return (Boolean) state.getValue(BlockPane.EAST);
			}
		}
		if(block instanceof BlockPistonBase) return true;
		if(block instanceof BlockPistonExtension) return true;
		if(block instanceof BlockPistonMoving) return true; //TODO check this
		if(block instanceof BlockPortal) return face == UP || face == DOWN || !((EnumFacing.Axis) state.getValue(BlockPortal.AXIS)).apply(face);
		if(block instanceof BlockRailBase) return face == DOWN;
		if(block instanceof BlockRedstoneDiode) return face == DOWN;
		if(block instanceof BlockRedstoneWire) return face == DOWN;
		if(block instanceof BlockReed) return true;
		//BlockSign
		if(block instanceof BlockSkull) return face == DOWN;
		if(block instanceof BlockSlab) return ((BlockSlab) block).isDouble() || (face != UP && state.getValue(BlockSlab.HALF) == EnumBlockHalf.BOTTOM) || (face != DOWN && state.getValue(BlockSlab.HALF) == EnumBlockHalf.TOP);
		if(block instanceof BlockSnow) return face == DOWN;
		if(block instanceof BlockStairs) return true;
		if(block instanceof BlockStandingSign) return face == DOWN;
		if(block instanceof BlockSlime) return true;
		if(block instanceof BlockTorch) return face.getOpposite() == state.getValue(BlockTorch.FACING);
		if(block instanceof BlockTrapDoor) {
			switch(face) {
				case UP:
				case DOWN:
					return ((Boolean) state.getValue(BlockTrapDoor.OPEN)) || ((state.getValue(BlockTrapDoor.HALF) == BlockTrapDoor.DoorHalf.BOTTOM) == (face == UP));
					
				default:
					return (!(Boolean) state.getValue(BlockTrapDoor.OPEN)) || face != ((EnumFacing) state.getValue(BlockTrapDoor.FACING)).getOpposite();
			}
		}
		if(block instanceof BlockTripWire) switch(face) {
			case UP:
				return false;
			case DOWN:
				return true;
			case NORTH:
				return (Boolean) state.getValue(BlockTripWire.NORTH);
			case WEST:
				return (Boolean) state.getValue(BlockTripWire.WEST);
			case SOUTH:
				return (Boolean) state.getValue(BlockTripWire.SOUTH);
			case EAST:
				return (Boolean) state.getValue(BlockTripWire.EAST);	
		}
		if(block instanceof BlockTripWireHook) return face == state.getValue(BlockTripWireHook.FACING);
		if(block instanceof BlockVine) switch(face) {
			case UP: return (Boolean) state.getValue(BlockVine.UP);
			case NORTH: return (Boolean) state.getValue(BlockVine.NORTH);
			case DOWN: return false;
			case SOUTH: return (Boolean) state.getValue(BlockVine.SOUTH);
			case EAST: return (Boolean) state.getValue(BlockVine.EAST);
			case WEST: return (Boolean) state.getValue(BlockVine.WEST);
		}
		if(block instanceof BlockWall) switch(face) {
			case UP: case DOWN: return true;
			
			case NORTH: return (Boolean) state.getValue(BlockWall.NORTH);
			case SOUTH: return (Boolean) state.getValue(BlockWall.SOUTH);
			case EAST: return (Boolean) state.getValue(BlockWall.EAST);
			case WEST: return (Boolean) state.getValue(BlockWall.WEST);
		}
		if(block instanceof BlockWallSign) return face.getOpposite() == state.getValue(BlockWallSign.FACING);
		if(block instanceof BlockWeb) return true;
		return false;
	}
	
	static boolean isConnected(BlockPos a, BlockPos b, IBlockAccess container) {
		Vec3i d = b.subtract(a);
		
		return inPresentOnSide(a, container, EnumFacing.getFacingFromVector(d.getX(), d.getY(), d.getZ())) && inPresentOnSide(b, container, EnumFacing.getFacingFromVector(-d.getX(), -d.getY(), -d.getZ()));
	}
}
