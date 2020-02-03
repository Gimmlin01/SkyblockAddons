package codes.biscuit.skyblockaddons.commands;

import java.util.Collections;
import java.util.List;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import codes.biscuit.skyblockaddons.utils.EnumUtils.Category;
import codes.biscuit.skyblockaddons.utils.EnumUtils.SeaCreature;
import codes.biscuit.skyblockaddons.utils.Log;
import codes.biscuit.skyblockaddons.utils.database.DbEvent;
import codes.biscuit.skyblockaddons.utils.database.DbItem;
import codes.biscuit.skyblockaddons.utils.database.DbQuery;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;

public class SkyblockStatsCommand extends CommandBase {
	private SkyblockAddons main;
	private Log log;

	public SkyblockStatsCommand(SkyblockAddons main) {
		this.main = main;
		this.log = main.getLog();
	}

	@Override
	public String getCommandName() {
		// TODO Auto-generated method stub
		return "skyblockstats";
	}

	@Override
	public List<String> getCommandAliases() {
		return Collections.singletonList("sbs");
	}

	@Override
	public String getCommandUsage(ICommandSender sender) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean canCommandSenderUseCommand(ICommandSender sender) {
		return true;
	}

	/**
	 * Opens the main gui, or locations gui if they type /sba edit
	 */
	@Override
	public void processCommand(ICommandSender sender, String[] args) {
		DbQuery dbQuery;
		if (args.length > 0) {
			switch (args[0]) {
			case "lol":
				main.getPlayerListener().getlootingFishingEvent().setSeaCreature(SeaCreature.SQUID);
				break;
			case "debug":
				main.setDebug(!main.getDebug());
				log.info("toggled debug mode to " + main.getDebug());
				break;
			case "web":
				if (args.length > 1) {
					switch (args[1]) {
					case "start":
						main.getDatabase().startWebServer();
						main.getLog().info("Visit http://127.0.0.1:8082");
						break;
					case "stop":
						main.getDatabase().stopWebServer();
						main.getLog().info("stopped Webserver");
						break;
					}
				}
				break;
			case "show":
				DbItem dbItem = main.getPlayerListener().getHeldItem();
				if (dbItem != null) {
					try {
						Long duration = Long.parseLong(args[1]) * 1000;
						dbQuery = DbQuery.getItemCount(dbItem.getName(), System.currentTimeMillis() - duration);
						main.getDatabase().getQuery(dbQuery);

					} catch (Exception e) {
						dbQuery = DbQuery.getItemCount(dbItem.getName());
						main.getDatabase().getQuery(dbQuery);
					}
				} else {
					main.getLog().info("Please hold something in your Hand");
				}
				break;
			case "fish":
				long time = System.currentTimeMillis();
				long duration = 120000;
				if (args.length > 1) {
					try {
						duration = Long.parseLong(args[1]) * 1000;
					} catch (NumberFormatException e) {
						if (args[1].equals("all")) {
							duration = time;
						}
					}
				}
				dbQuery = DbQuery.getMeanDuration(Category.FISHING, time - duration, time);
				main.getDatabase().getQuery(dbQuery);
				break;
			case "clear":
				if (args.length > 1) {
					Category category = null;
					if (args.length > 2) {
						category = Category.valueOf(args[2]);
					}
					switch (args[1]) {
					case "event":
						main.getDatabase().resetEventTable(category);
						break;
					case "item":
						main.getDatabase().resetItemTable();
						break;
					}
				} else {
					main.getDatabase().resetAllTables();
				}
				break;
			}
			return;
		}
	}
}
