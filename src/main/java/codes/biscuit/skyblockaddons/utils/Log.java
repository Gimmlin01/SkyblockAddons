package codes.biscuit.skyblockaddons.utils;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;


import codes.biscuit.skyblockaddons.SkyblockAddons;
public class Log {
	private boolean ingame = false;
	public SkyblockAddons main;
	  Logger logger = Logger.getLogger("MyLog");  
	    FileHandler fh;  

 

	
	public Log(SkyblockAddons main) {
		this.main=main;
	    try {  
	        // This block configure the logger with handler and formatter  
	        fh = new FileHandler(main.getDir()+"/sbs.log");  
	        logger.addHandler(fh);
	        SimpleFormatter formatter = new SimpleFormatter();  
	        fh.setFormatter(formatter);  
	        logger.setLevel(Level.ALL);

	    } catch (SecurityException e) {  
	        e.printStackTrace();  
	    } catch (IOException e) {  
	        e.printStackTrace();  
	    } 
	}
	
	public void info(String string) {
		if (ingame) {
			main.getUtils().sendMessage(string);
		}else {
	        System.out.println(string);
		}
		logger.info(string);
	}
	
	public void debug(String string) {
		if (main.getDebug()) {
			if (ingame) {
				main.getUtils().sendMessage(string);
			}else {
		        System.out.println(string);
			}
		}
		logger.log(Level.FINER, string);
	}
	
	public void setIngame(Boolean ingame) {
		this.ingame = ingame;
	}
}
