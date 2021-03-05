package calc;

import java.util.ArrayDeque;
import java.util.List;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;

import org.lwjgl.BufferUtils;
import org.lwjgl.LWJGLException;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.*;

public class GpuSolver {

	// calculation variables
	private int N, mask, solvecounter = 0;
	private final int PRE_ROWS = 5, BLOCK_SIZE = 64;
	private int len = 0, compute_units;
	private long start, end;
	private boolean ready = false;
	private ArrayDeque<Integer> ld_list, rd_list, col_list;

	// OpenCL variables
	private CLContext context;
	private CLPlatform platform;
	private List<CLDevice> devices;
	private CLDevice device;
	private CLCommandQueue queue;

	public GpuSolver(int N) throws URISyntaxException {
		this.N = N;
		mask = (1 << N) - 1;

		ld_list = new ArrayDeque<Integer>();
		rd_list = new ArrayDeque<Integer>();
		col_list = new ArrayDeque<Integer>();
		
		// load lwjgl-native
		loadLwjglNatives();
	}

	public void start() {
		// calculate the start-constellations
		calcStartConstellations();

		// Create our OpenCL context to run commands
		try {
			initializeCL();
		} catch (LWJGLException e) {
			e.printStackTrace();
		}
		
		// Error buffer used to check for OpenCL error that occurred while a command was running
		IntBuffer errorBuff = BufferUtils.createIntBuffer(1);

		// Create program and store it on the specified device
		CLProgram sqProgram;
		sqProgram = CL10.clCreateProgramWithSource(context, loadText("res/setqueen_kernel.c"), null);
		
		// build program and define N and preRows as a macro for the kernel
		String options = "-D N="+N + " -D PRE_ROWS="+PRE_ROWS + " -D BLOCK_SIZE="+BLOCK_SIZE + " -cl-mad-enable";
		int error = CL10.clBuildProgram(sqProgram, device, options, null);
		Util.checkCLError(error);
		// Create kernel
		CLKernel sqKernel = CL10.clCreateKernel(sqProgram, "run", null);
		
		// Buffers for ld, rd and col
		int global_work_size = len - (len % (BLOCK_SIZE * compute_units));
		int[] params = new int[global_work_size * 3];
		for(int i = 0; i < global_work_size*3; i+=3) {
			params[i] = ld_list.removeFirst();
			params[i+1] = rd_list.removeFirst();
			params[i+2] = col_list.removeFirst();
		}
		
		IntBuffer params_Buff = BufferUtils.createIntBuffer(global_work_size * 3);
		params_Buff.put(params);	
		params_Buff.rewind();
		// Create an OpenCL memory object containing a copy of the stack buffer
		CLMem params_mem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, params_Buff.capacity()*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		CL10.clEnqueueWriteBuffer(queue, params_mem, 0, 0, params_Buff, null, null);
		Util.checkCLError(errorBuff.get(0));
		
		// result memory
		CLMem resultMemory;
		resultMemory = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY, global_work_size * 4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		
		// Set the kernel parameters
		sqKernel.setArg(0, params_mem);
		sqKernel.setArg(1, resultMemory);
		
		// Create a buffer of pointers defining the multi-dimensional size of the number of work units to execute
		final int dimensions = 1;
		PointerBuffer globalWorkSize = BufferUtils.createPointerBuffer(dimensions);
		globalWorkSize.put(0, global_work_size);
		PointerBuffer localWorkSize = BufferUtils.createPointerBuffer(dimensions);
		localWorkSize.put(0, BLOCK_SIZE);

		// wait for the queue to finish all preparations
		CL10.clFinish(queue);
		
		// buffer for event that is used for measuring the execution time
		final PointerBuffer event_Buff = BufferUtils.createPointerBuffer(1);
		
		// set pseudo starttime
		ready = true;
		start = System.currentTimeMillis();
		
		// Run the specified number of work units using our OpenCL program kernel
		CL10.clEnqueueNDRangeKernel(queue, sqKernel, dimensions, null, globalWorkSize, localWorkSize, null, event_Buff);
//		System.out.println("> Started " + global_work_size  + " threads");
		
		for(int i = 0; i < len % (BLOCK_SIZE * compute_units); i++) {
			int ld = ld_list.removeFirst();
			int rd = rd_list.removeFirst();
			int col = col_list.removeFirst();
			nq2(ld, rd, col, PRE_ROWS, ~(ld | rd | col) & mask);
		}
		
		// wait till the task is complete
		CL10.clFinish(queue);
		
		// get time values using the clEvent, print the time
		final CLEvent event = queue.getCLEvent(event_Buff.get(0));
		start = event.getProfilingInfoLong(CL10.CL_PROFILING_COMMAND_START);
		end = event.getProfilingInfoLong(CL10.CL_PROFILING_COMMAND_END);
//		String seconds_str = String.format(Locale.ROOT, "%.3f", (end-start)*0.000000001);
//		System.out.println("------------------------");
//		System.out.println("time: \t\t\t" + (end-start) + " nanoseconds (~" + seconds_str + " seconds)");
		
		//This reads the result memory buffer
		IntBuffer resultBuff;
		resultBuff = BufferUtils.createIntBuffer(global_work_size);

		// We read the buffer in blocking mode so that when the method returns we know that the result buffer is full
		CL10.clEnqueueReadBuffer(queue, resultMemory, CL10.CL_TRUE, 0, resultBuff, null, null);
		// Print the values in the result buffer
//		System.out.println("cpu-solutions: \t\t" + solvecounter);
		for(int i = 0; i < resultBuff.capacity(); i++) {
			solvecounter += resultBuff.get(i);
		}
		solvecounter *= 2;
//		System.out.println("total solutions: \t" + solvecounter);

		// Destroy our kernel and program
		CL10.clReleaseKernel(sqKernel);
		CL10.clReleaseProgram(sqProgram);
		CL10.clReleaseMemObject(params_mem);
		CL10.clReleaseMemObject(resultMemory);

		// Destroy the OpenCL context
		destroyCL();
	}

	// calculation stuff
	private void calcStartConstellations() {
		for(int i = 0; i < N/2; i++) {
			int q = 1 << i;
			int ld = q << 1;
			int rd = q >> 1;
			nq(ld, rd, q, 1, ~(ld|rd|q) & mask);
		}
		if(N%2 > 0) {
			int q1 = 1 << (N/2);
			int ld1 = q1 << 2;
			int rd1 = q1 >> 2;
			for(int j = 0; j < N/2-1; j++) {
				int q2 = 1 << j;
				int ld2 = q2 << 1;
				int rd2 = q2 >> 1;
				nq(ld1|ld2, rd1|rd2, q1|q2, 2, ~(ld1|ld2|rd1|rd2|q1|q2) & mask);
			}
		}
		len = ld_list.size();
	}
	private void nq(int ld, int rd, int col, int row, int free) {
		if(row == PRE_ROWS) {
			ld_list.add(ld);
			rd_list.add(rd);
			col_list.add(col);
			return;
		}
		
		int bit;
		int nextfree;
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~((ld|bit)<<1 | (rd|bit)>>1 | col|bit) & mask;
			
			if(nextfree > 0)
				nq((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}
	private void nq2(int ld, int rd, int col, int row, int free) {
		if(row == N-1) {
			solvecounter++;
			return;
		}
		
		int bit;
		int nextfree;
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~((ld|bit)<<1 | (rd|bit)>>1 | col|bit) & mask;
			
			if(nextfree > 0)
				nq2((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}

	// OpenCl stuff
	private void initializeCL() throws LWJGLException { 
		IntBuffer errorBuf = BufferUtils.createIntBuffer(1);

		// Create OpenCL
		CL.create();

		// Get the first available platform
		platform = CLPlatform.getPlatforms().get(0);

		// Run our program on the GPU
		devices = platform.getDevices(CL10.CL_DEVICE_TYPE_GPU);
		device = devices.get(0);
		compute_units = device.getInfoInt(CL10.CL_DEVICE_MAX_COMPUTE_UNITS);
		
		// Create an OpenCL context, this is where we could create an OpenCL-OpenGL compatible context
		context = CLContext.create(platform, devices, errorBuf);

		// Create a command queue
		queue = CL10.clCreateCommandQueue(context, device, CL10.CL_QUEUE_PROFILING_ENABLE, errorBuf);
		
		// Check for any errors
		Util.checkCLError(errorBuf.get(0)); 
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
			// If there is an error finding the file
			System.err.println("Error retrieving OpenCL source file: ");
			npe.printStackTrace();
		} catch(IOException ioe) {
			// If there is an IO error while reading the file
			System.err.println("Error reading OpenCL source file: ");
			ioe.printStackTrace();
		} finally {
			// Finally clean up any open resources
			try {
				br.close();
			} catch (IOException ex) {
				// If there is an error closing the file after we are done reading from it
				System.err.println("Error closing OpenCL source file");
				ex.printStackTrace();
			}
		}

		// Return the string read from the OpenCL kernel source code file
		return resultString;
	}

	// function to load the native file that is nessesary for the interaction with the lwjgl library
	public void loadLwjglNatives() {
	    Path temp_libdir = null;
	    String filename = null;
	    
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
	    	filename = "lwjgl" + arch + ".dll";
	    } else if(os.contains("mac")) {
	    	// mac
	    	filename = "liblwjgl_mac.dylib";
	    } else if(os.contains("nix") || os.contains("nux") || os.contains("aix")) {
	    	// unix (linux etc)
	    	filename = "liblwjgl" + arch + "_linux.so";
	    } else if(os.contains("sunos")) {
	    	// solaris
	    	filename = "liblwjgl" + arch + "_solaris.so";
	    } else {
	    	// unknown os
	    	System.err.println("No native executables available for this operating system (" + os + ").");
	    }
	    
	    try {
	    	// create temporary directory to stor the native files inside
	    	temp_libdir = Files.createTempDirectory("Main");

	    	// copy the native file from within the jar to the temporary directory
	    	InputStream in = GpuSolver.class.getClassLoader().getResourceAsStream("natives/" + filename);
	    	byte[] buffer = new byte[1024];
	    	int read = -1;
	    	File file = new File(temp_libdir + "/" + filename);
	    	FileOutputStream fos = new FileOutputStream(file);
	    	while((read = in.read(buffer)) != -1) {
	    		fos.write(buffer, 0, read);
	    	}
	    	fos.close();
	    	in.close();

	    	// delete the temporary file and the folder when the jvm exits
			file.deleteOnExit();
			temp_libdir.toFile().deleteOnExit();
		} catch (IOException e) {
			e.printStackTrace();
		}
	    
	    System.setProperty("org.lwjgl.librarypath", temp_libdir.toAbsolutePath().toString());
	}

	// getters and setters
	public boolean isReady() {
		return ready;
	}
	public int getN() {
		return N;
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
}