package codes.biscuit.skyblockaddons.commands;

import java.util.Collections;
import java.util.List;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import codes.biscuit.skyblockaddons.utils.EnumUtils.Category;
import codes.biscuit.skyblockaddons.utils.Log;
import codes.biscuit.skyblockaddons.utils.database.DbEvent;
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
        		main.getDatabase().getDbItem(main.getPlayerListener().getHeldItem());
        		break;
            case "fish":
            	long time=System.currentTimeMillis();
            	long duration = 120000;
            	if (args.length > 1) {
            		try{
            			duration = Long.parseLong(args[1])*1000;
            		}catch (NumberFormatException e) {
            			if (args[1].equals("all")){
            				duration = time;
            			}
            		}
            	}
            	DbEvent event = new DbEvent(Category.FISHING, time, duration,null, null, null);
        		main.getDatabase().getDbEvent(event);
        		break;
            case "clear":
            	if (args.length > 1) {
            		Category category=null;
            		if (args.length > 2) {
            			category=Category.valueOf(args[2]);
            		}
            		switch (args[1]) {
            		case "event":
                		main.getDatabase().resetEventTable(category);
            			break;
            		case "item":
                    	main.getDatabase().resetItemTable(category);
            			break;
            		}
            	}else {
            		main.getDatabase().resetAllTables();
            	}
            	break;
            }
            return;
        }
    }
}
