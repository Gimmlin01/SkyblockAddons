package codes.biscuit.skyblockaddons.utils.database;

import java.sql.ResultSet;

public class DatabaseMessage {
	public String sql=null;
	public int id=-1;
	public boolean update=false;
	public ResultSet rs=null;
	public int affected=-1;
	public boolean createifnotexist=true;
	public DbItem dbItem=null;
	public boolean exit=false;
	
	DatabaseMessage(boolean exit){
		this.exit=exit;
	}
	
	DatabaseMessage(int id,Boolean update,String sql){
		this.id=id;
		this.sql=sql;
		this.update=update;
	}
	
	DatabaseMessage(int id,Boolean update,DbItem dbItem){
		this.id=id;
		this.dbItem=dbItem;
		this.update=update;
	}
	
	DatabaseMessage(int id,Boolean update, ResultSet rs){
		this.id=id;
		this.rs=rs;
		this.update=update;
	}
	
	DatabaseMessage(int id,Boolean update, int aff){
		this.id=id;
		this.affected=aff;
		this.update=update;
	}

}
