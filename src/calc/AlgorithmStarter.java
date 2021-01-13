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

	private int N;							// size of board						
	private int cpu;						// number of threads	
	private long old_solvecounter = 0;		// if we load an old calculation, get the old solvecounter
	private int symmetry = 8;				// look at boardProperties							
	int[] currentRows;						// look boardIntegers in boardProperties						
	private ArrayDeque<BoardProperties> boardPropertiesList;	// save starting constellations in this Array

	Set<Integer> startConstellations = new HashSet<Integer>();		// make sure there are no symmetric equivalent starting constellations in boardPropertiesList			
	ArrayList<AlgorithmThread> threadlist;							// list of starting constellations for each thread
	
	// for loading and saving and progress
	private boolean load = false;
	private long startConstCount = 0, calculatedStartConstCount = 0;

	// for pausing and canceling
	private long start = 0, end = 0;
	private boolean ready = false, pause = false;


	public AlgorithmStarter(int N, int cpu) {
		this.N = N;
		this.cpu = cpu;

		startConstellations = new HashSet<Integer>();

		boardPropertiesList = new ArrayDeque<BoardProperties>();
	}

	public void startAlgorithm() {
		System.gc();	// please collect your garbage, Sir!
		
		// starting time
		start = System.currentTimeMillis();
		
		// if we don't load an old calculation
		if(!load) {		
			// column, left and right diag, idx of row, mask marks the board, halfN half of N rounded up
			int col, ld, rd, row, mask = (1 << N) - 1, halfN = (N + (N % 2)) / 2;
			
			// calculating start constellations with the first Queen on square (0,0)
			for(int j = 1; j < N-2; j++) {						// j is idx of Queen in last row				
				for(int l = j+1; l < N-1; l++) {				// l is idx of Queen in last col
					
					currentRows = new int[N-2];					
					row = 1;
					ld = 0;
					rd = (1 << (N-1)) | (1 << l);
					col = (1 << (N-1)) | 1 | (1 << (N-1-j));

					// calculate the occupancy resulting from the starting constellation (1 is free)
					while(row<N-1) {
						ld = (ld<<1) & mask;
						rd >>= 1;
						if(row == l)
							ld |= 1;
						if(row == j)
							ld |= 1;
						if(row == N-1-j)
							rd |= (1<<(N-1));
						currentRows[row-1] = ~(ld | rd | col) & mask;
						row++;
					}

					currentRows[l-1] = 1;	// if Queen already placed in this row, only her square is free

					// add starting constellation to list
					boardPropertiesList.add(new BoardProperties(currentRows, 8, 0, l));	
					startConstellations.add((1<<24) + (j<<16) + (1<<8) + l);
				}
			}
			
			
			
			// calculate starting constellations for no Queens in corners
			// look above for if missing explanation
			for(int i = 1; i < halfN; i++) {						// gothrough first row	
				for(int j = i+1; j < N-1; j++) {					// go through last row
					for(int k = i+1; k < N-1; k++) {				// go through first col
						if(k == N-1-j)								// skip if occupied
							continue;
						for(int l = N-i-2; l > 0; l--) {			// go through last col
							if(l==k || l == j)
								continue;
							
							if(!checkRotations(i, j, k, l)) {		// if no rotation-symmetric starting constellation already found
								
								if(i == N-1-j && k == N-1-l)		// starting constellation symmetric by rot180?
									if(symmetry90(i, j, k, l))		// even by rot90?
										symmetry = 2;
									else
										symmetry = 4;
								else
									symmetry = 8;					// none of the above?

								currentRows = new int[N-2];			
								row = 1;
								ld = (1 << (N-1-i)) | (1 << (N-1-k));
								rd = (1 << (N-1-i)) | (1 << l);
								col = (1 << (N-1)) | (1) | (1 << (N-1-i)) | (1 << (N-1-j));
								
								while(row<N-1) {
									ld = (ld<<1) & mask;
									rd >>= 1;
									if(row == k)
										rd |= (1 << (N-1));
									if(row == l)
										ld |= 1;
									if(row == j)
										ld |= 1;
									if(row == N-1-j)
										rd |= (1<<(N-1));
									currentRows[row-1] = ~(ld | rd | col) & mask;
									row++;
								}
								
								currentRows[k-1] = 1 << (N-1);				
								currentRows[l-1] = 1;

								boardPropertiesList.add(new BoardProperties(currentRows, symmetry, k, l));	
								startConstellations.add((i<<24) + (j<<16) + (k<<8) + l);						
							}
						}
					}
				}
			}

			// save number of found starting constellations
			startConstCount = boardPropertiesList.size();
			// print in gui console
			Gui.print(startConstCount + " Start-Konstellationen gefunden", true);
		}
		
		// split starting constellations in cpu many lists (splitting the work for the threads)
		ArrayList< ArrayDeque<BoardProperties> > threadConstellations = new ArrayList< ArrayDeque<BoardProperties>>(cpu);
		for(int i = 0; i < cpu; i++) {
			threadConstellations.add(new ArrayDeque<BoardProperties>());
		}
		Iterator<BoardProperties> iterator = boardPropertiesList.iterator();
		int i = 0;
		while(iterator.hasNext()) {
			threadConstellations.get((i++) % cpu).add(iterator.next());
		}

	// start the threads and wait until they are all finished
		ExecutorService executor = Executors.newFixedThreadPool(cpu);
		threadlist = new ArrayList<AlgorithmThread>();
		for(ArrayDeque<BoardProperties> constellations : threadConstellations) {
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
//				System.out.println("fertig geworden");
			} else {
//				System.out.println("Zeitlimit abgelaufen");
				//Speichern
			}
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		// endtime
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

	// true, if starting constellation is symmetric for rot90
	private boolean symmetry90(int i, int j, int k, int l) {
		if(((i << 24) + (j << 16) + (k << 8) + (l))   ==   (((N-1-k)<<24) + ((N-1-l)<<16) + (j<<8) + i))
			return true;
		return false;
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
	public boolean isPaused() {
		return pause;
	}
	public boolean isReady() {
		return ready;
	}

	// time measurement
	public long getStarttime() {
		return start;
	}
	public long getEndtime() {
		return end;
	}
	
	// progress measurement
	public long getStartConstCount() {
		return startConstCount;
	}
	public long getCalculatedStartConstCount() {
		long counter = 0;
		for(AlgorithmThread algThread : threadlist) {
			counter += algThread.getStartConstIndex();
		}
		return calculatedStartConstCount + counter;
	}
	
	// loading 
	public ArrayDeque<BoardProperties> getUncalculatedStartConstellations() {
		ArrayDeque<BoardProperties> uncalcbplist = new ArrayDeque<BoardProperties>();
		for(AlgorithmThread algThread : threadlist) {
			uncalcbplist.addAll(algThread.getUncalculatedStartConstellations());
		}
		return uncalcbplist;
	}
	public long getUncalculatedStartConstCount() {
		return getUncalculatedStartConstellations().size();
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
		boardPropertiesList.addAll(fafprocessdata);
		old_solvecounter = fafprocessdata.solvecounter;
		startConstCount = fafprocessdata.startConstCount;
		calculatedStartConstCount = fafprocessdata.calculatedStartConstCount;
	}
}
