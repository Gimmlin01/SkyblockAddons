package codes.biscuit.skyblockaddons.utils.database;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;

import codes.biscuit.skyblockaddons.utils.Log;

public class DatabaseThread extends Thread {

	private BlockingQueue<DatabaseMessage> inQueue = null;
	private BlockingQueue<DatabaseMessage> outQueue = null;
	
	
	private Connection conn=null;
	private String user;
	private String pwd;
	private String db;
	private Log log;
	private Database parent;
	
	public DatabaseThread (Database parent, String db, String user, String pwd, BlockingQueue inQueue,BlockingQueue outQueue){
		this.parent=parent;
		this.log=parent.getMain().getLog();
		this.inQueue=inQueue;
		this.outQueue=outQueue;
		this.user=user;
		this.pwd=pwd;
		this.db=db;
	}
	
	public void run() {
		try {
	        System.out.println("Starting DatabaseThread");
			conn = DriverManager.getConnection("jdbc:h2:"+db, user, pwd);
			DatabaseMessage dbm=inQueue.take();
	        System.out.println("Entering Main Loop");
			// main loop
			while (!dbm.exit) {
				try {
					if (dbm.update) {
				        System.out.println("isUpdate");
						if (dbm.sql != null) {
							Statement stmt = conn.createStatement();
							dbm.affected = stmt.executeUpdate(dbm.sql);
						}else if (dbm.dbItem!=null) {
							//log.info("trying to update");
							PreparedStatement pstmt = conn.prepareStatement("insert into "+dbm.dbItem.category+" values(?,?,?,?,?,?,?)");
							dbm.dbItem.addTo(pstmt);
							int[] aff = pstmt.executeBatch(); // Insert the users
							for (int i : aff) {
								dbm.affected=i;
							}
							pstmt.close();
						}
					} else {
				        System.out.println("isQuery");
				        if (dbm.sql != null) {
							Statement stmt = conn.createStatement();
							dbm.rs = stmt.executeQuery(dbm.sql);
				        }else if (dbm.dbItem != null) {
							Statement stmt = conn.createStatement();
				        	dbm.sql = "SELECT count FROM "+dbm.dbItem.category+ 
				        			" WHERE name = '"+dbm.dbItem.name.replaceAll("\\'", "\\'\\'")+"';";
							dbm.rs = stmt.executeQuery(dbm.sql);
				        } else {
				        	log.info("nothing to do");
				        }
					}
					outQueue.put(dbm);
				
				
				} catch (Exception e){
					e.printStackTrace();
				} finally {
					dbm=inQueue.take();
				}
			}
			outQueue.put(new DatabaseMessage(true));
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}finally{
		      try{
		         if(conn!=null)
		            conn.close();
		      }catch(SQLException se){
		         se.printStackTrace();
		      }//end finally try
		   }//end try
		
	}
	
	
}
