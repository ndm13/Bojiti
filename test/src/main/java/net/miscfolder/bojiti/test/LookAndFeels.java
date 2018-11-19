package net.miscfolder.bojiti.test;

import javax.swing.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class LookAndFeels{
	public static void main(String[] args){
		for(Map.Entry<String,String> laf : getMap().entrySet())
			System.out.println(laf.getKey() + " | " + laf.getValue());
	}

	private static Map<String,String> lafMap;
	private static final Object loadLock = new Object();

	public static Map<String,String> getMap(){
		if(lafMap != null) return lafMap;
		synchronized(loadLock){
			if(lafMap != null) return lafMap;
			Map<String,String> map = new HashMap<>();
			for(UIManager.LookAndFeelInfo lafi : UIManager.getInstalledLookAndFeels())
				map.put(lafi.getName(), lafi.getClassName());
			return lafMap = Collections.unmodifiableMap(map);
		}
	}

	public static String getOrSystem(String name){
		return getMap().getOrDefault(name, UIManager.getSystemLookAndFeelClassName());
	}

	public static String getOrCrossPlatform(String name){
		return getMap().getOrDefault(name, UIManager.getCrossPlatformLookAndFeelClassName());
	}
}
