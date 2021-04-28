package calc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;

public class GpuSolver {

	// calculation variables
	private int N;
	private int compute_units, minmem, maxmem, k, l, LD, RD, mask, sym, kl;
	private int L, kbit, lbit, kmask, lmask;
	private long solvecounter, cpucounter;
	private int startConstCount, calculatedStartConstCount, cpuConstCount;
	private final int BLOCK_SIZE = 64;
	private long start, end;
	private boolean ready = false, running = false;

	// OpenCL variables
	private CLContext context;
	private CLPlatform platform;
	private List<CLDevice> devices;
	private CLDevice device;
	private CLCommandQueue queue;

	public GpuSolver() throws URISyntaxException {
		// load lwjgl-native
		loadLwjglNative();

		devices = new ArrayList<CLDevice>();
	}

	public void start() {
		reset();

		// Error buffer used to check for OpenCL error that occurred while a command was running
		IntBuffer errorBuff = BufferUtils.createIntBuffer(1);

		// Create an OpenCL context
		try {
			context = CLContext.create(platform, platform.getDevices(CL10.CL_DEVICE_TYPE_ALL), errorBuff);
		} catch (LWJGLException e) {
			e.printStackTrace();
		}

		// Create a command queue
		queue = CL10.clCreateCommandQueue(context, device, CL10.CL_QUEUE_PROFILING_ENABLE, errorBuff);
		Util.checkCLError(errorBuff.get(0)); 

		// Create program and store it on the specified device
		CLProgram sqProgram;
		sqProgram = CL10.clCreateProgramWithSource(context, loadText("res/setqueen_kernel.c"), null);

		// build program and define N and preRows as a macro for the kernel
		String options = "-D N="+N + " -D BLOCK_SIZE="+BLOCK_SIZE + " -cl-mad-enable";
		int error = CL10.clBuildProgram(sqProgram, device, options, null);
		Util.checkCLError(error);
		// Create kernel
		CLKernel sqKernel = CL10.clCreateKernel(sqProgram, "run", null);

		// preparation for cpu-solver
		mask = (1 << N) - 1;
		L = (1 << (N-1));
		kmask = mask - L;	// hält nur ganz links frei für die dame
		lmask = mask - 1;	// ganz rechts das gleiche
		
		// generate startconstellations and retreive the parameters
		ConstellationsGenerator presolver = new ConstellationsGenerator();
		presolver.genConstellations(N);

		// set global work size
		compute_units = device.getInfoInt(CL10.CL_DEVICE_MAX_COMPUTE_UNITS);
		startConstCount = presolver.getld_list().size();
		int globalWorkSize = presolver.getld_list().size() - (presolver.getld_list().size() % (BLOCK_SIZE * compute_units));

		int[] ldArr = new int[globalWorkSize];
		int[] rdArr = new int[globalWorkSize];
		int[] colArr = new int[globalWorkSize];
		int[] LDArr = new int[globalWorkSize];
		int[] RDArr = new int[globalWorkSize];
		int[] klArr = new int[globalWorkSize];
		int[] startArr = new int[globalWorkSize];
		int[] symArr = new int[globalWorkSize];
		for(int i = 0; i < globalWorkSize; i++) {
			ldArr[i] = presolver.getld_list().removeFirst();
			rdArr[i] = presolver.getrd_list().removeFirst();
			colArr[i] = presolver.getcol_list().removeFirst();
			LDArr[i] = presolver.getLD_list().removeFirst();
			RDArr[i] = presolver.getRD_list().removeFirst();
			klArr[i] = presolver.getkl_list().removeFirst();
			startArr[i] = presolver.getstart_list().removeFirst();
			symArr[i] = presolver.getsym_list().removeFirst();
		}

		// OpenCL-Memory Objects for the kernel arguments
		// ld
		CLMem ldMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		ByteBuffer paramPtr = CL10.clEnqueueMapBuffer(queue, ldMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, ldArr[i]);
		}
		CL10.clEnqueueUnmapMemObject(queue, ldMem, paramPtr, null, null);
		// rd
		CLMem rdMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		paramPtr = CL10.clEnqueueMapBuffer(queue, rdMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, rdArr[i]);
		}
		CL10.clEnqueueUnmapMemObject(queue, rdMem, paramPtr, null, null);
		// col
		CLMem colMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		paramPtr = CL10.clEnqueueMapBuffer(queue, colMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, colArr[i]);
		}
		CL10.clEnqueueUnmapMemObject(queue, colMem, paramPtr, null, null);
		// LD
		CLMem LDMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		paramPtr = CL10.clEnqueueMapBuffer(queue, LDMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, LDArr[i]);
		}
		CL10.clEnqueueUnmapMemObject(queue, LDMem, paramPtr, null, null);
		// RD
		CLMem RDMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		paramPtr = CL10.clEnqueueMapBuffer(queue, RDMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, RDArr[i]);
		}
		CL10.clEnqueueUnmapMemObject(queue, RDMem, paramPtr, null, null);
		// kl
		CLMem klMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		paramPtr = CL10.clEnqueueMapBuffer(queue, klMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, klArr[i]);
		}
		CL10.clEnqueueUnmapMemObject(queue, klMem, paramPtr, null, null);
		// start
		CLMem startMem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		paramPtr = CL10.clEnqueueMapBuffer(queue, startMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null, null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			paramPtr.putInt(i*4, startArr[i]);
		}
		CL10.clEnqueueUnmapMemObject(queue, startMem, paramPtr, null, null);

		// result memory
		CLMem resMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		ByteBuffer resPtr = CL10.clEnqueueMapBuffer(queue, resMem, CL10.CL_TRUE, CL10.CL_MAP_READ, 0, globalWorkSize*4, null,null, errorBuff);
		Util.checkCLError(errorBuff.get(0));

		// progress memory
		CLMem progressMem = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, globalWorkSize*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		ByteBuffer progressWritePtr = CL10.clEnqueueMapBuffer(queue, progressMem, CL10.CL_TRUE, CL10.CL_MAP_WRITE, 0, globalWorkSize*4, null,null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		for(int i = 0; i < globalWorkSize; i++) {
			progressWritePtr.putInt(i*4, 0);
		}
		CL10.clEnqueueUnmapMemObject(queue, progressMem, progressWritePtr, null, null);
		// map progress memory
		ByteBuffer progressPtr = CL10.clEnqueueMapBuffer(queue, progressMem, CL10.CL_TRUE, CL10.CL_MAP_READ, 0, globalWorkSize*4, null,null, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		
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

		// create buffer of pointers defining the multi-dimensional size of the number of work units to execute
		final int dimensions = 1;
		PointerBuffer globalWorkers = BufferUtils.createPointerBuffer(dimensions);
		globalWorkers.put(0, globalWorkSize);
		PointerBuffer localWorkSize = BufferUtils.createPointerBuffer(dimensions);
		localWorkSize.put(0, BLOCK_SIZE);

		// wait for the queue to finish all preparations
		CL10.clFlush(queue);

		// run kernel and profile time
		final PointerBuffer eventBuff = BufferUtils.createPointerBuffer(1);		// buffer for event that is used for measuring the execution time
		CL10.clEnqueueNDRangeKernel(queue, sqKernel, dimensions, null, globalWorkers, localWorkSize, null, eventBuff);	// Run the specified number of work units using our OpenCL program kernel
		CL10.clFlush(queue);
		running = true;
//		System.out.println("> Started " + globalWorkSize  + " threads");

		// set pseudo starttime
		ready = true;
		start = System.currentTimeMillis();

		// solve the rest using CPU
		int a = presolver.getld_list().size(), ld, rd, col, start_idx;
		for(int i = 0; i < a; i++) {
			sym = presolver.getsym_list().removeFirst();
			kl = presolver.getkl_list().removeFirst();
			LD = presolver.getLD_list().removeFirst();
			RD = presolver.getRD_list().removeFirst();
			k = kl >>> 8;
			l = kl & 255;
			kbit = (1 << (N-k-1));
			lbit = (1 << l);	
			ld = presolver.getld_list().removeFirst();
			rd = presolver.getrd_list().removeFirst();
			col = presolver.getcol_list().removeFirst();
			start_idx = presolver.getstart_list().removeFirst();

			solver(ld, rd, col, start_idx);
			
			calculatedStartConstCount++;
			solvecounter = cpucounter;
		}
		cpuConstCount = calculatedStartConstCount;
		
		// check process
		new Thread() {
			public void run() {
				long tempcounter;
				int tempCalcConstCount;
				while(running) {
					// calculate current sovlecounter
					tempcounter = cpucounter;
					tempCalcConstCount = cpuConstCount;
					for(int i = 0; i < globalWorkSize-2; i++) {
						tempcounter += resPtr.getInt(i*4) * symArr[i];
						tempCalcConstCount += progressPtr.getInt(i*4);
					}
					calculatedStartConstCount = tempCalcConstCount;
					solvecounter = tempcounter;
					// short delay
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}.start();
		
		CL10.clFinish(queue);			// wait till the task is complete
//		CL10.clWaitForEvents(eventBuff);
		running = false;
		
		// get time values using the clEvent, print time
		final CLEvent event = queue.getCLEvent(eventBuff.get(0));
		start = event.getProfilingInfoLong(CL10.CL_PROFILING_COMMAND_START);
		end = event.getProfilingInfoLong(CL10.CL_PROFILING_COMMAND_END);

		// read from the result memory buffer
		solvecounter = cpucounter;
		for(int i = 0; i < globalWorkSize; i++) {
			solvecounter += resPtr.getInt(i*4) *  symArr[i];
		}
		calculatedStartConstCount = startConstCount;

		// unmap pointers for results and progress
		CL10.clEnqueueUnmapMemObject(queue, resMem, resPtr, null, null);
		CL10.clEnqueueUnmapMemObject(queue, progressMem, progressPtr, null, null);
		
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

		ready = false;
	}
	
	private void reset() {
		startConstCount = 0;
		calculatedStartConstCount = 0;
		cpucounter = 0;
		solvecounter = 0;
		start = 0;
		end = 0;
	}

	// OpenCl stuff
	public ArrayDeque<String> listDevices() throws LWJGLException { 
		// Create OpenCL
		CL.create();

		devices.clear();

		// a list that contains vendors and names of all available devices
		ArrayDeque<String> device_infos = new ArrayDeque<String>();
		String device_type = null;
		try {
			for(CLPlatform platform : CLPlatform.getPlatforms()) {
				for(CLDevice device : platform.getDevices(CL10.CL_DEVICE_TYPE_ALL)) {
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
		CL10.clReleaseCommandQueue(queue);
		CL10.clReleaseContext(context);
		// And release OpenCL, after this method call we cannot use OpenCL unless we re-initialize it
		CL.destroy();
	}
	private String loadText(String name) {
		BufferedReader br = null;
		String resultString = null;
		try {
			// Get the file containing the OpenCL kernel source code
			InputStream clSourceFile = GpuSolver.class.getClassLoader().getResourceAsStream(name);
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

	private void solver(int ld, int rd, int col, int row) {
		if(row == N-1) {
			cpucounter += sym;
			return;
		}
		int klguard = ((kbit << row) ^ L) | ((lbit >>> row) ^ 1) | ((((kbit << row)&L) >>> (N-1)) * kmask) | (((lbit >>> row)&1) * lmask);	
		int free = ~(ld | rd | col | (LD >>> (N-1-row)) | (RD << (N-1-row)) | klguard) & mask;
		int bit = 0;
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			solver((ld|bit) << 1, (rd|bit) >>> 1, col|bit, row+1);
		}
	}
	
	// function to load the native file that is nessesary for the interaction with the lwjgl library
	public void loadLwjglNative() {
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

	// getters and setters
	public boolean isReady() {
		return ready;
	}

	public void setN(int N) {
		this.N = N;
	}
	public int getN() {
		return N;
	}
	public void setDevice(int index) {
		device = devices.get(index);
		platform = device.getPlatform();
	}

	public long getStarttime() {
		return start;
	}
	public long getEndtime() {
		return end;
	}
	public long getSolvecounter() {
		return solvecounter;
	}
	public float getProgress() {
		return ((float) calculatedStartConstCount) / startConstCount;
	}
	public int getStartConstCount() {
		return startConstCount;
	}
	public int getCalculatedStartConstCount() {
		return calculatedStartConstCount;
	}
}