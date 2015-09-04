package brownshome.physicsmod.server;

import java.util.Arrays;
import java.util.List;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommand;
import net.minecraft.command.ICommandSender;
import net.minecraft.util.BlockPos;
import brownshome.physicsmod.storage.SegmentWorldClient;
import brownshome.physicsmod.storage.SegmentWorldServer;

public class TogglePhysicsCommand implements ICommand {
	List<String> element = Arrays.asList("server", "client", "link", "tick");
	List<String> options = Arrays.asList("off", "on");
	List<String> names = Arrays.asList("p", "physics");
	
	@Override
	public int compareTo(Object o) {
		return getName().compareTo(((ICommand)o).getName());
	}

	@Override
	public String getName() {
		return "physics";
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		return "/physics [side] <on|off>";
	}

	@Override
	public List<String> getAliases() {
		return names;
	}

	@Override
	public void execute(ICommandSender sender, String[] args) throws CommandException {
		boolean on = true;
		
		if(args.length != 0)
			switch(args.length == 2 ? args[1] : args[0]) {
				case "on":
					on = true;
					break;
				case "off":
					on = false;
					break;
			}
		
		switch(args.length == 2 ? args[0] : "") {
			case "server":
				SegmentWorldServer.physTick = on;
				break;
			case "client":
				SegmentWorldClient.physTick = on;
				break;
			case "link":
				SegmentWorldClient.link = on;
				break;
			default:
				SegmentWorldClient.link = on;
			case "tick":
				SegmentWorldServer.physTick = on;
				SegmentWorldClient.physTick = on;
				break;
		}
	}

	@Override
	public boolean canCommandSenderUse(ICommandSender sender) {
		return true;
	}

	@Override
	public List<String> addTabCompletionOptions(ICommandSender sender, String[] args, BlockPos pos) {
		switch(args.length) {
			case 1:
				return element;
			case 2:
				return options;
		}
		
		return null;
	}

	@Override
	public boolean isUsernameIndex(String[] args, int index) {
		return false;
	}

}
