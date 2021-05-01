package calc;

import java.util.ArrayDeque;

abstract class Solver {

	private int N, startConstCount;
	private long start, end;
	private boolean ready, running;
	private long fSolvecounter, fTime;
	private int fSolvedStartconstCount;
	private ConstellationsGenerator constGenerator;

	// constructors
	Solver() {
		N = 16;
		constGenerator = new ConstellationsGenerator();
	}
	Solver(int N) {
		this.N = N;
		constGenerator = new ConstellationsGenerator();
	}

	// abstract methods
	abstract void compute();
	abstract void genConstellations();
	abstract void reset();
	abstract void resetRestoration();

//	abstract void save();
	abstract void restore(ProgressBackup pb);
	
	abstract void cancel();

	abstract long getSolvecounter();
	abstract int getSolvedStartConstCount();
	abstract ArrayDeque<Integer> getUnsolvedStartConstellations();

	// methods
	void incFSolvecounter() {
		fSolvecounter++;
	}

	// getters
	int getN() {
		return N;
	}

	long getStarttime() {
		return start - fTime;
	}

	long getEndtime() {
		return end;
	}

	int getStartConstCount() {
		return startConstCount;
	}

	int getUnsolvedStartConstCount() {
		return startConstCount - getSolvedStartConstCount();
	}

	float getProgress() {
		return ((float) getSolvedStartConstCount()) / startConstCount *100;
	}

	boolean isReady() {
		return ready;
	}

	boolean isRunning() {
		return running;
	}

	long getFSolvecounter() {
		return fSolvecounter;
	}

	long getFTime() {
		return fTime;
	}

	int getFSolvedStartConstCount() {
		return fSolvedStartconstCount;
	}
	
	ConstellationsGenerator getConstellationsGenerator() {
		return constGenerator;
	}

	// setters
	void setN(int N) {
		this.N = N;
	}

	void setStarttime(long start) {
		this.start = start;
	}

	void setEndtime(long end) {
		this.end = end;
	}
	
	void setStartConstCount(int startConstCount) {
		this.startConstCount = startConstCount;
	}
	
	void setReady(boolean ready) {
		this.ready = ready;
	}

	void setRunning(boolean running) {
		this.running = running;
	}
  
	void setFSolvecounter(long fSolvecounter) {
		this.fSolvecounter = fSolvecounter;
	}
	
	void setFTime(long fTime) {
		this.fTime = fTime;
	}
	
	void setFSolvedStartConstCount(int rSolvedStartConstCount) {
		this.fSolvedStartconstCount = rSolvedStartConstCount;
	}
}
