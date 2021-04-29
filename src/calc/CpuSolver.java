package calc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import util.FAFProcessData;

class CpuSolver extends Solver {

	private HashSet<Integer> startConstellations;
	private int threadcount;
	private ArrayList<CpuSolverThread> threadlist;
	private boolean pause, restored = false;

	// constructor from superclass
	CpuSolver() {
		super();
	}

	// methods inherited from superclass (not including getters and setters)
	@Override
	void compute() {
		// abort computing if the parameters are invalid
		if(getN() <= 0 || getN() >= 32) {
			System.err.println("Error: \tInvalid value for board size (N): \t" + getN());
			return;
		}
		if(threadcount <= 0 || threadcount > Runtime.getRuntime().availableProcessors()) {
			System.err.println("Error: \tInvalid value for number of threads (threadcount): \t" + threadcount);
			return;
		}
		
		// reset all values and start computation
		if(!restored)
			reset();
		setRunning(true);
		setStarttime(System.currentTimeMillis());
		if(!restored)
			genConstellations();

		// split starting constellations in [cpu] many lists (splitting the work for the threads)
		ArrayList<ArrayDeque<Integer>> threadConstellations = new ArrayList<ArrayDeque<Integer>>(threadcount);
		for(int i = 0; i < threadcount; i++) {
			threadConstellations.add(new ArrayDeque<Integer>());
		}
		int i = 0;
		for(int constellation : startConstellations) {
			threadConstellations.get((i++) % threadcount).addLast(constellation);
		}

		// start the threads and wait until they are all finished
		ExecutorService executor = Executors.newFixedThreadPool(threadcount);
		threadlist = new ArrayList<CpuSolverThread>();
		for(i = 0; i < threadcount; i++) {
			CpuSolverThread cpuSolverThread = new CpuSolverThread(getN(), threadConstellations.get(i));
			threadlist.add(cpuSolverThread);
			executor.submit(cpuSolverThread);
		}

		// threadlist built, everything ready
		setReady(true);

		// wait for the executor
		executor.shutdown();
		try {
			if(executor.awaitTermination(365, TimeUnit.DAYS)) {
				// done 
			} else {
				// not done
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		// computation done
		setEndtime(System.currentTimeMillis());
		setReady(false);
		setRunning(false);
		restored = false;
	}

	@Override
	void genConstellations() {
		startConstellations = getConstellationsGenerator().genConstellationsCpu(getN());
		setStartConstCount(startConstellations.size());
	}
	
	@Override
	void reset() {
		startConstellations = null;
		threadlist = null;
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
	void load(FAFProcessData filedata) {
		setN(filedata.N);
		int len = filedata.size();
		startConstellations = new HashSet<Integer>();
		for(int i = 0; i < len; i++) {
			startConstellations.add(filedata.removeFirst());
		}
		setFSolvecounter(filedata.solvecounter);
		setStartConstCount(filedata.startConstCount);
		setFSolvedStartConstCount(filedata.calculatedStartConstCount);

		restored = true;
		setEndtime(0);
	}

	// util methods
	void cheapSolver() {
		int mask = (1 << getN()) - 1;
		nq(0, 0, 0, 0, mask, mask);
	}
	private void nq(int ld, int rd, int col, int row, int free, int mask) {
		if(row == getN()-1) {
			incFSolvecounter();
			return;
		}

		int bit;
		int nextfree;

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~((ld|bit)<<1 | (rd|bit)>>1 | col|bit) & mask;

			if(nextfree > 0)
				nq((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree, mask);
		}
	}
	void pause() {
		pause = true;
		for(CpuSolverThread t : threadlist) {
			t.pause();
		}
	}
	void go() {
		pause = false;
		for(CpuSolverThread t : threadlist) {
			t.go();
		}
	}
	
	void cancel() {
		for(CpuSolverThread t : threadlist) {
			t.cancel();
		}
	}
	void dontCancel() {
		for(CpuSolverThread t : threadlist) {
			t.dontCancel();
		}
	}

	boolean responds() {
		for(CpuSolverThread t : threadlist) {
			if(!t.responds())
				return false;
		}
		return true;
	}
	void resetRespond() {
		for(CpuSolverThread t : threadlist) {
			t.resetRespond();
		}
	}
	
	// getters
	@Override
	long getSolvecounter() {
		long solvecounter = getFSolvecounter();
		for(CpuSolverThread t : threadlist) {
			solvecounter += t.getSolvecounter();
		}
		return solvecounter;
	}
	
	@Override
	int getSolvedStartConstCount() {
		int counter = getFSolvedStartConstCount();
		if(threadlist != null) {
			for(CpuSolverThread t : threadlist) {
				counter += t.getStartConstIndex();
			}
		}
		return counter;
	}
	
	ArrayDeque<Integer> getUnsolvedStartConstellations() {
		ArrayDeque<Integer> unsolvedConstList = new ArrayDeque<Integer>();
		for(CpuSolverThread t : threadlist) {
			for(int constellation : t.getUncalculatedStartConstellations()) {
				unsolvedConstList.add(constellation);
			}
		}
		return unsolvedConstList;
	}
	
	boolean isPaused() {
		return pause;
	}
	
	// setters
	void setThreadcount(int threadcount) {
		this.threadcount = threadcount;
	}
}
