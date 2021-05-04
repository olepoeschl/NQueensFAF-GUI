package calc;

import java.util.ArrayDeque;

import org.lwjgl.LWJGLException;

public class Solvers {

	// constants
	public static final int NBorder = 10;
	public static final int USE_CPU = 0;
	public static final int USE_GPU = 1;

	// Solvers
	private Solver[] solvers;
	private CpuSolver cpuSolver;
	private GpuSolver gpuSolver;

	// variables
	private int N;
	private int mode;
	private boolean canceled;

	public Solvers() {
		solvers = new Solver[2];
		cpuSolver = new CpuSolver();
		gpuSolver = new GpuSolver();
		solvers[USE_CPU] = cpuSolver;
		solvers[USE_GPU] = gpuSolver;
		
		canceled = false;
	}

	public void solve() {
		canceled = false;
		if(N > NBorder) {
			solvers[mode].setN(N);
			solvers[mode].compute();
		} else {
			cpuSolver.setN(N);
			cpuSolver.cheapSolver();
			solvers[mode].setStarttime(cpuSolver.getStarttime());
			solvers[mode].setEndtime(cpuSolver.getEndtime());
		}
	}
	
	public void resetRestoration() {
		for(Solver solver : solvers) {
			solver.resetRestoration();
		}
	}
	
	public void save(String path, long fTime) {
		solvers[mode].setFTime(fTime);
		ProgressBackup pb = new ProgressBackup(solvers[mode]);
		pb.save(path);
	}

	public void restore(String path) {
		ProgressBackup pb = ProgressBackup.restore(path);
		for(Solver solver : solvers) {
			solver.reset();
			solver.restore(pb);
		}
		setN(pb.N);
	}
	
	// specific methods for mode USE_CPU
	public void pause() {
		cpuSolver.pause();
	}
	public void go() {
		cpuSolver.go();
	}

	public void cancel() {
		solvers[mode].cancel();
		canceled = true;
	}
	public void dontCancel() {
		cpuSolver.dontCancel();
		canceled = false;
	}

	public boolean responds() {
		return cpuSolver.responds();
	}
	public void resetRespond() {
		cpuSolver.resetRespond();
	}

	// specific methods for mode USE_GPU
	public ArrayDeque<String> listDevices() throws LWJGLException {
		return gpuSolver.listDevices();
	}
	
	// getters
	public int getN() {
		return N;
	}

	public int getMode() {
		return mode;
	}

	public Solver getActiveSolver() {
		return solvers[mode];
	}
	
	public boolean isReady() {
		return solvers[mode].isReady();
	}

	public boolean isRunning() {
		return solvers[mode].isRunning();
	}
	
	public boolean isCanceled() {
		return canceled;
	}

	public long getStarttime() {
		return solvers[mode].getStarttime();
	}

	public long getEndtime() {
		return solvers[mode].getEndtime();
	}

	public long getFTime() {
		return solvers[mode].getFTime();
	}
	
	public long getSolvecounter() {
		if(N > NBorder)
			return solvers[mode].getSolvecounter();
		else
			return cpuSolver.getSolvecounter();
	}

	public int getStartConstCount() {
		return solvers[mode].getStartConstCount();
	}

	public int getSolvedStartConstCount() {
		return solvers[mode].getSolvedStartConstCount();
	}

	public int getUnsolvedStartConstCount() {
		return solvers[mode].getUnsolvedStartConstCount();
	}

	public ArrayDeque<Integer> getUnsolvedStartConstellations(){
		return solvers[mode].getUnsolvedStartConstellations();
	}

	public float getProgress() {
		if(N > NBorder)
			return solvers[mode].getProgress();
		else
			return 100f;
	}

	public boolean isInitialized() {
		return solvers[mode].isReady();
	}

	// setters
	public void setN(int N) {
		this.N = N;
	}

	public void setMode(int mode) {
		this.mode = mode;
	}

	public void setThreadcount(int threadcount) {
		cpuSolver.setThreadcount(threadcount);
	}

	public boolean isPaused() {
		return cpuSolver.isPaused();
	}

	public void setDevice(int idx) {
		gpuSolver.setDevice(idx);
	}
}
