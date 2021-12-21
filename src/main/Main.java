package main;

import java.io.FileNotFoundException;
import java.io.IOException;

import de.nqueensfaf.Solver;
import de.nqueensfaf.compute.CpuSolver;
import de.nqueensfaf.compute.GpuSolver;
import gui.Gui;
import headless.CommandLineArguments;

public class Main {

	public static void main(String[] args) {
		if(args.length == 0) {		// no command line arguments --> show the Gui
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
			// save the configs at end of program, if needed
			Runtime.getRuntime().addShutdownHook(new Thread(() -> {
				if(Config.changed()) {
					try {
						Config.writeConfigFile();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				} else {
					Config.deleteConfigFile();
				}
			}));
			
			if(!initialized)
				gui.init();
			gui.setVisible(true);
		} else {
			// headless
			CommandLineArguments clArgs;
			try {
				clArgs = new CommandLineArguments(args, "-h", "--help", "--threads", "-t", "--use-gpu", "-gpu", "--gpu-device", "-device", "--workgroup-size", "--list-gpus");
			} catch(IllegalArgumentException e) {
				System.err.println(e.getMessage());
				help();
				return;
			}
			
			// variables to be set by the command line arguments
			int threads = -1;
			boolean useGpu;
			int gpuDevice = -1;
			int workgroupSize;
			
			// some ugly lines of code processing all command line arguments
			// but whatever
			if(clArgs.switchPresent("--list-gpus")) {		// list all available GPU's
				String[] devices = new GpuSolver().getAvailableDevices();
				if(devices.length == 0) {
					throw new IllegalArgumentException("No available GPU's were found");
				}
				System.out.println("available GPU's:");
				for(int i = 0; i < devices.length; i++) {
					if(i == gpuDevice)
						System.out.println("[X]\tDevice " + i + ": " + devices[i]);
					else
						System.out.println("\tDevice " + i + ": " + devices[i]);
				}
				return;
			}
			try {
				if(clArgs.switchPresent("--threads")) {
					threads = clArgs.switchIntValue("--threads");
				} else if(clArgs.switchPresent("-t")) {
					threads = clArgs.switchIntValue("-t");
				}
			} catch (NumberFormatException e) {
				System.err.println("thread count must be a number");
				return;
			}
			useGpu = clArgs.switchPresent("--use-gpu") || clArgs.switchPresent("-gpu");
			try {
				if(clArgs.switchPresent("--gpu-device")) {
					gpuDevice = clArgs.switchIntValue("--gpu-device");
				} else if(clArgs.switchPresent("-device")) {
					gpuDevice = clArgs.switchIntValue("-device");
				}
			} catch (NumberFormatException e) {
				System.err.println("GPU device index must be a number");
				return;
			}
			try {
				workgroupSize = (int) Config.getDefaultValue("gpuWorkgroupSize");
				if(clArgs.switchPresent("--workgroup-size")) {
					workgroupSize = clArgs.switchIntValue("--workgroup-size");
				}
			} catch (NumberFormatException e) {
				System.err.println("workgroup size must be a number");
				return;
			}
			String[] targets = clArgs.targets();
			if(targets.length != 1) {
				help();
				return;
			}
			int N;
			try {
				N = Integer.parseInt(targets[0]);
			} catch(NumberFormatException e) {
				help();
				return;
			}
			
			// we got all arguments
			// now start the solver
			try {
				startCommandLineSolver(N, threads, useGpu, gpuDevice, workgroupSize);
			} catch(IllegalArgumentException e) {
				System.err.println(e.getMessage());
				return;
			}
		}
	}
	
	private static void help() {
		System.out.println("Usage: java -jar NQueensFAF.jar <board size> [options]");
		System.out.println("  available options:");
		System.out.println("\t-h, --help \t\tprint this message");
		System.out.println("\t-t <thread_count>, --threads <thread_count> \n\t\t\t\tset the thread count when using CPU");
		System.out.println("\t-gpu, --use-gpu \tuse a GPU if possible");
		System.out.println("\t-device <index>, --gpu-device <index>	\n\t\t\t\tset the index of the GPU that should be used, see '--list-gpus'");
		System.out.println("\t--workgroup-size <workgroup_size> \n\t\t\t\tset the OpenCL workgroup size when using GPU");
		System.out.println("\t--list-gpus \t\tprint a list of all available GPU devices and their indexes");
	}
	
	private static void startCommandLineSolver(int N, int threads, boolean useGpu, int gpuDevice, int workgroupSize) {
		// its not possible to enter negative values because the command line parser grabs all arguments beginning with '-'
		if(N == 0 || N >= 32) {
			throw new IllegalArgumentException("board size must be a number >0 and <32");
		}
		if(useGpu) {
			if(gpuDevice == -1) {
				System.out.println("GPU device index was not specified.. using default device");
				gpuDevice = 0;
			}
			if(workgroupSize == 0) {
				throw new IllegalArgumentException("workgroup size must be a number >0");
			}
		} else {
			if(threads == -1) {
				System.out.println("CPU thread count was not specified.. using single threading");
				threads = 1;
			} else if(threads == 0 || threads > Runtime.getRuntime().availableProcessors()) {
				throw new IllegalArgumentException("thread count must be a number >0 and <" + Runtime.getRuntime().availableProcessors() + " (=available processors)");
			}
		}
		// initialize solver
		Solver solver;
		if(useGpu) {
			GpuSolver gs = new GpuSolver();
			String[] devices = gs.getAvailableDevices();
			if(devices.length == 0) {
				throw new IllegalArgumentException("No available GPU's were found");
			}
			System.out.println("available GPU's:");
			for(int i = 0; i < devices.length; i++) {
				if(i == gpuDevice)
					System.out.println("[X]\tDevice " + i + ": " + devices[i]);
				else
					System.out.println("\tDevice " + i + ": " + devices[i]);
			}
			if(gpuDevice >= devices.length) {
				throw new IllegalArgumentException("invalid GPU device index: " + gpuDevice + " (only " + devices.length + " devices available)");
			}
			gs.setDevice(gpuDevice);
			gs.setWorkgroupSize(workgroupSize);
			solver = gs;
		} else {
			CpuSolver cs = new CpuSolver();
			cs.setThreadcount(threads);
			solver = cs;
		}
		solver.setN(N);
		solver.setProgressUpdateDelay(200);
		solver.addInitializationCallback(() -> {
			if(useGpu)
				System.out.println("starting GPU solver for board size " + N + "..");
			else
				System.out.println("starting CPU solver for board size " + N + "..");
		});
		solver.addTerminationCallback(() -> {
			System.out.println("\nfound " + solver.getSolutions() + " solutions in " + solver.getDuration() + "ms");
		});
		String[] loadingChars = new String[] {
				"|", "/", "-", "\\"
		};
		final int[] loadingCounter = new int[] {0};
		solver.setOnProgressUpdateCallback((progress, solutions) -> {
			if(progress < 1f) {
				if(loadingCounter[0] >= loadingChars.length)
					loadingCounter[0] = 0;
				System.out.print("\r" + loadingChars[loadingCounter[0]++] + "\tprogress: " + progress + ", solutions: " + solutions);
			}
		});
		solver.solve();
	}
}
