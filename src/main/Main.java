package main;

import java.io.File;
import java.io.IOException;

import de.nqueensfaf.Solver;

public class Main {

	public static void main(String[] args) {
		try {
			Solver solver;
			if(args.length >= 2) {
				solver = Solver.applyConfig(new File(args[1]));
			} else {
				solver = Solver.applyConfig(de.nqueensfaf.files.Config.getDefaultConfig());
			}
			solver.setN(Integer.parseInt(args[0]));
			solver.setOnProgressUpdateCallback((progress, solutions, duration) -> {
				System.out.print("\rprogress: " + progress + ", solutions: " + solutions + ", duration: " + getTimeStr(duration));
			});
			solver.setTerminationCallback((self) -> {
				System.out.println();
				System.out.println("found " + self.getSolutions() + " solutions in " + getTimeStr(self.getDuration()));
			});
			solver.solve();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
//		if (args.length == 0) { // no command line arguments --> show the Gui
//			Gui gui = new Gui();
//			boolean initialized = false;
//
//			// try to read config file ('nqueensfaf.properties')
//			try {
//				Config.readConfigFile();
//			} catch (FileNotFoundException e) {
//			} catch (IOException e) {
//				gui.init();
//				initialized = true;
//				gui.print("! Invalid content of nqueensfaf.properties file !");
//			}
//			// save the configs at end of program, if needed
//			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
//				if (Config.changed()) {
//					try {
//						Config.writeConfigFile();
//					} catch (FileNotFoundException e) {
//						e.printStackTrace();
//					} catch (IOException e) {
//						e.printStackTrace();
//					}
//				} else {
//					Config.deleteConfigFile();
//				}
//			}));
//
//			if (!initialized)
//				gui.init();
//			gui.setVisible(true);
//		} else {
//			new CLI(args).start();
//		}
	}
	static String getTimeStr(long time) {
		long h = time / 1000 / 60 / 60;
		long m = time / 1000 / 60 % 60;
		long s = time / 1000 % 60;
		long ms = time % 1000;

		String strh, strm, strs, strms;
		// hours
		if (h == 0) {
			strh = "00";
		} else if ((h + "").toString().length() == 3) {
			strh = "" + h;
		} else if ((h + "").toString().length() == 2) {
			strh = "0" + h;
		} else {
			strh = "00" + h;
		}
		// minutes
		if ((m + "").toString().length() == 2) {
			strm = "" + m;
		} else {
			strm = "0" + m;
		}
		// seconds
		if ((s + "").toString().length() == 2) {
			strs = "" + s;
		} else {
			strs = "0" + s;
		}
		// milliseconds
		if ((ms + "").toString().length() == 3) {
			strms = "" + ms;
		} else if ((ms + "").toString().length() == 2) {
			strms = "0" + ms;
		} else {
			strms = "00" + ms;
		}

		return strh + ":" + strm + ":" + strs + "." + strms;
	}
}
