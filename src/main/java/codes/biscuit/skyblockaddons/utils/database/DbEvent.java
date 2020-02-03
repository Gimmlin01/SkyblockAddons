package codes.biscuit.skyblockaddons.utils.database;

import java.lang.reflect.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import codes.biscuit.skyblockaddons.utils.EnumUtils.Bait;
import codes.biscuit.skyblockaddons.utils.EnumUtils.Category;
import codes.biscuit.skyblockaddons.utils.EnumUtils.Location;
import codes.biscuit.skyblockaddons.utils.EnumUtils.SeaCreature;

public class DbEvent extends DbEntry{

	private long begin;

	private long end;

	private long duration;
	
	private List<Long> dbItemsIds = new ArrayList <Long>();

	private Location location=Location.NULL;

	private DbItem heldItem;

	private Bait bait=Bait.NULL;
	
	private SeaCreature seaCreature=SeaCreature.NULL;

	
	public DbEvent(Category category, long begin, long end, List dbItems, Location location, DbItem heldItem) {
		this.category=category;
		this.begin = begin;
		this.end = end;
		this.duration = end - begin;
		this.location = location;
		this.heldItem = heldItem;
	}

	public DbEvent(Category category, long begin, Location location, DbItem heldItem, Bait bait) {
		this.category=category;
		this.begin = begin;
		this.location = location;
		this.heldItem = heldItem;
		this.bait=bait;
	}

	public void calcDuration() {
		duration = end - begin;
	}
	
	public int processAnswer(long id) {
		dbItemsIds.add(id);
		return dbItemsIds.size();
	}

	public void addTo(PreparedStatement pstmt) throws SQLException {
		pstmt.setString(1, null);
		pstmt.setLong(2, begin);
		pstmt.setLong(3, end);
		pstmt.setLong(4, duration);
		pstmt.setObject(5, dbItemsIds.toArray());
		pstmt.setString(6, location.name());
		if (heldItem != null) {
			pstmt.setString(7, heldItem.getName());

		} else {
			pstmt.setString(7, "NULL");
		}
		pstmt.setString(8, bait.name());
		pstmt.setString(9, seaCreature.name());
		pstmt.addBatch();
	}

	public long getBegin() {
		return this.begin;
	}

	public void setBegin(long value) {
		this.begin = value;
	}

	public long getEnd() {
		return this.end;
	}

	public void setEnd(long value) {
		this.end = value;
	}

	public long getDuration() {
		return this.duration;
	}

	public void setDuration(long value) {
		this.duration = value;
	}

	public Location getLocation() {
		return this.location;
	}

	public void setLocation(Location value) {
		this.location = value;
	}

	public DbItem getheldItem() {
		return this.heldItem;
	}

	public void setheldItem(DbItem value) {
		this.heldItem = value;
	}

	public Bait getBait() {
		return this.bait;
	}

	public void setBait(Bait value) {
		this.bait = value;
	}
	
	public SeaCreature getSeaCreature() {
		return this.seaCreature;
	}

	public void setSeaCreature(SeaCreature seaCreature) {
		this.seaCreature = seaCreature;
	}
}