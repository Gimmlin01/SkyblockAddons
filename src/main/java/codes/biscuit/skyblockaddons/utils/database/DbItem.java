package codes.biscuit.skyblockaddons.utils.database;

import java.sql.PreparedStatement;
import java.sql.SQLData;
import java.sql.SQLException;
import java.sql.SQLInput;
import java.sql.SQLOutput;
import java.util.Date;
import codes.biscuit.skyblockaddons.utils.EnumUtils.Location;
import codes.biscuit.skyblockaddons.utils.EnumUtils.Bait;
import codes.biscuit.skyblockaddons.utils.EnumUtils.Category;
import codes.biscuit.skyblockaddons.utils.ItemDiff;
import net.minecraft.item.ItemStack;

public class DbItem extends DbEntry{
	private long timestamp;
	private Location location;
	private String name;
	private int count;
	private String heldItem;
	private Bait bait;

	public DbItem(ItemDiff diff,Location location,DbItem heldItem) {
		this.timestamp=new Date().getTime();
		this.location=location;
		this.name=diff.getDisplayName();
		this.count=diff.getAmount();
		this.bait=Bait.NONE;
		this.category=Category.getCategory(this.name);
		if (heldItem != null) {
			this.heldItem=heldItem.getName();
		}
	}

	public DbItem(ItemStack item, Location location) {
		this.timestamp = new Date().getTime();
		this.location = location;
		this.name = item.getDisplayName();
		this.count = item.stackSize;
		this.bait = Bait.NONE;
		this.category = Category.getCategory(this.name);
	}
	
	public void processAnswer(long id) {
		
	}

	public void readSQL(SQLInput stream, String typeName) throws SQLException {
		id = stream.readLong();
		timestamp = stream.readLong();
		name = stream.readString();
		count = stream.readInt();
		location = Location.valueOf(stream.readString());
		heldItem = stream.readString();
		bait = Bait.valueOf(stream.readString());
	}

	public void addTo(PreparedStatement pstmt) throws SQLException {
		pstmt.setString(1, null);
		pstmt.setLong(2, timestamp);
		pstmt.setString(3, name);
		pstmt.setInt(4, count);
		pstmt.setString(5, location.name());
		pstmt.setString(6, heldItem);
		pstmt.setString(7, bait.name());
		pstmt.addBatch();
	}

	long getTimestamp() {
		return this.timestamp;
	}

	public Location getLocation() {
		return this.location;
	}

	public void setLocation(Location value) {
		this.location = value;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String value) {
		this.name = value;
	}

	public String getHeldItem() {
		return this.heldItem;
	}

	public void setHeldItem(String value) {
		this.heldItem = value;
	}

	public Bait getBait() {
		return this.bait;
	}

	public void setBait(Bait value) {
		this.bait = value;
	}

	public int getCount() {
		return this.count;
	}

	public void setCount(int value) {
		this.count = value;
	}


}
