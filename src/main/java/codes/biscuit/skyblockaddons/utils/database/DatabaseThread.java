package codes.biscuit.skyblockaddons.utils.database;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;

import codes.biscuit.skyblockaddons.utils.Log;
import codes.biscuit.skyblockaddons.utils.EnumUtils.Category;

public class DatabaseThread extends Thread {

	private BlockingQueue<DatabaseMessage> inQueue = null;
	private BlockingQueue<DatabaseMessage> outQueue = null;

	private Connection conn = null;
	private String user;
	private String pwd;
	private String db;
	private Log log;
	private Database parent;

	public DatabaseThread(Database parent, String db, String user, String pwd, BlockingQueue inQueue,
			BlockingQueue outQueue) {
		this.parent = parent;
		this.log = parent.getMain().getLog();
		this.inQueue = inQueue;
		this.outQueue = outQueue;
		this.user = user;
		this.pwd = pwd;
		this.db = db;
	}

	public void run() {
		try {
			System.out.println("Starting DatabaseThread");
			conn = DriverManager.getConnection(db, user, pwd);
			DatabaseMessage dbm = inQueue.take();
			System.out.println("Entering Main Loop");
			// main loop
			while (!dbm.exit) {
				try {
					if (dbm.update) {
						if (dbm.sql != null) {
							Statement stmt = conn.createStatement();
							dbm.affected = stmt.executeUpdate(dbm.sql);
						} else if (dbm.dbItem != null) {
							// log.info("trying to update");
							String sql = "insert into ITEMS values(?,?,?,?,?,?,?,?);";
							PreparedStatement pstmt = conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
							dbm.dbItem.addTo(pstmt);
							int[] aff = pstmt.executeBatch(); 
							dbm.rs = pstmt.getGeneratedKeys();
							try {
								while (dbm.rs.next()) {
									dbm.dbItem.setId(dbm.rs.getLong("ID"));
								}
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							dbm.affected=0;
							for (int i : aff) {
								dbm.affected += i;
							}
						}else if (dbm.dbEvent != null) {
							// log.info("trying to update");
							String sql = "insert into " + dbm.dbEvent.getCategory() + "_EVENT values(?,?,?,?,?,?,?,?,?);";
							PreparedStatement pstmt = conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
							dbm.dbEvent.addTo(pstmt);
							int[] aff = pstmt.executeBatch(); 
							dbm.rs = pstmt.getGeneratedKeys();
							try {
								while (dbm.rs.next()) {
									dbm.dbEvent.setId(dbm.rs.getLong("ID"));
								}
							} catch (SQLException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							dbm.affected=0;
							for (int i : aff) {
								dbm.affected += i;
							}
						}
					} else {
						if (dbm.sql != null) {
							Statement stmt = conn.createStatement();
							dbm.rs = stmt.executeQuery(dbm.sql);
						} else if (dbm.dbQuery != null) {
							Statement stmt = conn.createStatement();
							dbm.sql = dbm.dbQuery.getSql();
							dbm.rs = stmt.executeQuery(dbm.sql);							
						}else {
							log.info("nothing to do");
						}
					}
					outQueue.put(dbm);
					dbm = inQueue.take();
				} catch (SQLException e) {
					int errorCode = e.getErrorCode();
					if (errorCode == 90067) {
						//session closed
						log.debug("Session Closed");
						inQueue.put(dbm);
						break;
					}else {
					log.debug(""+e.getCause());

					log.debug(""+e.getErrorCode());
					e.printStackTrace();

					dbm = inQueue.take();
					}
				}catch (Exception e) {
					log.debug(""+e.getCause());

					log.debug(""+e.getMessage());
					e.printStackTrace();

					dbm = inQueue.take();
				}
			}
		}catch (SQLException e) {
			// TODO Auto-generated catch block
			log.debug(""+e.getErrorCode());
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException se) {
				se.printStackTrace();
			} finally {
				try {
					outQueue.put(new DatabaseMessage(true));
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} // end try

	}
	
	private void sendStatement(String sql,DatabaseMessage dbm) throws SQLException {
		PreparedStatement pstmt = conn.prepareStatement(sql,Statement.RETURN_GENERATED_KEYS);
		dbm.dbEvent.addTo(pstmt);
		int[] aff = pstmt.executeBatch(); 
		dbm.rs = pstmt.getGeneratedKeys();
		try {
			while (dbm.rs.next()) {
				dbm.dbEvent.setId(dbm.rs.getLong("ID"));
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (int i : aff) {
			dbm.affected = i;
		}
	}

}
