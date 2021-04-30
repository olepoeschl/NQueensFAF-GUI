package calc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL;
import org.lwjgl.opencl.CL10;
import org.lwjgl.opencl.CLCommandQueue;
import org.lwjgl.opencl.CLContext;
import org.lwjgl.opencl.CLDevice;
import org.lwjgl.opencl.CLEvent;
import org.lwjgl.opencl.CLKernel;
import org.lwjgl.opencl.CLMem;
import org.lwjgl.opencl.CLPlatform;
import org.lwjgl.opencl.CLProgram;
import org.lwjgl.opencl.Util;

import gui.Gui;
import util.FAFProcessData;

class GpuSolver extends Solver {

	// private HashSet<Integer> startConstellations;		// for saving and restoring (loading)
	private long currSolvecounter;
	private int solvedStartConstCount;

	// OpenCL objects
	private CLContext context;
	private CLPlatform platform;
	private List<CLDevice> devices;
	private CLDevice device;
	private CLCommandQueue xqueue, memqueue, signalqueue;
	private CLMem signalMem;

	// OpenCL variables
	private final int BLOCK_SIZE = 64;
	private int computeUnits;

	// variables for computing the remaining constellations on the cpu
	private long cpucounter;
	private int mask, L, sym, kl, k, l, kbit, lbit, kmask, lmask, LD, RD;
	private int cpuSolvedStartConstCount;

	// other variables
	private boolean gpuRunning, restored, cancelable;

	// constructor from superclass
	GpuSolver() {
		super();
		
		gpuRunning = false;
		restored = false;
		cancelable = false;
	}
	// initialize lwjgl libraries
	void init() {
		// load lwjgl-native
		loadLwjglNative();
		devices = new ArrayList<CLDevice>();
		
		// Create OpenCL
		try {
			CL.create();
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
	}

	// methods inherited from superclass (not including getters and setters)
	@Override
	void compute() {
		// abort computing if the parameters are invalid
		if(getN() <= Solvers.NBorder || getN() >= 32) {
			System.err.println("Error: \tInvalid value for board size (N): \t" + getN());
			return;
		}

		// reset all values and start computation
		if(!restored)
			reset();
		setRunning(true);
		setStarttime(System.currentTimeMillis());
		if(!restored)
			genConstellations();

		// Error buffer used to check for OpenCL error that occurred while a command was running
		IntBuffer errorBuff = BufferUtils.createIntBuffer(1);

		// Create an OpenCL context
		try {
			context = CLContext.create(platform, platform.getDevices(CL10.CL_DEVICE_TYPE_ALL), errorBuff);
		} catch (LWJGLException e) {
			e.printStackTrace();
		}

		// Create queues
		xqueue = CL10.clCreateCommandQueue(context, device, CL10.CL_QUEUE_PROFILING_ENABLE, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		memqueue = CL10.clCreateCommandQueue(context, device, CL10.CL_QUEUE_PROFILING_ENABLE, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		signalqueue = CL10.clCreateCommandQueue(context, device, CL10.CL_QUEUE_PROFILING_ENABLE, errorBuff);
		Util.checkCLError(errorBuff.get(0));

		// Create program and store it on the specified device
		CLProgram sqProgram;
		sqProgram = CL10.clCreateProgramWithSource(context, loadText("res/kernels.c"), null);

		// build program and define N and preRows as a macro for the kernel
		String options = "-D N="+getN() + " -D BLOCK_SIZE="+BLOCK_SIZE + " -cl-mad-enable";
		int error = CL10.clBuildProgram(sqProgram, device, options, null);
		Util.checkCLError(error);
		// Create kernel
		CLKernel sqKernel;
		if(cancelable)
			sqKernel = CL10.clCreateKernel(sqProgram, "runCancelable", null);
		else
			sqKernel = CL10.clCreateKernel(sqProgram, "run", null);

		// preparation for cpu-solver
		mask = (1 << getN()) - 1;
		L = (1 << (getN()-1));
		kmask = mask - L;	// hält nur ganz links frei für die dame
		lmask = mask - 1;	// ganz rechts das gleiche

		// set global work size
		computeUnits = device.getInfoInt(CL10.CL_DEVICE_MAX_COMPUTE_UNITS);
		int globalWorkSize = getStartConstCount() - (getStartConstCount() % (BLOCK_SIZE * computeUnits));
		Gui.print(String.format("%d constellations found", getStartConstCount()), true);
		if(globalWorkSize != getStartConstCount()) {
			Gui.print(String.format("> remaining %d constellations have to be solved using CPU", getStartConstCount()-globalWorkSize), true);
		}
		int[] ldArr = new int[globalWorkSize];
		int[] rdArr = new int[globalWorkSize];
		int[] colArr = new int[globalWorkSize];
		int[] LDArr = new int[globalWorkSize];
		int[] RDArr = new int[globalWorkSize];
		int[] klArr = new int[globalWorkSize];
		int[] startArr = new int[globalWorkSize];
		int[] symArr = new int[globalWorkSize];
		for(int i = 0; i < globalWorkSize; i++) {
			ldArr[i] = getConstellationsGenerator().getld_list().removeFirst();
			rdArr[i] = getConstellationsGenerator().getrd_list().removeFirst();
			colArr[i] = getConstellationsGenerator().getcol_list().removeFirst();
			LDArr[i] = getConstellationsGenerator().getLD_list().removeFirst();
			RDArr[i] = getConstellationsGenerator().getRD_list().removeFirst();
			klArr[i] = getConstellationsGenerator().getkl_list().removeFirst();
			startArr[i] = getConstellationsGenerator().getstart_list().removeFirst();
			symArr[i] = getConstellationsGenerator().getsym_list().removeFirst();
		}

		// OpenCL-Memory Objects for the kernel arguments
		// ld
		CLMem ldMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		ByteBuffer paramPtr = CL10.clEnqueueMapBuffer(memqueue, ldMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, ldArr[i]);
		}
		CL10.clEnqueueUnmapMemObject(memqueue, ldMem, paramPtr, null, null);
		// rd
		CLMem rdMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		paramPtr = CL10.clEnqueueMapBuffer(memqueue, rdMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, rdArr[i]);
		}
		CL10.clEnqueueUnmapMemObject(memqueue, rdMem, paramPtr, null, null);
		// col
		CLMem colMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		paramPtr = CL10.clEnqueueMapBuffer(memqueue, colMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, colArr[i]);
		}
		CL10.clEnqueueUnmapMemObject(memqueue, colMem, paramPtr, null, null);
		// LD
		CLMem LDMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		paramPtr = CL10.clEnqueueMapBuffer(memqueue, LDMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, LDArr[i]);
		}
		CL10.clEnqueueUnmapMemObject(memqueue, LDMem, paramPtr, null, null);
		// RD
		CLMem RDMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		paramPtr = CL10.clEnqueueMapBuffer(memqueue, RDMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, RDArr[i]);
		}
		CL10.clEnqueueUnmapMemObject(memqueue, RDMem, paramPtr, null, null);
		// kl
		CLMem klMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		paramPtr = CL10.clEnqueueMapBuffer(memqueue, klMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, klArr[i]);
		}
		CL10.clEnqueueUnmapMemObject(memqueue, klMem, paramPtr, null, null);
		// start
		CLMem startMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		paramPtr = CL10.clEnqueueMapBuffer(memqueue, startMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, startArr[i]);
		}
		CL10.clEnqueueUnmapMemObject(memqueue, startMem, paramPtr, null, null);

		// result memory
		CLMem resMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		ByteBuffer resWritePtr = CL10.clEnqueueMapBuffer(memqueue, resMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null,null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			resWritePtr.putInt(i*4, 0);
		}
		CL10.clEnqueueUnmapMemObject(memqueue, resMem, resWritePtr, null, null);
		IntBuffer resBuff = BufferUtils.createIntBuffer(globalWorkSize);

		// progress indicator memory
		CLMem progressMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		ByteBuffer progressWritePtr = CL10.clEnqueueMapBuffer(memqueue, progressMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null,null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			progressWritePtr.putInt(i*4, 0);
		}
		CL10.clEnqueueUnmapMemObject(memqueue, progressMem, progressWritePtr, null, null);
		IntBuffer progressBuff = BufferUtils.createIntBuffer(globalWorkSize);

		// Set the kernel parameters
		sqKernel.setArg(0, ldMem);
		sqKernel.setArg(1, rdMem);
		sqKernel.setArg(2, colMem);
		sqKernel.setArg(3, LDMem);
		sqKernel.setArg(4, RDMem);
		sqKernel.setArg(5, klMem);
		sqKernel.setArg(6, startMem);
		sqKernel.setArg(7, resMem);
		sqKernel.setArg(8, progressMem);

		if(cancelable) {
			// signal memory
			signalMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, 4, errorBuff);
			Util.checkCLError(errorBuff.get(0));
			paramPtr = CL10.clEnqueueMapBuffer(memqueue, signalMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, 4, null, null, errorBuff);
			Util.checkCLError(errorBuff.get(0));
			paramPtr.putInt(0);
			CL10.clEnqueueUnmapMemObject(memqueue, signalMem, paramPtr, null, null);
			sqKernel.setArg(9, signalMem);
		}

		// create buffer of pointers defining the multi-dimensional size of the number of work units to execute
		final int dimensions = 1;
		PointerBuffer globalWorkers = BufferUtils.createPointerBuffer(dimensions);
		globalWorkers.put(0, globalWorkSize);
		PointerBuffer localWorkSize = BufferUtils.createPointerBuffer(dimensions);
		localWorkSize.put(0, BLOCK_SIZE);

		// run kernel and profile time
		final PointerBuffer eventBuff = BufferUtils.createPointerBuffer(1);		// buffer for event that is used for measuring the execution time
		CL10.clEnqueueNDRangeKernel(xqueue, sqKernel, dimensions, null, globalWorkers, localWorkSize, null, eventBuff);	// Run the specified number of work units using our OpenCL program kernel
		CL10.clFlush(xqueue);
		//		System.out.println("> Started " + globalWorkSize  + " threads");

		// set pseudo starttime
		setReady(true);
		setStarttime(System.currentTimeMillis());

		// continously update progress
		gpuRunning = true;
		new Thread() {
			public void run() {
				long tempcounter;
				int tempCalcConstCount;
				while(gpuRunning) {
					// calculate current sovlecounter
					tempcounter = cpucounter;
					tempCalcConstCount = cpuSolvedStartConstCount;
					CL10.clEnqueueReadBuffer(memqueue, resMem, CL10.CL_TRUE, 0, resBuff, null, null);
					CL10.clEnqueueReadBuffer(memqueue, progressMem, CL10.CL_TRUE, 0, progressBuff, null, null);
					for(int i = 0; i < globalWorkSize; i++) {
						tempcounter += resBuff.get(i) * symArr[i];
						tempCalcConstCount += progressBuff.get(i);
					}
					solvedStartConstCount = tempCalcConstCount;
					currSolvecounter = tempcounter;
					// short delay
					try {
						sleep(128);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				gpuRunning = true;
			}
		}.start();
		
		// solve the rest using CPU
		int a = getConstellationsGenerator().getld_list().size(), ld, rd, col, start_idx;
		for(int i = 0; i < a; i++) {
			sym = getConstellationsGenerator().getsym_list().removeFirst();
			kl = getConstellationsGenerator().getkl_list().removeFirst();
			LD = getConstellationsGenerator().getLD_list().removeFirst();
			RD = getConstellationsGenerator().getRD_list().removeFirst();
			k = kl >>> 8;
			l = kl & 255;
			kbit = (1 << (getN()-k-1));
			lbit = (1 << l);	
			ld = getConstellationsGenerator().getld_list().removeFirst();
			rd = getConstellationsGenerator().getrd_list().removeFirst();
			col = getConstellationsGenerator().getcol_list().removeFirst();
			start_idx = getConstellationsGenerator().getstart_list().removeFirst();

			solver(ld, rd, col, start_idx);
	
			cpuSolvedStartConstCount++;
			currSolvecounter = cpucounter;
		}
		Gui.print("CPU has finished", true);

		CL10.clFinish(xqueue);			// wait till the task is complete
		//		CL10.clWaitForEvents(eventBuff);
		gpuRunning = false;

		// get time values using the clEvent, print time
		final CLEvent event = xqueue.getCLEvent(eventBuff.get(0));
		setStarttime(event.getProfilingInfoLong(CL10.CL_PROFILING_COMMAND_START) / 1000000);
		setEndtime(event.getProfilingInfoLong(CL10.CL_PROFILING_COMMAND_END) / 1000000);

		// wait for the update-thread (above) to end
		while(!gpuRunning) {
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		gpuRunning = false;
		
		// read from the result and progress memory buffers
		CL10.clEnqueueReadBuffer(memqueue, resMem, CL10.CL_TRUE, 0, resBuff, null, null);
		CL10.clEnqueueReadBuffer(memqueue, progressMem, CL10.CL_TRUE, 0, progressBuff, null, null);
		currSolvecounter = cpucounter;
		solvedStartConstCount = cpuSolvedStartConstCount;
		for(int i = 0; i < globalWorkSize; i++) {
			currSolvecounter += resBuff.get(i) *  symArr[i];
			solvedStartConstCount += progressBuff.get(i);
		}

		// Destroy our kernel and program
		CL10.clReleaseKernel(sqKernel);
		CL10.clReleaseProgram(sqProgram);
		CL10.clReleaseMemObject(ldMem);
		CL10.clReleaseMemObject(rdMem);
		CL10.clReleaseMemObject(colMem);
		CL10.clReleaseMemObject(LDMem);
		CL10.clReleaseMemObject(RDMem);
		CL10.clReleaseMemObject(klMem);
		CL10.clReleaseMemObject(startMem);
		CL10.clReleaseMemObject(resMem);
		CL10.clReleaseMemObject(progressMem);

		// Destroy the OpenCL context
		destroyCL();

		// computation done
		setReady(false);
		setRunning(false);
		restored = false;
	}

	@Override
	void genConstellations() {
		getConstellationsGenerator().genConstellationsGpu(getN());
		getConstellationsGenerator().sortConstellations();
		setStartConstCount(getConstellationsGenerator().getld_list().size());
	}

	@Override
	void reset() {
		//		startConstellations = null;
		currSolvecounter = 0;
		cpucounter = 0;
		solvedStartConstCount = 0;
		cpuSolvedStartConstCount = 0;
		setStartConstCount(0);
		setStarttime(0);
		setEndtime(0);
		setFSolvecounter(0);
		setFSolvedStartConstCount(0);
		System.gc();
	}

	@Override
	void save() {

	}

	@Override
	void load(FAFProcessData d) {

	}

	@Override
	void resetLoad() {
		restored = false;
	}
	
	@Override
	void cancel() {
		if(gpuRunning) {
			IntBuffer errorBuff = BufferUtils.createIntBuffer(1);
			ByteBuffer ptr = CL10.clEnqueueMapBuffer(signalqueue, signalMem, CL10.CL_FALSE, CL10.CL_MAP_WRITE, 0, 4, null, null, errorBuff);
			Util.checkCLError(errorBuff.get(0));
			ptr.putInt(1);
			CL10.clEnqueueUnmapMemObject(memqueue, signalMem, ptr, null, null);
		}
	}

	// own methods
	private void solver(int ld, int rd, int col, int row) {
		if(row == getN()-1) {
			cpucounter += sym;
			return;
		}
		int klguard = ((kbit << row) ^ L) | ((lbit >>> row) ^ 1) | ((((kbit << row)&L) >>> (getN()-1)) * kmask) | (((lbit >>> row)&1) * lmask);	
		int free = ~(ld | rd | col | (LD >>> (getN()-1-row)) | (RD << (getN()-1-row)) | klguard) & mask;
		int bit = 0;

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			solver((ld|bit) << 1, (rd|bit) >>> 1, col|bit, row+1);
		}
	}
	
	// OpenCl stuff
	public ArrayDeque<String> listDevices() throws LWJGLException {
		devices.clear();

		// a list that contains vendors and names of all available devices
		ArrayDeque<String> device_infos = new ArrayDeque<String>();
		String device_type = null;
		try {
			for(CLPlatform platform : CLPlatform.getPlatforms()) {
				for(CLDevice device : platform.getDevices(CL10.CL_DEVICE_TYPE_GPU)) {
					devices.add(device);

					switch(device.getInfoInt(CL10.CL_DEVICE_TYPE)) {
					case 1:
						device_type = "Default";
						break;
					case 2:
						device_type = "CPU";
						break;
					case 4:
						device_type = "GPU";
						break;
					case 8:
						device_type = "ACCELERATOR";
						break;
					}
					device_infos.add("[" + device_type + "]   " + device.getInfoString(CL10.CL_DEVICE_NAME));
				}
			}
		} catch(NullPointerException e) {
			// no device found
		}
		return device_infos;
	}
	private void destroyCL() {
		// Finish destroying anything we created
		CL10.clReleaseCommandQueue(xqueue);
		CL10.clReleaseContext(context);
		// And release OpenCL, after this method call we cannot use OpenCL unless we re-initialize it
		CL.destroy();
	}
	private String loadText(String filepath) {
		BufferedReader br = null;
		String resultString = null;
		try {
			// Get the file containing the OpenCL kernel source code
			InputStream clSourceFile = GpuSolver.class.getClassLoader().getResourceAsStream(filepath);
			// Create a buffered file reader for the source file
			br = new BufferedReader(new InputStreamReader(clSourceFile));
			// Read the file's source code line by line and store it in a string builder
			String line = null;
			StringBuilder result = new StringBuilder();
			while((line = br.readLine()) != null) {
				result.append(line);
				result.append("\n");
			}
			// Convert the string builder into a string containing the source code to return
			resultString = result.toString();
		} catch(NullPointerException npe) {
			npe.printStackTrace();
		} catch(IOException ioe) {
			ioe.printStackTrace();
		} finally {
			// Finally clean up any open resources
			try {
				br.close();
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}

		// Return the string read from the OpenCL kernel source code file
		return resultString;
	}

	// function to load the native file that is nessesary for the interaction with the lwjgl library
	void loadLwjglNative() {
		Path temp_libdir = null;
		String filenameIn = null;
		String filenameOut = null;

		// determine system architecture
		String arch = System.getProperty("os.arch");
		if(arch.contains("64"))
			arch = "64";
		else
			arch = "";
		// determine operating system
		String os = System.getProperty("os.name").toLowerCase();
		if(os.contains("win")) {
			// windows
			filenameIn = "lwjgl" + arch + ".dll";
			filenameOut = filenameIn;
		} else if(os.contains("mac")) {
			// mac
			filenameIn = "liblwjgl_mac.dylib";
			filenameOut = "liblwjgl.dylib";
		} else if(os.contains("nix") || os.contains("nux") || os.contains("aix")) {
			// unix (linux etc)
			filenameIn = "liblwjgl" + arch + "_linux.so";
			filenameOut = "liblwjgl" + arch + ".so";
		} else if(os.contains("sunos")) {
			// solaris
			filenameIn = "liblwjgl" + arch + "_solaris.so";
			filenameOut = "liblwjgl" + arch + ".so";
		} else {
			// unknown os
			System.err.println("No native executables available for this operating system (" + os + ").");
		}

		try {
			// create temporary directory to stor the native files inside
			temp_libdir = Files.createTempDirectory("NQueensFaf");

			// copy the native file from within the jar to the temporary directory
			InputStream in = GpuSolver.class.getClassLoader().getResourceAsStream("natives/" + filenameIn);
			byte[] buffer = new byte[1024];
			int read = -1;
			File file = new File(temp_libdir + "/" + filenameOut);
			FileOutputStream fos = new FileOutputStream(file);
			while((read = in.read(buffer)) != -1) {
				fos.write(buffer, 0, read);
			}
			fos.close();
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		System.setProperty("org.lwjgl.librarypath", temp_libdir.toAbsolutePath().toString());
	}

	// getters
	@Override
	long getSolvecounter() {
		return currSolvecounter;
	}

	@Override
	int getSolvedStartConstCount() {
		return solvedStartConstCount;
	}

	@Override
	ArrayDeque<Integer> getUnsolvedStartConstellations(){
		return null;
	}

	// setters
	void setDevice(int index) {
		device = devices.get(index);
		platform = device.getPlatform();
	}
	
	void setCancelable(boolean cancelable) {
		this.cancelable = cancelable;
	}
}
