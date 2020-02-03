package codes.biscuit.skyblockaddons.utils.database;

import java.sql.Statement;

import codes.biscuit.skyblockaddons.utils.EnumUtils.Category;

public class DbQuery extends DbEntry {
	private String sql = "";
	private String name = "";
	private long beg = 0;
	private long end = Long.MAX_VALUE;
	private String what;
	private String table;
	private String where = "";
	private Boolean event = false;
	
	public DbQuery(String sql) {
		this.sql=sql;
	}
	
	public DbQuery(String sql,long beg,long end) {
		this.beg=beg;
		this.end=end;
		this.sql=sql;
	}
	
	public DbQuery(String what,String table,String name,long beg,long end) {
		this.beg=beg;
		this.end=end;
		this.what=what;
		this.table=table;
		this.name=name;
	}
	
	public DbQuery(String what,String table,long beg,long end) {
		this.beg=beg;
		this.end=end;
		this.what=what;
		this.table=table+"_EVENT";
		this.event=true;
	}
	
	public DbQuery(String sql,long beg) {
		this.beg=beg;
		this.sql=sql;
	}
	
	
	public int processAnswer(long id) {
		
		return 0;
	}
	
	public void setSql(String sql) {
		this.sql=sql;
	}
	
	public String getWhere() {
		if (!event)
			return "name = '"+ name.replaceAll("\\'", "\\'\\'") + "' AND timestamp >= "+beg+" AND timestamp <= "+ end +";";
		return "begin >= "+ beg +" AND begin <= " + end + ";";
	}

	public String getSql() {
		if (sql == "") {
			if (where == "") {
				where=getWhere();
			}
			sql= "SELECT "+what+" FROM "+table+" WHERE "+where+";";
		}
		return sql;
	}
	
	public boolean getEvent() {
		return event;
	}
	
	public long getBegin() {
		return beg;
	}
	
	public long getEnd() {
		long time = System.currentTimeMillis();
		if (end >= time)
			return time;
		return end;
	}
	
	public String getName() {
		return name;
	}
	
	
	public static DbQuery getItemCount(String name, long beg, long end) {
		return new DbQuery("count","ITEMS",name,beg,end);
	}
	
	public static DbQuery getItemCount(String name, long beg) {
		return getItemCount(name,beg,Long.MAX_VALUE);
	}
	
	public static DbQuery getItemCount(String name) {
		return getItemCount(name,0,Long.MAX_VALUE);
	}
	
	public static DbQuery getMeanDuration(Category category) {
		return new DbQuery("duration,dbItemIds",category.name(),0,Long.MAX_VALUE);
	}
	
	public static DbQuery getMeanDuration(Category category,long beg) {
		return new DbQuery("duration,dbItemIds",category.name(),beg,Long.MAX_VALUE);
	}
	
	public static DbQuery getMeanDuration(Category category,long beg,long end) {
		return new DbQuery("duration,dbItemIds",category.name(),beg,end);
	}
}
