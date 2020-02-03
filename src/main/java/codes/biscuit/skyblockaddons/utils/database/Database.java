package codes.biscuit.skyblockaddons.utils.database;

import java.lang.reflect.Array;
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
import codes.biscuit.skyblockaddons.utils.EnumUtils.Bait;
import codes.biscuit.skyblockaddons.utils.EnumUtils.Category;
import codes.biscuit.skyblockaddons.utils.EnumUtils.Location;
import codes.biscuit.skyblockaddons.utils.EnumUtils.SeaCreature;
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
	private Server webserver;
	private long start = Long.MIN_VALUE;
	private long stop= Long.MAX_VALUE;

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
			server = Server.createTcpServer("-tcpPort", port, "-ifNotExists").start();
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
	
	public void startWebServer() {
		try {
			main.getLog().debug("Starting Web Server on Port: 8082");
			webserver = Server.createWebServer().start();
		} catch (JdbcSQLNonTransientConnectionException e) {
			main.getLog().debug("Cannot Bind Port 8082. Server Already Running? Trying to connect...");
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
	
	public void stopWebServer() {
		if (webserver != null && webserver.isRunning(true)) {
			webserver.shutdown();
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
				if (dbm.dbItem != null && main.getDebug()) {
					DbQuery dbQuery = DbQuery.getItemCount(dbm.dbItem.getName());
					getQuery(dbQuery);
				}

				if (dbm.dbItem != null && dbm.dbItem.getId() >= 0) {
					main.getLog().debug("Got Back Id: " + dbm.dbItem.getId());
					ArrayList<DbEntry> waiting = new ArrayList(entrysWaitingForAnswer.get(dbm.id));
					ArrayList<DbEntry> remove = new ArrayList();
					for (DbEntry dbe : waiting) {
						int size = dbe.processAnswer(dbm.dbItem.getId());
						remove.add(dbe);
						main.getLog().info("Processed Answer new Size: " + size);
					}
					waiting.removeAll(remove);
				}
			} else {
				try {
					if (dbm.dbQuery != null){
						if (dbm.dbQuery.getEvent()) {
							long sum = 0;
							Object[] array;
							int i = 0;
							int j = 0;
							while (dbm.rs.next()) {
								j++;
								array = (Object[]) dbm.rs.getArray("dbItemIds").getArray();
								boolean valid = false;
								for (Object obj : array ) {
									if (obj != null) {
										valid=true;
									}
								}
								if (valid) {
									i++;
									sum += dbm.rs.getLong("duration");
								}
							}
							if (i != 0) {
								double mean = sum / i / 1000.0;
								main.getLog().info("Mean Fishing Time: " + mean + "s. " + i + " valid Events " + j + " not");
							} else {
								main.getLog().info("No valid Data");
								main.getLog().info(dbm.sql);
							}
						}else {
							int sum = 0;
							while (dbm.rs.next()) {
								sum += dbm.rs.getInt("count");
							}
							if (dbm.dbQuery.getBegin() <= 0) {
								main.getLog().info(sum + " " + dbm.dbQuery.getName() + " so far");
							} else {
								long delta =dbm.dbQuery.getEnd() - dbm.dbQuery.getBegin();
								double perSecond=sum*1000.0/delta;
								main.getLog().info(sum + " " + dbm.dbQuery.getName() + " since "
										+ (delta/ 1000) + "s => " + perSecond + " per second");
							}
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
						e.printStackTrace();
						// do nothing
					}

					if (msgDone) {
						main.getLog().debug("item already done");
						dbEvent.processAnswer(messageHistory.get(msgId).dbItem.getId());
					} else if (!entrysWaitingForAnswer.containsEntry(msgId, dbEvent)) {
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

	public void getQuery(DbQuery dbQuery) {
		DatabaseMessage dbm = new DatabaseMessage(this.id, dbQuery);
		send(dbm);
	}

//	public void getDbEvent(DbEvent dbEvent) {
//		DatabaseMessage dbm = new DatabaseMessage(this.id, false, dbEvent);
//		send(dbm);
//	}

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

	public void dropItemTable() {
		String sql = "DROP TABLE ITEMS";
		DatabaseMessage dbm = new DatabaseMessage(this.id, true, sql);
		send(dbm);
	}

	public void dropEventTable(Category category) {
		String sql = "DROP TABLE " + category + "_EVENT";
		DatabaseMessage dbm = new DatabaseMessage(this.id, true, sql);
		send(dbm);
	}

	public void resetItemTable() {
		dropItemTable();
		initItemTable();
		main.getLog().info("Item Table was reset");

	}

	public void resetEventTable(Category category) {
		if (category != null) {
			dropEventTable(category);
			initEventTable(category);
			main.getLog().info(category + "_EVENT Table was reset");
		} else {
			resetAllEventTables();
		}
	}

	public void resetAllEventTables() {
		for (Category cat : Category.values()) {
			resetEventTable(cat);
		}
	}

	public void initAllEventTables() {
		for (Category cat : Category.values()) {
			initEventTable(cat);
		}
	}

	public void resetAllTables() {
		resetItemTable();
		resetAllEventTables();
	}

	public void initAllTables() {
		initItemTable();
		initAllEventTables();
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

	public void initItemTable() {
		String sql = ("CREATE TABLE IF NOT EXISTS ITEMS ( " + "id LONG NOT NULL AUTO_INCREMENT, " + "timestamp LONG, "
				+ "name VARCHAR(255), " + "count INTEGER, " + Location.getSql() + "heldItem VARCHAR(255), "
				+ Bait.getSql() + Category.getSql()	+ "PRIMARY KEY (id)" + ")");
		DatabaseMessage dbm = new DatabaseMessage(this.id, true, sql);
		send(dbm);
	}

	public void initFishingEventTable() {
		String sql = ("CREATE TABLE IF NOT EXISTS " + Category.FISHING + "_EVENT ("
				+ "id LONG NOT NULL AUTO_INCREMENT, " + "begin LONG, " + "end LONG, " + "duration LONG, "
				+ "dbItemIds ARRAY[256], " + Location.getSql() + "heldItem VARCHAR(255), "
				+ Bait.getSql() + SeaCreature.getSql() + "PRIMARY KEY (id)" + ")");
		DatabaseMessage dbm = new DatabaseMessage(this.id, true, sql);
		send(dbm);
	}

	public void initMiscEventTable() {
		String sql = ("CREATE TABLE IF NOT EXISTS " + Category.MISC + "_EVENT (" + "  id LONG NOT NULL AUTO_INCREMENT,"
				+ "  begin LONG," + "  end LONG," + "  duration LONG," + "  dbItemIds ARRAY[256]," + "  location ENUM("
				+ "    'ISLAND'," + "    'VILLAGE'," + "    'AUCTION_HOUSE'," + "    'BANK'," + "    'LIBRARY',"
				+ "    'COAL_MINE'," + "    'GRAVEYARD'," + "    'COLOSSEUM'," + "    'WILDERNESS'," + "    'MOUNTAIN',"
				+ "    'WIZARD_TOWER'," + "    'RUINS'," + "    'FOREST'," + "    'FARM'," + "    'FISHERMANS_HUT',"
				+ "    'HIGH_LEVEL'," + "    'FLOWER_HOUSE'," + "    'CANVAS_ROOM'," + "    'TAVERN',"
				+ "    'BIRCH_PARK'," + "    'SPRUCE_WOODS'," + "    'JUNGLE_ISLAND'," + "    'SAVANNA_WOODLAND',"
				+ "    'DARK_THICKET'," + "    'GOLD_MINE'," + "    'DEEP_CAVERNS'," + "    'GUNPOWDER_MINES',"
				+ "    'LAPIS_QUARRY'," + "    'PIGMAN_DEN'," + "    'SLIMEHILL'," + "    'DIAMOND_RESERVE',"
				+ "    'OBSIDIAN_SANCTUARY'," + "    'THE_BARN'," + "    'MUSHROOM_DESERT'," + "    'SPIDERS_DEN',"
				+ "    'BLAZING_FORTRESS'," + "    'THE_END'," + "    'DRAGONS_NEST'," + "    'NULL'" + "  ),"
				+ "  heldItem VARCHAR(255)," + "  bait ENUM('NULL', 'FISH', 'SPIKED')," + "  PRIMARY KEY (id)" + ")");
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
