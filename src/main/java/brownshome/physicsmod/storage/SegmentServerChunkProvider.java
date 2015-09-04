package brownshome.physicsmod.storage;

import java.util.HashSet;

import net.minecraft.network.play.server.S21PacketChunkData;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.chunk.storage.IChunkLoader;
import net.minecraft.world.gen.ChunkProviderServer;

public class SegmentServerChunkProvider extends ChunkProviderServer {
	HashSet<Long> nonEmptyChunks = new HashSet<>();

	public SegmentServerChunkProvider(WorldServer world, IChunkLoader diskLoader, IChunkProvider chunkGenerator) {
		super(world, diskLoader, chunkGenerator);
	}
	
	public boolean chunkExists(int x, int z) {
		return nonEmptyChunks.contains(ChunkCoordIntPair.chunkXZ2Int(x, z)) && id2ChunkMap.containsItem(ChunkCoordIntPair.chunkXZ2Int(x, z));
	}
	
	public void activateChunk(int x, int z) {
		long c = ChunkCoordIntPair.chunkXZ2Int(x, z);
		if(nonEmptyChunks.add(c))
			id2ChunkMap.remove(c);

		c = ChunkCoordIntPair.chunkXZ2Int(x + 1, z);
		if(nonEmptyChunks.add(c))
			id2ChunkMap.remove(c);
		
		c = ChunkCoordIntPair.chunkXZ2Int(x - 1, z);
		if(nonEmptyChunks.add(c))
			id2ChunkMap.remove(c);
		
		c = ChunkCoordIntPair.chunkXZ2Int(x, z + 1);
		if(nonEmptyChunks.add(c))
			id2ChunkMap.remove(c);
		
		c = ChunkCoordIntPair.chunkXZ2Int(x, z - 1);
		if(nonEmptyChunks.add(c))
			id2ChunkMap.remove(c);
	}
}