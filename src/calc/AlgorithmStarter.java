package calc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import util.FAFProcessData;

public class AlgorithmStarter {

	private int N, mask;										// Brettgröße N, mask ist Integer mit N 1en rechts in der Bitdarstellung (entspricht dem Brett)
	private int cpu;											// Anzahl der gewünschten Threads (Anzahl der Kerne)
	private long old_solvecounter = 0;
	private int symmetry = 8;									// Vielfachheit der gefundenen Lösung
	int[] currentRows;											// beschreibt für aktuelle Startpos die Belegung der N Zeilen (als Int in Bitdarstellung)
	private boolean[] rowNotFree, colNotFree, diaLeftNotFree, diaRightNotFree;	// Belegung der Diagonalen, Zeilen, Spalten (true belegt, false frei)
	private ArrayDeque<BoardProperties> boardPropertiesList;	// Bretteigenschaften (Belegung einzelner Zeilen) zu jeder Startposition

	Set<Integer> startConstellations = new HashSet<Integer>();					// checkt, ob aktuelle Startposition schon gefunden wurde ( beachte Symmetrie)
	ArrayList<AlgorithmThread> threadlist;
	
	private long startConstCount = 0, calculatedStartConstCount = 0;
	
	//Variablen für den Speicher- und Ladevorgang
	//private long startConstCount;
	private boolean load = false;

	//Prozesszustands-Regelung
	private long start = 0, end = 0;
	private boolean pause = false;
	


	public AlgorithmStarter(int N, int cpu) {
		this.N = N;
		this.cpu = cpu;
		mask = (int) (Math.pow(2, N) - 1);

		startConstellations = new HashSet<Integer>();

		boardPropertiesList = new ArrayDeque<BoardProperties>();

		colNotFree = new boolean[N];
		rowNotFree = new boolean[N];
		diaLeftNotFree = new boolean[2*N-1];
		diaRightNotFree = new boolean[2*N-1];
	}

	public void startAlgorithm() {
		//Speichere Start-Zeit
		start = System.currentTimeMillis();
		
		if(!load) {
			int halfN = (N + (N % 2)) / 2;				// Dame nur links setzen, Rest eh symmetrisch
			//Start-Konstellationen berechnen für 1.Dame ist nicht in der oberen linken Ecke (hier muss man Symmetrie checken)
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

								if(i == N-1-j && k == N-1-l)		// 180° symmetrisch?
									if(symmetry90(i, j, k, l))		// sogar 90° symmetrisch?
										symmetry = 2;
									else
										symmetry = 4;
								else
									symmetry = 8;					// gar nicht symmetrisch


								colNotFree[0] = true;
								colNotFree[N-1] = true;

								currentRows = new int[N];					// 1, wenn belegt, 0 sonst
								for(int m = 1; m < N-1; m++) {				// wird an AlgorithmThgread übergeben damit man weiß, welche Felder durch die Startpos. 
									for(int n = 0; n < N; n++) {			// schon belegt sind
										if(!SquareIsSafe(m, n)) 
											currentRows[m] += 1 << (N-1-n);
									}
								}

								colNotFree[0] = false;
								colNotFree[N-1] = false;

								currentRows[k] = mask >> 1;					// überschreibe die Belegungen in Zeile und Spalte 1 und N
								currentRows[l] = (mask >> 1) << 1;
								currentRows[0] = mask - (1<<(N-1-i));
								currentRows[N-1] = mask - (1<<(N-1-j));

								boardPropertiesList.add(new BoardProperties(currentRows, symmetry));	// boeardIntegersList enthät für jede startpos. zu jeder zeile einen integer der die belegung angibt

								startConstellations.add((i<<24) + (j<<16) + (k<<8) + l);						// Sachen wieder freigeben	
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

			//Start-Konstellationen berechnen für 1.Dame auf Feld (0, 0)
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
						for(int m = 1; m < N-1; m++) {				// wird an AlgorithmThgread übergeben damit man weiß, welche Felder durch die Startpos. 
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
						startConstellations.add((1<<24) + (j<<16) + (1<<8) + l);

						rowNotFree[l] = false;
						diaRightNotFree[l] = false;
						diaLeftNotFree[l + N-1] = false;
					}
				}

				colNotFree[j] = false;
				diaRightNotFree[N-1 - j + N-1] = false;
				diaLeftNotFree[N-1 + j] = false;
			}
			
			//schreibe in startConstCount
			startConstCount = boardPropertiesList.size();
		}
		
		//---
		
		ArrayList< ArrayDeque<BoardProperties> > threadConstellations = new ArrayList< ArrayDeque<BoardProperties>>(cpu);
		for(int i = 0; i < cpu; i++) {
			threadConstellations.add(new ArrayDeque<BoardProperties>());
		}

		//startConstellations in cpu viele Teile aufteilen
		Iterator<BoardProperties> iterator = boardPropertiesList.iterator();
		int i = 0;
		while(iterator.hasNext()) {
			threadConstellations.get((i++) % cpu).add(iterator.next());
		}

		//Thread starten und auf ihre Beendung warten
		threadlist = new ArrayList<AlgorithmThread>();
		for(ArrayDeque<BoardProperties> constellations : threadConstellations) {
			AlgorithmThread algThread = new AlgorithmThread(N, constellations);
			threadlist.add(algThread);
//			algThread.setPriority(7);
			algThread.start();
		}
		for(AlgorithmThread algThread : threadlist) {
			try {
				algThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		//Zeit stoppen
		end = System.currentTimeMillis();
	}

	private boolean SquareIsSafe(int r, int c) {					//Prüft ob das übergebene Feld von einer anderen Dame gedeckt ist.
		if (colNotFree[c] || rowNotFree[r] || diaRightNotFree[r-c+N-1] || diaLeftNotFree[r+c])			// wenn beide diagonalen und die spalte frei sind ist alles klar
			return false;										

		return true;
	}

	//gibt true zurück, wenn Rotation von aktueller Konstellation bereits vorhanden
	//und false, wenn nicht
	private boolean checkRotations(int i, int j, int k, int l) {
		//Drehung um 90°
		if(startConstellations.contains(((N-1-k)<<24) + ((N-1-l)<<16) + (j<<8) + i)) 
			return true;

		//Drehung um 180°
		if(startConstellations.contains(((N-1-j)<<24) + ((N-1-i)<<16) + ((N-1-l)<<8) + N-1-k)) 
			return true;

		//Drehung um 270°
		if(startConstellations.contains((l<<24) + (k<<16) + ((N-1-i)<<8) + N-1-j)) 
			return true;

		return false;
	}

	// true, wenn Spieg. der aktuellen Konstellation an der Diagonale (o.l. -> u.r.) bereits in startconstellations 
	private boolean checkDiaLeft(int i, int j, int k, int l) {
		if(startConstellations.contains(((N-1-l)<<24) + ((N-1-k)<<16) + ((N-1-j)<<8) + N-1-i)) 
			return true;

		return false;
	}

	// true, wenn konstellation 90° drehsymmetrisch
	private boolean symmetry90(int i, int j, int k, int l) {
		if(((i << 24) + (j << 16) + (k << 8) + (l))   ==   (((N-1-k)<<24) + ((N-1-l)<<16) + (j<<8) + i))
			return true;
		return false;
	}


	// start the main
	public static void main(String[] args) {
		AlgorithmStarter algStarter = new AlgorithmStarter(18, 1);
		algStarter.startAlgorithm();
	}
	// --------------
	
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

	public long getStarttime() {
		return start;
	}
	public long getEndtime() {
		return end;
	}
	
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

		//Berechne progress
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
	
	//Laden des Fortschritts eines alten Rechenvorganges
	public void load(FAFProcessData fafprocessdata) {
		load = true;

//		N = fafprocessdata.N;
		boardPropertiesList.addAll(fafprocessdata);
		old_solvecounter = fafprocessdata.solvecounter;
		startConstCount = fafprocessdata.startConstCount;
		calculatedStartConstCount = fafprocessdata.calculatedStartConstCount;
	}
}
