package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

public class Config {

	private static final String filename = "nqueensfaf.properties";

	private static final HashMap<String, String> datatypes = new HashMap<String, String>();
	static {
		datatypes.put("updatesEnabled", 				"boolean");
		datatypes.put("timeUpdateDelay", 				"long");
		datatypes.put("progressUpdateDelay", 			"long");
		datatypes.put("autoSaveEnabled", 				"boolean");
		datatypes.put("autoSavePercentageStep", 		"int");
		datatypes.put("autoSaveFilename", 				"String");
		datatypes.put("autoDeleteEnabled", 				"boolean");
	}
	private static final HashMap<String, Object> defaultConfigs = new HashMap<String, Object>();
	static {
		defaultConfigs.put("updatesEnabled", 				true			);
		defaultConfigs.put("timeUpdateDelay",				0				);
		defaultConfigs.put("progressUpdateDelay", 			0				);
		defaultConfigs.put("autoSaveEnabled", 				false			);
		defaultConfigs.put("autoSavePercentageStep", 		10				);
		defaultConfigs.put("autoSaveFilename", 				"nqueensfaf#N#"	);
		defaultConfigs.put("autoDeleteEnabled", 			false			);
	}
	private static HashMap<String, Object> configs;
	
	public static void readConfigFile() throws FileNotFoundException, IOException {
		Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream(filename)) {
		    props.load(fis);
		} catch (FileNotFoundException ex) {
			configs = defaultConfigs;
			throw new IOException("Could not find nqueensfaf.properties file", ex);
		} catch (IOException ex) {
			configs = defaultConfigs;
			throw new IOException("Error while loading values from nqueensfaf.properties file", ex);
		}
		if(props.keySet().size() == 0) {
			configs = defaultConfigs;
			try {
				new File(filename).delete();
			} catch (SecurityException e) {}
			return;
		}
		configs = new HashMap<String, Object>();
		loop: for(var key : props.keySet()) {
			if(!props.keySet().contains((String) key))
				continue;
			Object value = props.get(key);
			// check if the given value has the correct data type
			switch(datatypes.get(key)) {
			case "boolean":
				if(!(value instanceof Boolean))
					continue loop;
				value = (Boolean) value;
				break;
			case "String":
				if(!(value instanceof String)) {
					continue loop;
				}
				value = (String) value;
				break;
			case "long":
				try {
					value = Long.parseLong((String) value);
				} catch (NumberFormatException e) {
					continue loop;
				}
				break;
			case "int":
				try {
					value = Integer.parseInt((String) value);
				} catch (NumberFormatException e) {
					continue loop;
				}
				break;
			default:
				break;
			}
			configs.put((String) key, value);
		}
	}

	public static Object getValue(String key) {
		if(!datatypes.keySet().contains(key))
			throw new IllegalArgumentException("Invalid config key: '" + key + "'");
		switch(datatypes.get(key)) {
		case "long":
			long l = Long.parseLong(configs.get(key).toString());
			return l;
		case "int":
			int i = Integer.parseInt(configs.get(key).toString());
			return i;
		default:
			return configs.get(key);
		}
	}
	
	public static void setValue(String key, Object value) {
		if(!datatypes.keySet().contains(key))
			throw new IllegalArgumentException("Invalid config key: '" + key + "'");
		configs.put(key, value);
	}
}
