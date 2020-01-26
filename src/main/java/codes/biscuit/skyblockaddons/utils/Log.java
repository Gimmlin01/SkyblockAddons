package codes.biscuit.skyblockaddons.utils;

import codes.biscuit.skyblockaddons.SkyblockAddons;
public class Log {
	private boolean ingame = false;
	public SkyblockAddons main;
	
	public Log(SkyblockAddons main) {
		this.main=main;
	}
	
	public void info(String string) {
		if (ingame) {
			main.getUtils().sendMessage(string);
		}else {
	        System.out.println(string);
		}
	}
	
	public void debug(String string) {
		if (main.getDebug())
			info(string);
	}
	
	public void setIngame(Boolean ingame) {
		this.ingame = ingame;
	}
}
