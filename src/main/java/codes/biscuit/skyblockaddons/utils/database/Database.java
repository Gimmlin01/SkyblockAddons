package codes.biscuit.skyblockaddons.utils.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import codes.biscuit.skyblockaddons.SkyblockAddons;
import codes.biscuit.skyblockaddons.utils.EnumUtils.Category;
import codes.biscuit.skyblockaddons.utils.Log;


public class Database {
	private String user = "sbs";
	private String pwd = "123";
	private String db = "/sbs_database";
	private String dir = "";
	private DatabaseThread dbt = null;
	private BlockingQueue<DatabaseMessage> inQueue = null;
	private BlockingQueue<DatabaseMessage> outQueue = null;
	private int id = 0;
	private SkyblockAddons main;
	
	public Database(SkyblockAddons main) {
		this.main=main;
		this.dir = main.getDir();
		inQueue=new ArrayBlockingQueue<DatabaseMessage>(1024);
		outQueue=new ArrayBlockingQueue<DatabaseMessage>(1024);
		startDatabaseThread();
		initAllTables();
		
	}
	
	public void startDatabaseThread() {		
		dbt=new DatabaseThread(this,dir+db, user, pwd, inQueue,outQueue);
		dbt.start();
	}
	
	public void checkResults() {
		DatabaseMessage answer = outQueue.poll();
		while (answer != null) {
			if (answer.exit) {
				try {
					dbt.join();
					main.getLog().info("restarting Database Thread");
					startDatabaseThread();
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else if (answer.update) {
				main.getLog().debug(answer.sql+" affected " + answer.affected);
				if (answer.dbItem != null && main.getDebug()) {
					getCount(answer.dbItem);
				}
			}else {
				try {
					if (answer.dbItem != null) {
						int sum=0;
						while (answer.rs.next()) {
							sum += answer.rs.getInt("count");
						}
						main.getLog().info(sum +" "+ answer.dbItem.name + " so far");
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			answer=outQueue.poll();
		}
	}
	
	
	public void addDbItem(DbItem dbItem) {
		if (dbItem.count >=1) {
			DatabaseMessage dbm = new DatabaseMessage(this.id,true,dbItem);
			this.id+=1;
			try {
				inQueue.put(dbm);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public void getCount(DbItem dbItem) {
		DatabaseMessage dbm = new DatabaseMessage(this.id,false,dbItem);
		this.id+=1;
		try {
			inQueue.put(dbm);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	
	
	
	
	
	
	public void dropTable(Category category) {
		String sql = "DROP TABLE " + category;
		DatabaseMessage dbm = new DatabaseMessage(this.id,true,sql);
		this.id++;
		try {
			inQueue.put(dbm);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}	
		
	}
	
	public void resetTable(Category category) {
		dropTable(category);
		initTable(category);
		main.getLog().info(category+" was reset");
	}
	
	public void resetAllTables() {
		for (Category cat :Category.values()) {
			resetTable(cat);
		}
	}
	
	public void initAllTables() {
		for (Category cat :Category.values()) {
			initTable(cat);
		}
	}
	
	public void initTable(Category category) {
    	main.getLog().info("init "+category);
		switch (category) {
		case FISHING:
        	initFishingTable();
        	break;
		case MISC:
        	initMiscTable();
        	break;
        default:
        	main.getLog().info("Cannot init Table - "+ category + " not found!");
		}
	}
	
	
	public void initFishingTable() {
    	main.getLog().info("init fish");
		String sql = ("CREATE TABLE IF NOT EXISTS "+Category.FISHING+" (" + 
				"  id LONG NOT NULL AUTO_INCREMENT," + 
				"  timestamp LONG," + 
				"  name VARCHAR(255)," + 
				"  count INTEGER," + 
				"  location ENUM(" + 
				"    'ISLAND'," + 
				"    'VILLAGE'," + 
				"    'AUCTION_HOUSE'," + 
				"    'BANK'," + 
				"    'LIBRARY'," + 
				"    'COAL_MINE'," + 
				"    'GRAVEYARD'," + 
				"    'COLOSSEUM'," + 
				"    'WILDERNESS'," + 
				"    'MOUNTAIN'," + 
				"    'WIZARD_TOWER'," + 
				"    'RUINS'," + 
				"    'FOREST'," + 
				"    'FARM'," + 
				"    'FISHERMANS_HUT'," + 
				"    'HIGH_LEVEL'," + 
				"    'FLOWER_HOUSE'," + 
				"    'CANVAS_ROOM'," + 
				"    'TAVERN'," + 
				"    'BIRCH_PARK'," + 
				"    'SPRUCE_WOODS'," + 
				"    'JUNGLE_ISLAND'," + 
				"    'SAVANNA_WOODLAND'," + 
				"    'DARK_THICKET'," + 
				"    'GOLD_MINE'," + 
				"    'DEEP_CAVERNS'," + 
				"    'GUNPOWDER_MINES'," + 
				"    'LAPIS_QUARRY'," + 
				"    'PIGMAN_DEN'," + 
				"    'SLIMEHILL'," + 
				"    'DIAMOND_RESERVE'," + 
				"    'OBSIDIAN_SANCTUARY'," + 
				"    'THE_BARN'," + 
				"    'MUSHROOM_DESERT'," + 
				"    'SPIDERS_DEN'," + 
				"    'BLAZING_FORTRESS'," + 
				"    'THE_END'," + 
				"    'DRAGONS_NEST'," +
				"    'NULL'" + 
				"  )," + 
				"  heldItem VARCHAR(255)," + 
				"  bait ENUM('NONE', 'FISH', 'SPIKED')," + 
				"  PRIMARY KEY (id)" + 
				")");
		DatabaseMessage createTable = new DatabaseMessage(this.id,true,sql);
		this.id+=1;
		try {
			inQueue.put(createTable);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public void initMiscTable() {
    	main.getLog().info("init misc");
		String sql = ("CREATE TABLE IF NOT EXISTS "+Category.MISC+" (" + 
				"  id LONG NOT NULL AUTO_INCREMENT," + 
				"  timestamp LONG," + 
				"  name VARCHAR(255)," + 
				"  count INTEGER," + 
				"  location ENUM(" + 
				"    'ISLAND'," + 
				"    'VILLAGE'," + 
				"    'AUCTION_HOUSE'," + 
				"    'BANK'," + 
				"    'LIBRARY'," + 
				"    'COAL_MINE'," + 
				"    'GRAVEYARD'," + 
				"    'COLOSSEUM'," + 
				"    'WILDERNESS'," + 
				"    'MOUNTAIN'," + 
				"    'WIZARD_TOWER'," + 
				"    'RUINS'," + 
				"    'FOREST'," + 
				"    'FARM'," + 
				"    'FISHERMANS_HUT'," + 
				"    'HIGH_LEVEL'," + 
				"    'FLOWER_HOUSE'," + 
				"    'CANVAS_ROOM'," + 
				"    'TAVERN'," + 
				"    'BIRCH_PARK'," + 
				"    'SPRUCE_WOODS'," + 
				"    'JUNGLE_ISLAND'," + 
				"    'SAVANNA_WOODLAND'," + 
				"    'DARK_THICKET'," + 
				"    'GOLD_MINE'," + 
				"    'DEEP_CAVERNS'," + 
				"    'GUNPOWDER_MINES'," + 
				"    'LAPIS_QUARRY'," + 
				"    'PIGMAN_DEN'," + 
				"    'SLIMEHILL'," + 
				"    'DIAMOND_RESERVE'," + 
				"    'OBSIDIAN_SANCTUARY'," + 
				"    'THE_BARN'," + 
				"    'MUSHROOM_DESERT'," + 
				"    'SPIDERS_DEN'," + 
				"    'BLAZING_FORTRESS'," + 
				"    'THE_END'," + 
				"    'DRAGONS_NEST'," + 
				"    'NULL'" +
				"  )," + 
				"  heldItem VARCHAR(255)," + 
				"  bait ENUM('NONE', 'FISH', 'SPIKED')," + 
				"  PRIMARY KEY (id)" + 
				")");
		DatabaseMessage createTable = new DatabaseMessage(this.id,true,sql);
		this.id+=1;
		try {
			inQueue.put(createTable);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}		
	}
	
	public SkyblockAddons getMain() {
		return main;
	}
	
}
