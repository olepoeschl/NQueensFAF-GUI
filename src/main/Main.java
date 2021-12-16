package main;

import java.io.FileNotFoundException;
import java.io.IOException;

import gui.Gui;

public class Main {

	public static void main(String[] args) {
		Gui gui = new Gui();
		boolean initialized = false;
		
		// try to read config file ('nqueensfaf.properties')
		try {
			Config.readConfigFile();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			gui.init();
			initialized = true;
			gui.print("! Invalid content of nqueensfaf.properties file !");
		}
		
		if(!initialized)
			gui.init();
		gui.setVisible(true);
	}

}
