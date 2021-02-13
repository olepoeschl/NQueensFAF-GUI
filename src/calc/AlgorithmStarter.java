package calc;

import java.util.ArrayDeque;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import gui.Gui;
import util.FAFProcessData;

// sets 3 to 4 Queens on the NxN - board and calculates corresponding occupancy
// Queens are only placed on the first and last row and col
// this board with those Queens is called starting constellation and no starting constellation can be transformed into an other one by using rotation or mirroring

public class AlgorithmStarter {

	public static final int small_n_limit = 10;

	private int N, mask, solvecounter = 0;							// size of board						
	private int cpu;						// number of threads	
	private long old_solvecounter = 0;		// if we load an old calculation, get the old solvecounter											

	Set<Integer> startConstellations = new HashSet<Integer>();		// make sure there are no symmetric equivalent starting constellations in boardPropertiesList			
	ArrayList<AlgorithmThread> threadlist;							// list of starting constellations for each thread

	// for loading and saving and progress
	private boolean load = false;
	private int startConstCount = 0, calculatedStartConstCount = 0, startConstCountBad = 0;

	// for pausing and canceling
	private long start = 0, end = 0;
	private boolean ready = false, pause = false;


	public AlgorithmStarter(int N, int cpu) {
		this.N = N;
		this.cpu = cpu;

		startConstellations = new HashSet<Integer>();
	}

	public void startAlgorithm() {
		System.gc();	// please collect your garbage, Sir!

		// starting time
		start = System.currentTimeMillis();

		if(N <= small_n_limit) {
			mask = (1 << N) - 1;
			nq(0, 0, 0, 0, mask);
		}
		else {
			// if we don't load an old calculation
			if(!load) {		
				// column, left and right diag, idx of row, mask marks the board, halfN half of N rounded up
				final int halfN = (N + 1) / 2;

				// calculating start constellations with the first Queen on square (0,0)
				for(int j = 1; j < N-2; j++) {						// j is idx of Queen in last row				
					for(int l = j+1; l < N-1; l++) {				// l is idx of Queen in last col
						startConstellations.add(toijkl(0, j, 0, l));
					}
				}

				startConstCountBad = startConstellations.size();

				// calculate starting constellations for no Queens in corners
				// look above for if missing explanation
				for(int k = 1; k < halfN; k++) {						// gothrough first col
					for(int l = k+1; l < N-1; l++) {					// go through last col
						for(int i = k+1; i < N-1; i++) {				// go through first row
							if(i == N-1-l)								// skip if occupied
								continue;
							for(int j = N-k-2; j > 0; j--) {			// go through last row
								if(j==i || l == j)
									continue;

								if(!checkRotations(i, j, k, l)) {		// if no rotation-symmetric starting constellation already found
									startConstellations.add(toijkl(i, j, k, l));
								}
							}
						}
					}
				}
				// save number of found starting constellations
				startConstCount = startConstellations.size();

				// print in gui console
				Gui.print(startConstCount + " start-constellations were found, " + startConstCountBad + " of these suck", true);
			}

			// split starting constellations in cpu many lists (splitting the work for the threads)
			ArrayList< ArrayDeque<Integer> > threadConstellations = new ArrayList< ArrayDeque<Integer>>(cpu);
			for(int i = 0; i < cpu; i++) {
				threadConstellations.add(new ArrayDeque<Integer>());
			}
			Iterator<Integer> iterator = startConstellations.iterator();
			int i = 0;
			while(iterator.hasNext()) {
				threadConstellations.get((i++) % cpu).add(iterator.next());
			}

			// start the threads and wait until they are all finished
			ExecutorService executor = Executors.newFixedThreadPool(cpu);
			threadlist = new ArrayList<AlgorithmThread>();
			for(ArrayDeque<Integer> constellations : threadConstellations) {
				AlgorithmThread algThread = new AlgorithmThread(N, constellations);
				threadlist.add(algThread);
				executor.submit(algThread);
			}

			// threadlist built, everything ready
			ready = true;

			// wait for the executor
			executor.shutdown();
			try {
				if(executor.awaitTermination(2, TimeUnit.DAYS)) {
					// done 
				} else {
					// not done
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}
		}

		// end time
		end = System.currentTimeMillis();
	}

	// true, if starting constellation rotated by any angle has already been found
	private boolean checkRotations(int i, int j, int k, int l) {
		// rot90
		if(startConstellations.contains(((N-1-k)<<24) + ((N-1-l)<<16) + (j<<8) + i)) 
			return true;

		// rot180
		if(startConstellations.contains(((N-1-j)<<24) + ((N-1-i)<<16) + ((N-1-l)<<8) + N-1-k)) 
			return true;

		// rot270
		if(startConstellations.contains((l<<24) + (k<<16) + ((N-1-i)<<8) + N-1-j)) 
			return true;

		return false;
	}
	private int toijkl(int i, int j, int k, int l) {
		return (i<<24) + (j<<16) + (k<<8) + l;
	}

	// pause, cancel, continue
	public void pause() {
		pause = true;
		for(AlgorithmThread algThread : threadlist) {
			algThread.pause();
		}
	}
	public void go() {
		pause = false;
		for(AlgorithmThread algThread : threadlist) {
			algThread.go();
		}
	}
	public void cancel() {
		for(AlgorithmThread algThread : threadlist) {
			algThread.cancel();
		}
	}
	public void dontCancel() {
		for(AlgorithmThread algThread : threadlist) {
			algThread.dontCancel();
		}
	}

	public boolean isPaused() {
		return pause;
	}
	public boolean isReady() {
		return ready;
	}

	public boolean responds() {
		boolean responds = true;
		for(AlgorithmThread algThread : threadlist) {
			if( ! algThread.responds())
				responds = false;
		}
		return responds;
	}
	public void resetRespond() {
		for(AlgorithmThread algThread : threadlist) {
			algThread.resetRespond();
		}
	}

	// time measurement
	public long getStarttime() {
		return start;
	}
	public long getEndtime() {
		return end;
	}

	// progress measurement
	public int getStartConstCount() {
		return startConstCount;
	}
	public int getCalculatedStartConstCount() {
		int counter = 0;
		for(AlgorithmThread algThread : threadlist) {
			counter += algThread.getStartConstIndex();
		}
		return calculatedStartConstCount + counter;
	}
	public int getUncalculatedStartConstCount() {
		return getUncalculatedStartConstellations().size();
	}
	// loading 
	public ArrayDeque<Integer> getUncalculatedStartConstellations() {
		ArrayDeque<Integer> uncalcbplist = new ArrayDeque<Integer>();
		for(AlgorithmThread algThread : threadlist) {
			uncalcbplist.addAll(algThread.getUncalculatedStartConstellations());
		}
		return uncalcbplist;
	}
	public float getProgress() {
		if(threadlist == null)
			return 0;

		// calculate progress
		float progress = getCalculatedStartConstCount();
		return progress / getStartConstCount();
	}

	public int getN() {
		return N;
	}
	public long getSolvecounter() {
		if(N <= small_n_limit) {
			return this.solvecounter;
		}

		long solvecounter = 0;
		for(AlgorithmThread algThread : threadlist) {
			solvecounter += algThread.getSolvecounter();
		}
		return solvecounter + old_solvecounter;
	}

	//load progress of old calculation
	public void load(FAFProcessData fafprocessdata) {
		load = true;
		//		N = fafprocessdata.N;
		startConstellations.addAll(fafprocessdata);
		old_solvecounter = fafprocessdata.solvecounter;
		startConstCount = fafprocessdata.startConstCount;
		calculatedStartConstCount = fafprocessdata.calculatedStartConstCount;
	}


	// basic recursive backtracking solver for small N
	private void nq(int ld, int rd, int col, int row, int free) {
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
				nq((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1, nextfree);
		}
	}
}
