package calc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import gui.Gui;

public class AlgorithmStarter {

	private int N, mask;										// Brettgr��e N, mask ist Integer mit N 1en rechts in der Bitdarstellung (entspricht dem Brett)
	private int cpu;											// Anzahl der gew�nschten Threads (Anzahl der Kerne)
	private long solvecounter = 0;
	private int symmetry = 8;									// Vielfachheit der gefundenen L�sung
	int[] currentRows;											// beschreibt f�r aktuelle Startpos die Belegung der N Zeilen (als Int in Bitdarstellung)
	private boolean[] rowNotFree, colNotFree, diaLeftNotFree, diaRightNotFree;	// Belegung der Diagonalen, Zeilen, Spalten (true belegt, false frei)
	private ArrayDeque<BoardProperties> boardPropertiesList;	// Bretteigenschaften (Belegung einzelner Zeilen) zu jeder Startposition

	ArrayDeque<int[]> startConstellations;						// checkt, ob aktuelle Startposition schon gefunden wurde ( beachte Symmetrie)
	ArrayList<AlgorithmThread> threadlist;
	
	//
	private long start = 0;
	private boolean pause = false;
	

	public AlgorithmStarter(int N, int cpu, boolean pausable) {
		this.N = N;
		this.cpu = cpu;
		mask = (int) (Math.pow(2, N) - 1);

		startConstellations = new ArrayDeque<int[]>();
	
		boardPropertiesList = new ArrayDeque<BoardProperties>();
		
		colNotFree = new boolean[N];
		rowNotFree = new boolean[N];
		diaLeftNotFree = new boolean[2*N-1];
		diaRightNotFree = new boolean[2*N-1];
	}

	public void startAlgorithm() {
		//Speichere Start-Zeit
		start = System.currentTimeMillis();
		
		int halfN = (N + (N % 2)) / 2;				// Dame nur links setzen, Rest eh symmetrisch

		//Start-Konstellationen berechnen f�r 1.Dame ist nicht in der oberen linken Ecke (hier muss man Symmetrie checken)
		for(int i = 1; i < halfN; i++) {			// erste Zeile durchgehen
			colNotFree[i] = true;					// Spalte wird belegt
			diaRightNotFree[-i+N-1] = true;			// dia right wird belegt
			diaLeftNotFree[i] = true;				// dia left wird belegt
			
			for(int j = i+1; j < N-1; j++) {		// letzte Zeile durchgehen
				if( ! SquareIsSafe(N-1, j))
					continue;
				colNotFree[j] = true;							// Spalte wird belegt
				diaRightNotFree[(N-1)-j+N-1] = true;			// dia right wird belegt
				diaLeftNotFree[(N-1)+j] = true;					// dia left wird belegt
				
				for(int k = i+1; k < N-1; k++) {				// erste Spalte durchgehen
					if( ! SquareIsSafe(k, 0))
						continue;
					rowNotFree[k] = true;								// Zeile wird belegt
					diaRightNotFree[k+N-1] = true;						// dia right wird belegt
					diaLeftNotFree[k] = true;							// dia left wird belegt

					for(int l = 1; l < N-1; l++) {						// letzte Spalte durchgehen
						if( SquareIsSafe(l, N-1) && !checkRotations(i, j, k, l) && !checkDiaLeft(i, j, k, l)) {		// wenn zul. und neu, dann neue Startpos. gefunden
							rowNotFree[l] = true;
							diaRightNotFree[l] = true;
							diaLeftNotFree[l + N-1] = true;

							if(i == N-1-j && k == N-1-l)		// 180� symmetrisch?
								if(symmetry90(i, j, k, l))		// sogar 90� symmetrisch?
									symmetry = 2;
								else
									symmetry = 4;
							else
								symmetry = 8;					// gar nicht symmetrisch
							
							colNotFree[0] = true;
							colNotFree[N-1] = true;
							
							currentRows = new int[N];					// 1, wenn belegt, 0 sonst
							for(int m = 1; m < N-1; m++) {				// wird an AlgorithmThgread �bergeben damit man wei�, welche Felder durch die Startpos. 
								for(int n = 0; n < N; n++) {			// schon belegt sind
									if(!SquareIsSafe(m, n)) 
										currentRows[m] += 1 << (N-1-n);
								}
							}
							
							colNotFree[0] = false;
							colNotFree[N-1] = false;
							
							currentRows[k] = mask >> 1;					// �berschreibe die Belegungen in Zeile und Spalte 1 und N
							currentRows[l] = (mask >> 1) << 1;
							currentRows[0] = mask - (1<<(N-1-i));
							currentRows[N-1] = mask - (1<<(N-1-j));
							
							boardPropertiesList.add(new BoardProperties(currentRows, symmetry));	// boeardIntegersList enth�t f�r jede startpos. zu jeder zeile einen integer der die belegung angibt
							
							startConstellations.add(new int[]{i, j, k, l} );						// Sachen wieder freigeben	
							rowNotFree[l] = false;
							diaRightNotFree[l] = false;
							diaLeftNotFree[l + N-1] = false;
						}
					}

					rowNotFree[k] = false;								// Zeile wird freigegeben
					diaRightNotFree[k+N-1] = false;						// dia right wird wieder frei gemacht
					diaLeftNotFree[k] = false;							// dia left wird wieder frei gemacht
				}

				colNotFree[j] = false;							// col wird frei gegeben
				diaRightNotFree[(N-1)-j+N-1] = false;			// dia right wird wieder frei gemacht
				diaLeftNotFree[(N-1)+j] = false;				// dia left wird wieder frei gemacht
			}

			colNotFree[i] = false;					// col wird freigegeben
			diaRightNotFree[-i+N-1] = false;		// dia right wird belegt
			diaLeftNotFree[i] = false;				// dia left wird belegt
		}

		//Start-Konstellationen berechnen f�r 1.Dame auf Feld (0, 0)
		diaRightNotFree[N-1] = true;
		diaLeftNotFree[0] = true;
		
		for(int j = 1; j < N-2; j++) {
			if( ! SquareIsSafe(N-1, j))
				continue;
			colNotFree[j] = true;
			diaRightNotFree[N-1 - j + N-1] = true;
			diaLeftNotFree[N-1 + j] = true;
			
			for(int l = j+1; l < N-1; l++) {
				if( SquareIsSafe(l, N-1)) {		
					rowNotFree[l] = true;
					diaRightNotFree[l] = true;
					diaLeftNotFree[l + N-1] = true;
					
					colNotFree[0] = true;
					colNotFree[N-1] = true;
					
					currentRows = new int[N];					// 1, wenn belegt, 0 sonst
					for(int m = 1; m < N-1; m++) {				// wird an AlgorithmThgread �bergeben damit man wei�, welche Felder durch die Startpos. 
						for(int n = 0; n < N; n++) {			// schon belegt sind
							if(!SquareIsSafe(m, n)) 
								currentRows[m] += 1 << (N-1-n);
						}
					}
					
					colNotFree[0] = false;
					colNotFree[N-1] = false;
					
					currentRows[0] = mask >> 1;
					currentRows[N-1] = ~(1 << (N-1-j)) & mask;
					currentRows[l] = (mask >> 1) << 1;
					
					boardPropertiesList.add(new BoardProperties(currentRows, 8));	
					startConstellations.add(new int[]{0, j, 0, l});
					
					rowNotFree[l] = false;
					diaRightNotFree[l] = false;
					diaLeftNotFree[l + N-1] = false;
				}
			}

			colNotFree[j] = false;
			diaRightNotFree[N-1 - j + N-1] = false;
			diaLeftNotFree[N-1 + j] = false;
		}
		
		
		//---
		ArrayList< ArrayDeque<BoardProperties> > threadConstellations = new ArrayList< ArrayDeque<BoardProperties>>(cpu);
		for(int i = 0; i < cpu; i++) {
			threadConstellations.add(new ArrayDeque<BoardProperties>());
		}

		//startConstellations in cpu viele Teile aufteilen
		System.out.println("L�nge von startConstellations = " + startConstellations.size());
		Iterator<BoardProperties> iterator = boardPropertiesList.iterator();
		int i = 0;
		while(iterator.hasNext()) {
			threadConstellations.get((i++) % cpu).add(iterator.next());
		}
		
		//Thread starten und auf ihre Beendung warten
		threadlist = new ArrayList<AlgorithmThread>();
		for(ArrayDeque<BoardProperties> constellations : threadConstellations) {
			AlgorithmThread algThread = new AlgorithmThread( N, constellations);
			threadlist.add(algThread);
			algThread.start();
		}
		for(AlgorithmThread algThread : threadlist) {
			try {
				algThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
		long end = System.currentTimeMillis();
		long time = end - start;
		String timestr = "[" + ( time/1000/60 ) + ":" + (time/1000%60) + "." + (time%1000) + "]";
		
		
		//Counter berechnen und Ergebnis ausgeben
		for(AlgorithmThread algThread : threadlist) {
			solvecounter += algThread.getSolvecounter();
		}
		
		System.out.println(timestr + "\tfertig, solvecounter = " + solvecounter);
	}

	private boolean SquareIsSafe(int r, int c) {					//Pr�ft ob das �bergebene Feld von einer anderen Dame gedeckt ist.
		if (colNotFree[c] || rowNotFree[r] || diaRightNotFree[r-c+N-1] || diaLeftNotFree[r+c])			// wenn beide diagonalen und die spalte frei sind ist alles klar
			return false;										

		return true;
	}

	//gibt true zur�ck, wenn Rotation von aktueller Konstellation bereits vorhanden
	//und false, wenn nicht
	private boolean checkRotations(int i, int j, int k, int l) {
		//Drehung um 90�
		for(int[] constellation : startConstellations) {
			if(Arrays.equals( new int[]{N-1-k, N-1-l, j, i}, constellation)) {
				return true;
			}
		}
		//Drehung um 180�
		for(int[] constellation : startConstellations) {
			if(Arrays.equals( new int[]{N-1-j, N-1-i, N-1-l, N-1-k}, constellation)) {
				return true;
			}
		}
		//Drehung um 270�
		for(int[] constellation : startConstellations) {
			if(Arrays.equals( new int[]{l, k, N-1-i, N-1-j}, constellation)) {
				return true;
			}
		}

		return false;
	}
	
	// true, wenn Spieg. der aktuellen Konstellation an der Diagonale (o.l. -> u.r.) bereits in startconstellations 
	private boolean checkDiaLeft(int i, int j, int k, int l) {
		for(int[] constellation : startConstellations) {
			if(Arrays.equals(new int[]{N-1-l, N-1-k, N-1-j, N-1-i}, constellation)) {
				return true;
			}
		}
		
		return false;
	}
	
	// true, wenn konstellation 90� drehsymmetrisch
	private boolean symmetry90(int i, int j, int k, int l) {
		if(Arrays.equals(new Integer[] {i, j, k, l}, new Integer[]{N-1-k, N-1-l, j, i}))
			return true;
		return false;
	}
	
	
	// start the main
	public static void main(String[] args) {
		AlgorithmStarter algStarter = new AlgorithmStarter(17, 2, false);
		algStarter.startAlgorithm();
	}
	
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
	public boolean isPaused() {
		return pause;
	}
	
	public long getStarttime() {
		return start;
	}
	public long getStartConstLen() {
		return startConstellations.size();
	}
	public long getCalculatedStartConstellations() {
		long counter = 0;
		for(AlgorithmThread algThread : threadlist) {
			counter += algThread.getStartConstIndex();
		}
		return counter - 1;
	}
	public float getProgress() {
		if(threadlist == null)
			return 0;
		
		//Berechne progress
		float progress = 0;
		for(AlgorithmThread algThread : threadlist) {
			progress += algThread.getStartConstIndex();
		}
		return progress / startConstellations.size();
	}
	public long getSolvecounter() {
		return solvecounter;
	}
}
