package headless;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.DecimalFormat;

import de.nqueensfaf.Solver;
import de.nqueensfaf.compute.CpuSolver;
import de.nqueensfaf.compute.GpuSolver;
import de.nqueensfaf.compute.SymSolver;
import main.Config;

public class CLI {

	private String[] args;
	private String errstr = "try '-h' or '-help' for more information";

	public CLI(String[] args) {
		this.args = args;
	}

	public void start() {
		CommandLineArguments clArgs;
		try {
			clArgs = new CommandLineArguments(args, "-h", "-help", "-threads", "-gpu", "-device", "-workgroup-size", "-list-gpus");
		} catch (IllegalArgumentException e) {
			System.err.println("error: " + e.getMessage());
			System.out.println(errstr);
			return;
		}

		// variables to be set by the command line arguments
		int threads = -1;
		boolean useGpu;
		int gpuDevice = -1;
		int workgroupSize;

		// some ugly lines of code processing all command line arguments
		// but whatever
		if (clArgs.switchPresent("-h") || clArgs.switchPresent("-help")) {
			help();
			return;
		}
		if (clArgs.switchPresent("-list-gpus")) { // list all available GPU's
			String[] devices;
			try {
				devices = new GpuSolver().getAvailableDevices();
			} catch (IllegalStateException e) {
				// no OpenCL-capable device was found
				// a warning is written by the NQueensFAF library, so we don't need to print
				// anything here
				return;
			}
			// this if statement will never be executed, but whatever, I just leave it here
			if (devices.length == 0) {
				System.err.println("No available GPU's were found");
				return;
			}
			System.out.println("available GPU's:");
			for (int i = 0; i < devices.length; i++) {
				if (i == gpuDevice)
					System.out.println("[X]\tDevice " + i + ": " + devices[i]);
				else
					System.out.println("\tDevice " + i + ": " + devices[i]);
			}
			return;
		}
		try {
			if (clArgs.switchPresent("-threads")) {
				threads = clArgs.switchIntValue("-threads");
			}
		} catch (NumberFormatException e) {
			System.err.println("error: thread count must be a number");
			System.out.println(errstr);
			return;
		}
		useGpu = clArgs.switchPresent("-gpu");
		try {
			if (clArgs.switchPresent("-device")) {
				gpuDevice = clArgs.switchIntValue("-device");
			}
		} catch (NumberFormatException e) {
			System.err.println("error: GPU device index must be a number");
			System.out.println(errstr);
			return;
		}
		try {
			workgroupSize = (int) Config.getDefaultValue("gpuWorkgroupSize");
			if (clArgs.switchPresent("-workgroup-size")) {
				workgroupSize = clArgs.switchIntValue("-workgroup-size");
			}
		} catch (NumberFormatException e) {
			System.err.println("error: workgroup size must be a number");
			System.out.println(errstr);
			return;
		}
		String[] targets = clArgs.targets();
		if (targets.length != 1) {
			help();
			return;
		}
		int N;
		try {
			N = Integer.parseInt(targets[0]);
		} catch (NumberFormatException e) {
			System.err.println("error: board size must be a number");
			System.out.println(errstr);
			return;
		}

		// we got all arguments
		// now start the solver
		try {
			startCommandLineSolver(N, threads, useGpu, gpuDevice, workgroupSize);
		} catch (IllegalArgumentException e) {
			System.err.println("error: " + e.getMessage());
			System.out.println(errstr);
			return;
		}
	}

	private void help() {
		try {
			if (new File(".").getCanonicalPath().endsWith(".exe")) {
				System.out.println("Usage: NQueensFAF.exe");
				System.out.println("           (to use the gui)");
				System.out.println("   or  NQueensFAF.exe <board_size> [options]");
				System.out.println("           (to use it as command line tool)");
			} else {
				System.out.println("Usage: java -jar NQueensFAF.jar");
				System.out.println("           (to use the gui)");
				System.out.println("   or  java -jar NQueensFAF.jar <board_size> [options]");
				System.out.println("           (to use it as command line tool)");
			}
		} catch (IOException e1) {
			System.err.println("error: ");
			e1.printStackTrace();
		}
		System.out.println();
		System.out.println("  available options for command line usage:");
		System.out.println("\t-h, -help \t\tprint this message");
		System.out.println("\t-threads <thread_count> \n\t\t\t\tset the thread count when using CPU");
		System.out.println("\t-gpu \tuse a GPU if possible");
		System.out
				.println("\t-device <index>	\n\t\t\t\tset the index of the GPU that should be used, see '-list-gpus'");
		System.out.println("\t-workgroup-size <workgroup_size> \n\t\t\t\tset the OpenCL workgroup size when using GPU");
		System.out.println("\t-list-gpus \t\tprint a list of all available GPU devices and their indexes");
	}

	private void startCommandLineSolver(int N, int threads, boolean useGpu, int gpuDevice, int workgroupSize) {
		// its not possible to enter negative values because the command line parser
		// grabs all arguments beginning with '-'
		if (N == 0 || N >= 32) {
			throw new IllegalArgumentException("board size must be a number >0 and <32");
		}
		if (useGpu) {
			if (gpuDevice == -1) {
				System.out.println("GPU device index was not specified.. using default device");
				gpuDevice = 0;
			}
			if (workgroupSize == 0) {
				throw new IllegalArgumentException("workgroup size must be a number >0");
			}
		} else {
			if (threads == -1) {
				System.out.println("CPU thread count was not specified.. using single threading");
				threads = 1;
			} else if (threads == 0 || threads > Runtime.getRuntime().availableProcessors()) {
				throw new IllegalArgumentException("thread count must be a number >0 and <="
						+ Runtime.getRuntime().availableProcessors() + " (=available processors)");
			}
		}
		// initialize solver
		Solver solver;
		SymSolver symSolver = new SymSolver();
		if (useGpu) {
			GpuSolver gs = new GpuSolver();
			String[] devices;
			try {
				devices = new GpuSolver().getAvailableDevices();
			} catch (IllegalStateException e) {
				// no OpenCL-capable device was found
				// a warning is written by the NQueensFAF library, so we don't need to print
				// anything here
				return;
			}
			// this if statement will never be executed, but whatever, I just leave it here
			if (devices.length == 0) {
				throw new IllegalArgumentException("No available GPU's were found");
			}
			System.out.println("available GPU's:");
			for (int i = 0; i < devices.length; i++) {
				if (i == gpuDevice)
					System.out.println("[X]\tDevice " + i + ": " + devices[i]);
				else
					System.out.println("\tDevice " + i + ": " + devices[i]);
			}
			if (gpuDevice >= devices.length) {
				throw new IllegalArgumentException(
						"invalid GPU device index: " + gpuDevice + " (only " + devices.length + " devices available)");
			}
			gs.setDevice(gpuDevice);
			solver = gs;
		} else {
			CpuSolver cs = new CpuSolver();
			cs.setThreadcount(threads);
			solver = cs;
		}
		solver.setN(N);
		solver.setProgressUpdateDelay(200);
		final long[] maxOutputLen = new long[] { 0l };
		solver.addTerminationCallback(() -> {
			System.out.print("\r");
			for (int i = 0; i < maxOutputLen[0]; i++) { // 63 is total length of the progress output
				System.out.print("_");
			}
			System.out.println();
			System.out.println("found " + getSolutionsStr(solver.getSolutions()) + " solutions in "
					+ getTimeStr(solver.getDuration()));
			// print unique solution counts
			long solutionsUnique = (solver.getSolutions() + 4 * symSolver.getSolutions180()
					+ 6 * symSolver.getSolutions90()) / 8;
			System.out.println("\nunique solutions:");
			System.out.println("      with  90° symmetry: " + getSolutionsStr(symSolver.getSolutions90()));
			System.out.println("      with 180° symmetry: " + getSolutionsStr(symSolver.getSolutions180()));
			System.out.println("      without   symmetry: "
					+ getSolutionsStr(solutionsUnique - symSolver.getSolutions90() - symSolver.getSolutions180()));
			System.out.println("      in total: " + getSolutionsStr(solutionsUnique));
		});
		String[] loadingChars = new String[] { "|", "/", "-", "\\" };
		final int[] loadingCounter = new int[] { 0 };
		final long[] loadingDuration = new long[] { 0l };
		final float[] loadingProgress = new float[] { 0f };
		final long[] loadingSolutions = new long[] { 0l };
		Thread progressUpdateThread = new Thread(() -> {
			String str, solutionsStr;
			DecimalFormat df = new DecimalFormat();
			df.setMinimumFractionDigits(5);
			df.setMaximumFractionDigits(5);
			while (loadingProgress[0] < 1f) {
				if (loadingCounter[0] >= loadingChars.length)
					loadingCounter[0] = 0;
				solutionsStr = getSolutionsStr(loadingSolutions[0]);
				while (solutionsStr.length() < 30) {
					solutionsStr += " ";
				}
				str = "\r" + loadingChars[loadingCounter[0]++] + "\tduration: " + getTimeStr(loadingDuration[0]) + "\tprogress: " + df.format(loadingProgress[0])
						+ " solutions: " + solutionsStr;
				if(str.length() > maxOutputLen[0]) {
					maxOutputLen[0] = str.length();
				}
				System.out.print(str);
				try {
					Thread.sleep(128);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		});
		solver.setOnProgressUpdateCallback((progress, solutions) -> {
			loadingProgress[0] = progress;
			loadingSolutions[0] = solutions;
		});
		solver.setOnTimeUpdateCallback((duration) -> {
			loadingDuration[0] = duration;
		});

		// read config file
		try {
			Config.readConfigFile();
		} catch (FileNotFoundException e) {
		} catch (IOException e) {
			throw new IllegalStateException("Invalid content of nqueensfaf.properties file");
		}
		// apply config values
		solver.setProgressUpdatesEnabled((boolean) Config.getValue("progressUpdatesEnabled"));
		try {
			solver.setTimeUpdateDelay((long) Config.getValue("timeUpdateDelay"));
		} catch (IllegalArgumentException e) {
			long defaultVal = (long) Config.getDefaultValue("timeUpdateDelay");
			solver.setTimeUpdateDelay(defaultVal);
			Config.resetValue("timeUpdateDelay");
		}
		try {
			solver.setProgressUpdateDelay((long) Config.getValue("progressUpdateDelay"));
		} catch (IllegalArgumentException e) {
			long defaultVal = (long) Config.getDefaultValue("progressUpdateDelay");
			solver.setProgressUpdateDelay(defaultVal);
			Config.resetValue("progressUpdateDelay");
		}
		solver.setAutoSaveEnabled((boolean) Config.getValue("autoSaveEnabled"));
		try {
			solver.setAutoSavePercentageStep((int) Config.getValue("autoSavePercentageStep"));
		} catch (IllegalArgumentException e) {
			int defaultVal = (int) Config.getDefaultValue("autoSavePercentageStep");
			solver.setAutoSavePercentageStep(defaultVal);
			Config.resetValue("autoSavePercentageStep");
		}
		solver.setAutoSaveFilename((String) Config.getValue("autoSaveFilename"));
		solver.setAutoDeleteEnabled((boolean) Config.getValue("autoDeleteEnabled"));
		if (useGpu && workgroupSize == (int) Config.getDefaultValue("gpuWorkgroupSize")) {
			try {
				((GpuSolver) solver).setWorkgroupSize((int) Config.getValue("gpuWorkgroupSize"));
			} catch (IllegalArgumentException e) {
				int defaultVal = (int) Config.getDefaultValue("gpuWorkgroupSize");
				((GpuSolver) solver).setWorkgroupSize(defaultVal);
				Config.resetValue("gpuWorkgroupSize");
			}
		}
		if(useGpu) {
			try {
				((GpuSolver) solver).setNumberOfPresetQueens((int) Config.getValue("presetQueens"));
			} catch (IllegalArgumentException e) {
				int defaultVal = (int) Config.getDefaultValue("presetQueens");
				((GpuSolver) solver).setNumberOfPresetQueens(defaultVal);
				Config.resetValue("presetQueens");
			}
		}

		if (solver.areProgressUpdatesEnabled()) {
			progressUpdateThread.start();
		}

		if (useGpu)
			System.out.println("starting GPU solver for board size " + N + "..");
		else
			System.out.println("starting CPU solver for board size " + N + "..");

		// start symmetric solver for finding also all unique solutions
		symSolver.setN(N);
		symSolver.solveAsync();
		// start the solver
		solver.solve();
	}

	// utility methods
	private String getTimeStr(long time) {
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

	private String getSolutionsStr(long solvecounter) {
		StringBuilder strbuilder = new StringBuilder(Long.toString(solvecounter));
		int len = strbuilder.length();
		for (int i = len - 3; i > 0; i -= 3) {
			strbuilder.insert(i, ".");
		}
		return strbuilder.toString();
	}
}
