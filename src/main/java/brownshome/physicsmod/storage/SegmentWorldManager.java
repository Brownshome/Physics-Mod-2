package brownshome.physicsmod.storage;

import java.util.Iterator;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S25PacketBlockBreakAnim;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.BlockPos;
import net.minecraft.world.WorldManager;
import net.minecraft.world.WorldServer;
import brownshome.physicsmod.PhysicsMod;
import brownshome.physicsmod.network.ServerWrapperPacket;

public class SegmentWorldManager extends WorldManager {
	MinecraftServer server;
	SegmentWorldServer world;

	public SegmentWorldManager(MinecraftServer server, WorldServer world) {
		super(server, world);

		this.server = server;
		this.world = (SegmentWorldServer) world;
	}

	//TODO
	public void playAusSFX(EntityPlayer p_180439_1_, int p_180439_2_, BlockPos blockPosIn, int p_180439_4_) {}
	public void broadcastSound(int p_180440_1_, BlockPos p_180440_2_, int p_180440_3_) {}

	@SuppressWarnings("unchecked")
	public void sendBlockBreakProgress(int breakerId, BlockPos pos, int progress)
	{
		Iterator<EntityPlayerMP> iterator = server.getConfigurationManager().playerEntityList.iterator();

		while (iterator.hasNext()) {
			EntityPlayerMP entityplayermp = iterator.next();

			if (entityplayermp != null && entityplayermp.worldObj == world.getParent() && entityplayermp.getEntityId() != breakerId) {
				double d0 = (double)pos.getX() - entityplayermp.posX;
				double d1 = (double)pos.getY() - entityplayermp.posY;
				double d2 = (double)pos.getZ() - entityplayermp.posZ;

				if (d0 * d0 + d1 * d1 + d2 * d2 < 1024.0D) {
					PhysicsMod.channel.sendTo(new ServerWrapperPacket(new S25PacketBlockBreakAnim(breakerId, pos, progress), world), entityplayermp);
				}
			}
		}
	}
}
