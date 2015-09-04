package brownshome.physicsmod.entity;

import net.minecraft.entity.IMerchant;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.IInventory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.IInteractionObject;
import brownshome.physicsmod.network.SegmentNetHandlerServer;
import brownshome.physicsmod.storage.SegmentWorldServer;

//This is created every time an EntityPlayer connects to the server or a segment is created. NB: this entity only exist server side
public class PlayerGhost extends EntityPlayerMP {
	EntityPlayerMP player;
	
	public PlayerGhost(EntityPlayerMP player, SegmentWorldServer segment) {
		super(MinecraftServer.getServer(), segment, player.getGameProfile(), new SegmentInteractionManager(segment));
		
		segment.updatePlayerPos(player, this);
		this.player = player;
		
		new SegmentNetHandlerServer(this);
		
		inventory = player.inventory;
		openContainer = player.openContainer;
	}
	
	public void markPlayerActive() {
        player.markPlayerActive();
    }

	public EntityPlayerMP getPlayer() {
		return player;
	}
	
	public void updateOpenContainer() {
		this.openContainer = getPlayer().openContainer;
	}
	
	public void displayGUIChest(IInventory chestInventory) {
		getPlayer().displayGUIChest(chestInventory);
		updateOpenContainer();
	}
	
	public void displayGui(IInteractionObject guiOwner) {
		getPlayer().displayGui(guiOwner);
		updateOpenContainer();
	}
	
	public void closeScreen() {
		getPlayer().closeScreen();
		updateOpenContainer();
	}
	
	public void closeContainer() {
		getPlayer().closeContainer();
		updateOpenContainer();
	}
	
	public void displayGUIHorse(EntityHorse horse, IInventory inventory) {
		getPlayer().displayGUIHorse(horse, inventory);
		updateOpenContainer();
	}
	
	public void displayVillagerTradeGui(IMerchant villager) {
		getPlayer().displayVillagerTradeGui(villager);
		updateOpenContainer();
	}

	public boolean isSneaking() {
		return getPlayer().isSneaking();
	}
	
	public void onUpdate() {
		super.onUpdate();
		((SegmentWorldServer) worldObj).updatePlayerPos(getPlayer(), this); //TODO find other times pos is updated
		updateOpenContainer();
	}
}
