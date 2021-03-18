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
	private long solvecounter = 0;
	private final int BLOCK_SIZE = 64;
	private long start, end;
	private boolean ready = false;

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

		// generate startconstellations and retreive the parameters
		ConstellationsGenerator presolver = new ConstellationsGenerator();
		presolver.genConstellations(N);

		// set global work size
		int global_work_size = presolver.getld_list().size();

		int[] ld_arr = new int[global_work_size];
		int[] rd_arr = new int[global_work_size];
		int[] col_arr = new int[global_work_size];
		int[] LD_arr = new int[global_work_size];
		int[] RD_arr = new int[global_work_size];
		int[] kl_arr = new int[global_work_size];
		int[] start_arr = new int[global_work_size];
		int[] sym_arr = new int[global_work_size];
		for(int i = 0; i < global_work_size; i++) {
			ld_arr[i] = presolver.getld_list().removeFirst();
			rd_arr[i] = presolver.getrd_list().removeFirst();
			col_arr[i] = presolver.getcol_list().removeFirst();
			LD_arr[i] = presolver.getLD_list().removeFirst();
			RD_arr[i] = presolver.getRD_list().removeFirst();
			kl_arr[i] = presolver.getkl_list().removeFirst();
			start_arr[i] = presolver.getstart_list().removeFirst();
			sym_arr[i] = presolver.getsym_list().removeFirst();
		}

		// Buffers for the kernel arguments
		// ld
		IntBuffer ld_buff = BufferUtils.createIntBuffer(global_work_size);
		ld_buff.put(ld_arr);	
		ld_buff.rewind();
		// rd
		IntBuffer rd_buff = BufferUtils.createIntBuffer(global_work_size);
		rd_buff.put(rd_arr);	
		rd_buff.rewind();
		// col
		IntBuffer col_buff = BufferUtils.createIntBuffer(global_work_size);
		col_buff.put(col_arr);	
		col_buff.rewind();
		// LD
		IntBuffer LD_buff = BufferUtils.createIntBuffer(global_work_size);
		LD_buff.put(LD_arr);	
		LD_buff.rewind();
		// RD
		IntBuffer RD_buff = BufferUtils.createIntBuffer(global_work_size);
		RD_buff.put(RD_arr);	
		RD_buff.rewind();
		// kl
		IntBuffer kl_buff = BufferUtils.createIntBuffer(global_work_size);
		kl_buff.put(kl_arr);	
		kl_buff.rewind();
		// start
		IntBuffer start_buff = BufferUtils.createIntBuffer(global_work_size);
		start_buff.put(start_arr);	
		start_buff.rewind();

		// OpenCL-Memory Objects for the kernel arguments
		// ld
		CLMem ld_mem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, global_work_size*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		CL10.clEnqueueWriteBuffer(queue, ld_mem, 0, 0, ld_buff, null, null);
		Util.checkCLError(errorBuff.get(0));
		// rd
		CLMem rd_mem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, global_work_size*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		CL10.clEnqueueWriteBuffer(queue, rd_mem, 0, 0, rd_buff, null, null);
		Util.checkCLError(errorBuff.get(0));
		// col
		CLMem col_mem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, global_work_size*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		CL10.clEnqueueWriteBuffer(queue, col_mem, 0, 0, col_buff, null, null);
		Util.checkCLError(errorBuff.get(0));
		// LD
		CLMem LD_mem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, global_work_size*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		CL10.clEnqueueWriteBuffer(queue, LD_mem, 0, 0, LD_buff, null, null);
		Util.checkCLError(errorBuff.get(0));
		// RD
		CLMem RD_mem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, global_work_size*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		CL10.clEnqueueWriteBuffer(queue, RD_mem, 0, 0, RD_buff, null, null);
		Util.checkCLError(errorBuff.get(0));
		// kl
		CLMem kl_mem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, global_work_size*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		CL10.clEnqueueWriteBuffer(queue, kl_mem, 0, 0, kl_buff, null, null);
		Util.checkCLError(errorBuff.get(0));
		// start
		CLMem start_mem = CL10.clCreateBuffer(context, CL10.CL_MEM_WRITE_ONLY | CL10.CL_MEM_ALLOC_HOST_PTR, global_work_size*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));
		CL10.clEnqueueWriteBuffer(queue, start_mem, 0, 0, start_buff, null, null);
		Util.checkCLError(errorBuff.get(0));

		// result memory
		CLMem resultMemory;
		resultMemory = CL10.clCreateBuffer(context, CL10.CL_MEM_READ_ONLY, global_work_size*4, errorBuff);
		Util.checkCLError(errorBuff.get(0));

		// Set the kernel parameters
		sqKernel.setArg(0, ld_mem);
		sqKernel.setArg(1, rd_mem);
		sqKernel.setArg(2, col_mem);
		sqKernel.setArg(3, LD_mem);
		sqKernel.setArg(4, RD_mem);
		sqKernel.setArg(5, kl_mem);
		sqKernel.setArg(6, start_mem);
		sqKernel.setArg(7, resultMemory);

		// create buffer of pointers defining the multi-dimensional size of the number of work units to execute
		final int dimensions = 1;
		PointerBuffer globalWorkSize = BufferUtils.createPointerBuffer(dimensions);
		globalWorkSize.put(0, global_work_size);
		PointerBuffer localWorkSize = BufferUtils.createPointerBuffer(dimensions);
		localWorkSize.put(0, BLOCK_SIZE);

		// wait for the queue to finish all preparations
		CL10.clFinish(queue);

		// run kernel and profile time
		final PointerBuffer event_Buff = BufferUtils.createPointerBuffer(1);		// buffer for event that is used for measuring the execution time
		CL10.clEnqueueNDRangeKernel(queue, sqKernel, dimensions, null, globalWorkSize, localWorkSize, null, event_Buff);	// Run the specified number of work units using our OpenCL program kernel
//		System.out.println("> Started " + global_work_size  + " threads");

		// set pseudo starttime
		ready = true;
		start = System.currentTimeMillis();
		
		CL10.clFinish(queue);			// wait till the task is complete

		// get time values using the clEvent, print time
		final CLEvent event = queue.getCLEvent(event_Buff.get(0));
		start = event.getProfilingInfoLong(CL10.CL_PROFILING_COMMAND_START);
		end = event.getProfilingInfoLong(CL10.CL_PROFILING_COMMAND_END);
//		String seconds_str = String.format(Locale.ROOT, "%.3f", (end-start)*0.000000001);
//		System.out.println("------------------------");
//		System.out.println("time: \t\t\t" + (end-start) + " nanoseconds (~" + seconds_str + " seconds)");

		// read from the result memory buffer
		IntBuffer resultBuff = BufferUtils.createIntBuffer(global_work_size);
		CL10.clEnqueueReadBuffer(queue, resultMemory, CL10.CL_TRUE, 0, resultBuff, null, null);
		// Print the values in the result buffer
		for(int i = 0; i < resultBuff.capacity(); i++) {
			solvecounter += resultBuff.get(i) * sym_arr[i];			// calculate the solutions-counter using the symmetry-factors of the sym_arr-array
		}
//		System.out.println("total solutions: \t" + (solvecounter));

		// Destroy our kernel and program
		CL10.clReleaseKernel(sqKernel);
		CL10.clReleaseProgram(sqProgram);
		CL10.clReleaseMemObject(ld_mem);
		CL10.clReleaseMemObject(rd_mem);
		CL10.clReleaseMemObject(col_mem);
		CL10.clReleaseMemObject(LD_mem);
		CL10.clReleaseMemObject(RD_mem);
		CL10.clReleaseMemObject(kl_mem);
		CL10.clReleaseMemObject(start_mem);
		CL10.clReleaseMemObject(resultMemory);

		// Destroy the OpenCL context
		destroyCL();

		ready = false;
	}
	
	private void reset() {
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

	// function to load the native file that is nessesary for the interaction with the lwjgl library
	public void loadLwjglNative() {
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
			temp_libdir = Files.createTempDirectory("NQueensFAF");

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
}