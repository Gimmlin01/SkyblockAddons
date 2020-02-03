package codes.biscuit.skyblockaddons.utils.database;

import java.util.ArrayList;
import java.util.List;

import codes.biscuit.skyblockaddons.utils.EnumUtils.Category;

public abstract class DbEntry {
	long id = -1;
	protected Category category;
	protected List<Integer> dependencyMsgs=new ArrayList<Integer>();
	
	abstract int processAnswer(long id);

	public long getId() {
		return this.id;
	}

	public void setId(long value) {
		this.id = value;
	}
	
	public Category getCategory() {
		return this.category;
	}

	public void setCategory(Category value) {
		this.category = value;
	}
	
	public List<Integer> getDependencyMsgs(){
		return dependencyMsgs;
	}
	
	public int addDependency(Integer id) {
		dependencyMsgs.add(id);
		return dependencyMsgs.size();
	}

}
