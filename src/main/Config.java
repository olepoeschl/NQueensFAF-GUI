package main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;

public class Config {

	private static final String filename = "nqueensfaf.properties";

	private static final HashMap<String, String> datatypes = new HashMap<String, String>();
	static {
		datatypes.put("progressUpdatesEnabled", 		"boolean"	);
		datatypes.put("timeUpdateDelay", 				"long"		);
		datatypes.put("progressUpdateDelay", 			"long"		);
		datatypes.put("autoSaveEnabled", 				"boolean"	);
		datatypes.put("autoSavePercentageStep", 		"int"		);
		datatypes.put("autoSaveFilename", 				"String"	);
		datatypes.put("autoDeleteEnabled", 				"boolean"	);
		datatypes.put("gpuWorkgroupSize", 				"int"		);
	}
	private static final HashMap<String, Object> defaultConfigs = new HashMap<String, Object>();
	static {
		defaultConfigs.put("progressUpdatesEnabled", 		true			);
		defaultConfigs.put("timeUpdateDelay",				128				);
		defaultConfigs.put("progressUpdateDelay", 			128				);
		defaultConfigs.put("autoSaveEnabled", 				false			);
		defaultConfigs.put("autoSavePercentageStep", 		10				);
		defaultConfigs.put("autoSaveFilename", 				"nqueensfaf#N#"	);
		defaultConfigs.put("autoDeleteEnabled", 			false			);
		defaultConfigs.put("gpuWorkgroupSize", 				64				);
	}
	private static HashMap<String, Object> configs;
	
	public static void readConfigFile() throws FileNotFoundException, IOException {
		configs = new HashMap<String, Object>();
		Properties props = new Properties();
		try (FileInputStream fis = new FileInputStream(filename)) {
		    props.load(fis);
		} catch (FileNotFoundException ex) {
			loadDefaultValues();
			throw ex;
		} catch (IOException ex) {
			loadDefaultValues();
			throw new IOException("Error while loading values from nqueensfaf.properties file", ex);
		}
		if(props.keySet().size() == 0) {	// delete config file if it is empty
			loadDefaultValues();
			deleteConfigFile();
			return;
		}
		loop: for(var key : props.keySet()) {
			configs.put(key.toString(), defaultConfigs.get(key));
			if(!props.keySet().contains((String) key))
				continue;
			Object value = props.get(key);
			// check if the given value has the correct data type
			switch(datatypes.get(key)) {
			case "boolean":
				try {
					value = Boolean.parseBoolean(value.toString());
				} catch (NumberFormatException e) {
					continue loop;
				}
				break;
			case "String":
				if(!(value instanceof String)) {
					continue loop;
				}
				value = (String) value;
				break;
			case "long":
				try {
					value = Long.parseLong(value.toString());
				} catch (NumberFormatException e) {
					continue loop;
				}
				break;
			case "int":
				try {
					value = Integer.parseInt(value.toString());
				} catch (NumberFormatException e) {
					continue loop;
				}
				break;
			default:
				break;
			}
			configs.put(key.toString(), value);
		}
	}

	public static void writeConfigFile() throws FileNotFoundException, IOException {
		Properties props = new Properties();
		for(var k : configs.keySet()) {
			props.setProperty(k, configs.get(k).toString());
		}
		try (var out = new FileOutputStream(filename)){
			props.store(out, null);
		}
	}

	public static void deleteConfigFile() {
		try {
			new File(filename).delete();
		} catch (SecurityException e) {}
	}
	
	public static boolean changed() {
		for(var k : defaultConfigs.keySet()) {
			if(!configs.get(k).equals(defaultConfigs.get(k))) {
				return true;
			}
		}
		return false;
	}
	
	public static void loadDefaultValues() {
		configs.clear();
		for(var key : defaultConfigs.keySet()) {
			configs.put(key.toString(), defaultConfigs.get(key));
		}
	}
	
	public static Object getDefaultValue(String key) {
		if(!defaultConfigs.keySet().contains(key))
			throw new IllegalArgumentException("Invalid config key: '" + key + "'");
		switch(datatypes.get(key)) {
		case "boolean":
			boolean b = Boolean.parseBoolean(defaultConfigs.get(key).toString());
			return b;
		case "long":
			long l = Long.parseLong(defaultConfigs.get(key).toString());
			return l;
		case "int":
			int i = Integer.parseInt(defaultConfigs.get(key).toString());
			return i;
		default:
			return defaultConfigs.get(key);
		}
	}
	
	public static Object getValue(String key) {
		if(!datatypes.keySet().contains(key))
			throw new IllegalArgumentException("Invalid config key: '" + key + "'");
		switch(datatypes.get(key)) {
		case "boolean":
			boolean b = Boolean.parseBoolean(configs.get(key).toString());
			return b;
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
		switch(datatypes.get(key)) {
		case "boolean":
			try {
				value = Boolean.parseBoolean(value.toString());
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("expected boolean value, got: " + value.toString());
			}
			break;
		case "String":
			if(!(value instanceof String)) {
				throw new IllegalArgumentException("expected String value, got: " + value.toString());
			}
			value = value.toString();
			break;
		case "long":
			try {
				value = Long.parseLong((String) value);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("expected long value, got: " + value.toString());
			}
			break;
		case "int":
			try {
				value = Integer.parseInt((String) value);
			} catch (NumberFormatException e) {
				throw new IllegalArgumentException("expected int value, got: " + value.toString());
			}
			break;
		default:
			break;
		}
		configs.put(key, value);
	}
	
	public static void resetValue(String key) {
		if(!datatypes.keySet().contains(key))
			throw new IllegalArgumentException("Invalid config key: '" + key + "'");
		configs.put(key, defaultConfigs.get(key));
	}
}
