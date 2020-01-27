package codes.biscuit.skyblockaddons.utils.database;

import java.sql.Connection;
import org.h2.tools.Server;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;

import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.h2.jdbc.JdbcSQLNonTransientConnectionException;
import codes.biscuit.skyblockaddons.SkyblockAddons;
import codes.biscuit.skyblockaddons.utils.EnumUtils.Category;
import net.minecraft.client.Minecraft;
import codes.biscuit.skyblockaddons.utils.Log;

public class Database {
	private String port = "31415";
	private String user = "sbs";
	private String pwd = "123";
	private String conn = "jdbc:h2:tcp://localhost:" + port + "/";
	private String db = "/sbs_database";
	private String dir = "";
	private DatabaseThread dbt = null;
	private BlockingQueue<DatabaseMessage> inQueue = null;
	private BlockingQueue<DatabaseMessage> outQueue = null;
	private int id = 0;
	private SkyblockAddons main;
	private Multimap<Integer, DbEntry> entrysWaitingForAnswer;
	private List<DatabaseMessage> messageHistory;
	private Server server;

	public Database(SkyblockAddons main) {
		this.main = main;
		this.dir = main.getDir();
		inQueue = new ArrayBlockingQueue<DatabaseMessage>(1024);
		outQueue = new ArrayBlockingQueue<DatabaseMessage>(1024);
		entrysWaitingForAnswer = ArrayListMultimap.create();
		messageHistory = new ArrayList<DatabaseMessage>();
		startServer();

	}

	public void startServer() {
		try {
			main.getLog().debug("Starting TCP Server on Port: " + port);
			server = Server.createTcpServer("-tcpPort", port, "-tcpAllowOthers").start();
			startDatabaseThread();
		} catch (JdbcSQLNonTransientConnectionException e) {
			main.getLog().debug("Cannot Bind Port " + port + ". Server Already Running? Trying to connect...");
			startDatabaseThread();
			// TODO Auto-generated catch block
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void restartServer() {
		stopServer();
		startServer();
	}

	public void stopServer() {
		if (server != null && server.isRunning(true)) {
			server.shutdown();
		}
	}

	public void startDatabaseThread() {
		main.getLog().debug("Starting Database Thread");
		List<DatabaseMessage> oldMessages = new ArrayList<>();
		inQueue.drainTo(oldMessages);
		dbt = new DatabaseThread(this, conn + dir + db, user, pwd, inQueue, outQueue);
		dbt.start();
		initAllTables();
		for (DatabaseMessage dbm : oldMessages) {
			inQueue.add(dbm);
		}
	}

	public void checkResults() {
		DatabaseMessage dbm = outQueue.poll();
		while (dbm != null) {
			if (dbm.exit) {
				try {
					dbt.join();
					main.getLog().info("Database Thread died. Restarting everything in 10s");
					new Timer().schedule(new TimerTask() {
						@Override
						public void run() {
							main.getLog().info("restarting Server");
							restartServer();
						}
					}, 10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else if (dbm.update) {
				main.getLog().debug(dbm.sql + " affected " + dbm.affected);
				if (dbm.dbItem != null && main.getDebug()) {
					getDbItem(dbm.dbItem);
				}

				if (dbm.dbItem != null && dbm.dbItem.getId() >= 0) {
					ArrayList<DbEntry> waiting = new ArrayList(entrysWaitingForAnswer.get(dbm.id));
					ArrayList<DbEntry> remove = new ArrayList();
					for (DbEntry dbe : waiting) {
						dbe.processAnswer(dbm.dbItem.getId());
						remove.add(dbe);
					}
					waiting.removeAll(remove);
				}
			} else {
				try {
					if (dbm.dbItem != null) {
						int sum = 0;
						while (dbm.rs.next()) {
							sum += dbm.rs.getInt("count");
						}
						main.getLog().info(sum + " " + dbm.dbItem.getName() + " so far");
					} else if (dbm.dbEvent != null) {

						long sum = 0;
						int i = 0;
						while (dbm.rs.next()) {
							i++;
							sum += dbm.rs.getLong("duration");
						}
						if (i != 0) {
							double mean = sum / i / 1000.0;
							main.getLog().info("Mean Fishing Time: " + mean + "s. " + i + " Events");
						} else {
							main.getLog().info("No Data");
							main.getLog().info(dbm.sql);
						}

					} else {
						main.getLog().info("nothing to do");
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			dbm.done = true;
			dbm = outQueue.poll();
		}
	}

	public int addDbItem(DbItem dbItem) {
		if (dbItem.getCount() >= 1) {
			DatabaseMessage dbm = new DatabaseMessage(this.id, true, dbItem);
			send(dbm);
			return dbm.id;
		}
		return -1;
	}

	public void addDbEvent(DbEvent dbEvent) {
		if (dbEvent != null) {
			if (!dbEvent.getDependencyMsgs().isEmpty()) {
				boolean retry = false;
				for (Integer msgId : dbEvent.getDependencyMsgs()) {
					Boolean msgDone = false;
					try {
						msgDone = messageHistory.get(msgId).done;
					} catch (IndexOutOfBoundsException e) {
						// do nothing
					}

					if (msgDone) {
						main.getLog().debug("item already done");
						dbEvent.processAnswer(messageHistory.get(msgId).dbItem.getId());
					} else if (!entrysWaitingForAnswer.containsEntry(msgDone, dbEvent)) {
						main.getLog().debug("waiting");
						entrysWaitingForAnswer.put(msgId, dbEvent);
						// retry
						retry = true;

					} else {
						main.getLog().debug("still waiting");
						retry = true;
					}
				}
				if (retry) {
					new Timer().schedule(new TimerTask() {
						@Override
						public void run() {
							addDbEvent(dbEvent);
						}
					}, 500);
					return;
				}
			}
			DatabaseMessage dbm = new DatabaseMessage(this.id, true, dbEvent);
			send(dbm);
		}
	}

	public void getDbItem(DbItem dbItem) {
		DatabaseMessage dbm = new DatabaseMessage(this.id, false, dbItem);
		send(dbm);
	}

	public void getDbEvent(DbEvent dbEvent) {
		DatabaseMessage dbm = new DatabaseMessage(this.id, false, dbEvent);
		send(dbm);
	}

	public int send(DatabaseMessage dbm) {
		messageHistory.add(dbm);
		this.id += 1;
		try {
			inQueue.put(dbm);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return this.id;
	}

	public void dropItemTable(Category category) {
		String sql = "DROP TABLE " + category;
		DatabaseMessage dbm = new DatabaseMessage(this.id, true, sql);
		this.id++;
		try {
			inQueue.put(dbm);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void dropEventTable(Category category) {
		String sql = "DROP TABLE " + category + "_EVENT";
		DatabaseMessage dbm = new DatabaseMessage(this.id, true, sql);
		this.id++;
		try {
			inQueue.put(dbm);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void resetItemTable(Category category) {
		if (category != null) {
			dropItemTable(category);
			initItemTable(category);
			main.getLog().info(category + " was reset");
		} else {
			resetAllItemTables();
		}
	}

	public void resetEventTable(Category category) {
		if (category != null) {
			dropEventTable(category);
			initEventTable(category);
			main.getLog().info(category + " was reset");
		} else {
			resetAllEventTables();
		}
	}

	public void resetAllEventTables() {
		for (Category cat : Category.values()) {
			resetEventTable(cat);
		}
	}

	public void resetAllItemTables() {
		for (Category cat : Category.values()) {
			resetItemTable(cat);
		}
	}

	public void initAllEventTables() {
		for (Category cat : Category.values()) {
			initEventTable(cat);
		}
	}

	public void initAllItemTables() {
		for (Category cat : Category.values()) {
			initItemTable(cat);
		}
	}

	public void resetAllTables() {
		resetAllItemTables();
		resetAllEventTables();
	}

	public void initAllTables() {
		initAllItemTables();
		initAllEventTables();
	}

	public void initItemTable(Category category) {
		main.getLog().info("init " + category);
		switch (category) {
		case FISHING:
			initFishingTable();
			break;
		case MISC:
			initMiscTable();
			break;
		default:
			main.getLog().info("Cannot init Table - " + category + " not found!");
		}
	}

	public void initEventTable(Category category) {
		main.getLog().info("init " + category + "_EVENT");
		switch (category) {
		case FISHING:
			initFishingEventTable();
			break;
		case MISC:
			initMiscEventTable();
			break;
		default:
			main.getLog().info("Cannot init Table - " + category + " not found!");
		}
	}

	public void initFishingTable() {
		String sql = ("CREATE TABLE IF NOT EXISTS " + Category.FISHING + " (" + "  id LONG NOT NULL AUTO_INCREMENT,"
				+ "  timestamp LONG," + "  name VARCHAR(255)," + "  count INTEGER," + "  location ENUM("
				+ "    'ISLAND'," + "    'VILLAGE'," + "    'AUCTION_HOUSE'," + "    'BANK'," + "    'LIBRARY',"
				+ "    'COAL_MINE'," + "    'GRAVEYARD'," + "    'COLOSSEUM'," + "    'WILDERNESS'," + "    'MOUNTAIN',"
				+ "    'WIZARD_TOWER'," + "    'RUINS'," + "    'FOREST'," + "    'FARM'," + "    'FISHERMANS_HUT',"
				+ "    'HIGH_LEVEL'," + "    'FLOWER_HOUSE'," + "    'CANVAS_ROOM'," + "    'TAVERN',"
				+ "    'BIRCH_PARK'," + "    'SPRUCE_WOODS'," + "    'JUNGLE_ISLAND'," + "    'SAVANNA_WOODLAND',"
				+ "    'DARK_THICKET'," + "    'GOLD_MINE'," + "    'DEEP_CAVERNS'," + "    'GUNPOWDER_MINES',"
				+ "    'LAPIS_QUARRY'," + "    'PIGMAN_DEN'," + "    'SLIMEHILL'," + "    'DIAMOND_RESERVE',"
				+ "    'OBSIDIAN_SANCTUARY'," + "    'THE_BARN'," + "    'MUSHROOM_DESERT'," + "    'SPIDERS_DEN',"
				+ "    'BLAZING_FORTRESS'," + "    'THE_END'," + "    'DRAGONS_NEST'," + "    'NULL'" + "  ),"
				+ "  heldItem VARCHAR(255)," + "  bait ENUM('NONE', 'FISH', 'SPIKED')," + "  PRIMARY KEY (id)" + ")");
		DatabaseMessage dbm = new DatabaseMessage(this.id, true, sql);
		send(dbm);
	}

	public void initMiscTable() {
		String sql = ("CREATE TABLE IF NOT EXISTS " + Category.MISC + " (" + "  id LONG NOT NULL AUTO_INCREMENT,"
				+ "  timestamp LONG," + "  name VARCHAR(255)," + "  count INTEGER," + "  location ENUM("
				+ "    'ISLAND'," + "    'VILLAGE'," + "    'AUCTION_HOUSE'," + "    'BANK'," + "    'LIBRARY',"
				+ "    'COAL_MINE'," + "    'GRAVEYARD'," + "    'COLOSSEUM'," + "    'WILDERNESS'," + "    'MOUNTAIN',"
				+ "    'WIZARD_TOWER'," + "    'RUINS'," + "    'FOREST'," + "    'FARM'," + "    'FISHERMANS_HUT',"
				+ "    'HIGH_LEVEL'," + "    'FLOWER_HOUSE'," + "    'CANVAS_ROOM'," + "    'TAVERN',"
				+ "    'BIRCH_PARK'," + "    'SPRUCE_WOODS'," + "    'JUNGLE_ISLAND'," + "    'SAVANNA_WOODLAND',"
				+ "    'DARK_THICKET'," + "    'GOLD_MINE'," + "    'DEEP_CAVERNS'," + "    'GUNPOWDER_MINES',"
				+ "    'LAPIS_QUARRY'," + "    'PIGMAN_DEN'," + "    'SLIMEHILL'," + "    'DIAMOND_RESERVE',"
				+ "    'OBSIDIAN_SANCTUARY'," + "    'THE_BARN'," + "    'MUSHROOM_DESERT'," + "    'SPIDERS_DEN',"
				+ "    'BLAZING_FORTRESS'," + "    'THE_END'," + "    'DRAGONS_NEST'," + "    'NULL'" + "  ),"
				+ "  heldItem VARCHAR(255)," + "  bait ENUM('NONE', 'FISH', 'SPIKED')," + "  PRIMARY KEY (id)" + ")");
		DatabaseMessage dbm = new DatabaseMessage(this.id, true, sql);
		send(dbm);
	}

	public void initFishingEventTable() {
		String sql = ("CREATE TABLE IF NOT EXISTS " + Category.FISHING + "_EVENT ("
				+ "  id LONG NOT NULL AUTO_INCREMENT," + "  begin LONG," + "  end LONG," + "  duration LONG,"
				+ "  dbItemIds ARRAY[256]," + "  location ENUM(" + "    'ISLAND'," + "    'VILLAGE',"
				+ "    'AUCTION_HOUSE'," + "    'BANK'," + "    'LIBRARY'," + "    'COAL_MINE'," + "    'GRAVEYARD',"
				+ "    'COLOSSEUM'," + "    'WILDERNESS'," + "    'MOUNTAIN'," + "    'WIZARD_TOWER'," + "    'RUINS',"
				+ "    'FOREST'," + "    'FARM'," + "    'FISHERMANS_HUT'," + "    'HIGH_LEVEL',"
				+ "    'FLOWER_HOUSE'," + "    'CANVAS_ROOM'," + "    'TAVERN'," + "    'BIRCH_PARK',"
				+ "    'SPRUCE_WOODS'," + "    'JUNGLE_ISLAND'," + "    'SAVANNA_WOODLAND'," + "    'DARK_THICKET',"
				+ "    'GOLD_MINE'," + "    'DEEP_CAVERNS'," + "    'GUNPOWDER_MINES'," + "    'LAPIS_QUARRY',"
				+ "    'PIGMAN_DEN'," + "    'SLIMEHILL'," + "    'DIAMOND_RESERVE'," + "    'OBSIDIAN_SANCTUARY',"
				+ "    'THE_BARN'," + "    'MUSHROOM_DESERT'," + "    'SPIDERS_DEN'," + "    'BLAZING_FORTRESS',"
				+ "    'THE_END'," + "    'DRAGONS_NEST'," + "    'NULL'" + "  )," + "  heldItem VARCHAR(255),"
				+ "  bait ENUM('NONE', 'FISH', 'SPIKED')," + "  PRIMARY KEY (id)" + ")");
		DatabaseMessage dbm = new DatabaseMessage(this.id, true, sql);
		send(dbm);
	}

	public void initMiscEventTable() {
		String sql = ("CREATE TABLE IF NOT EXISTS " + Category.MISC + "_EVENT (" + "  id LONG NOT NULL AUTO_INCREMENT,"
				+ "  begin LONG," + "  end LONG," + "  duration LONG," + "  dbItemIds ARRAY[256," + "  location ENUM("
				+ "    'ISLAND'," + "    'VILLAGE'," + "    'AUCTION_HOUSE'," + "    'BANK'," + "    'LIBRARY',"
				+ "    'COAL_MINE'," + "    'GRAVEYARD'," + "    'COLOSSEUM'," + "    'WILDERNESS'," + "    'MOUNTAIN',"
				+ "    'WIZARD_TOWER'," + "    'RUINS'," + "    'FOREST'," + "    'FARM'," + "    'FISHERMANS_HUT',"
				+ "    'HIGH_LEVEL'," + "    'FLOWER_HOUSE'," + "    'CANVAS_ROOM'," + "    'TAVERN',"
				+ "    'BIRCH_PARK'," + "    'SPRUCE_WOODS'," + "    'JUNGLE_ISLAND'," + "    'SAVANNA_WOODLAND',"
				+ "    'DARK_THICKET'," + "    'GOLD_MINE'," + "    'DEEP_CAVERNS'," + "    'GUNPOWDER_MINES',"
				+ "    'LAPIS_QUARRY'," + "    'PIGMAN_DEN'," + "    'SLIMEHILL'," + "    'DIAMOND_RESERVE',"
				+ "    'OBSIDIAN_SANCTUARY'," + "    'THE_BARN'," + "    'MUSHROOM_DESERT'," + "    'SPIDERS_DEN',"
				+ "    'BLAZING_FORTRESS'," + "    'THE_END'," + "    'DRAGONS_NEST'," + "    'NULL'" + "  ),"
				+ "  heldItem VARCHAR(255)," + "  bait ENUM('NONE', 'FISH', 'SPIKED')," + "  PRIMARY KEY (id)" + ")");
		DatabaseMessage dbm = new DatabaseMessage(this.id, true, sql);
		send(dbm);
	}

	public SkyblockAddons getMain() {
		return main;
	}

	public void finalize() throws Throwable {
		stopServer();
		super.finalize();
	}

}
