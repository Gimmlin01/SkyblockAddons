package codes.biscuit.skyblockaddons.commands;

import java.util.Collections;
import java.util.List;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import codes.biscuit.skyblockaddons.utils.EnumUtils.Category;
import codes.biscuit.skyblockaddons.utils.Log;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

public class SkyblockStatsCommand extends CommandBase {
	private SkyblockAddons main;
	private Log log;
	
	public SkyblockStatsCommand(SkyblockAddons main) {
		this.main=main;
		this.log=main.getLog();
	}

	@Override
	public String getCommandName() {
		// TODO Auto-generated method stub
		return "skyblockstats";
	}
	
    @Override
    public List<String> getCommandAliases()
    {
        return Collections.singletonList("sbs");
    }

	@Override
	public String getCommandUsage(ICommandSender sender) {
		// TODO Auto-generated method stub
		return null;
	}

	 @Override
	    public boolean canCommandSenderUseCommand(ICommandSender sender)
	    {
	        return true;
	    }

	    /**
	     * Opens the main gui, or locations gui if they type /sba edit
	     */
    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        if (args.length > 0) {
            switch (args[0]) {
            case "debug":
            	main.setDebug(!main.getDebug());
            	log.info("toggled debug mode to "+main.getDebug());
            	break;
            case "show":
            		main.getDatabase().getCount(main.getUtils().getHeldItem());
                break;
            case "clear":
            	if (args.length > 1) {
            		Category category = Category.valueOf(args[1]);
            		main.getDatabase().resetTable(category);
            	}else {
            		main.getDatabase().resetAllTables();
            	}
            	break;
            }
            return;
        }
    }
}
