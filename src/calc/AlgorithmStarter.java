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

public class AlgorithmStarter {

	private int N;										// Brettgröße N, mask ist Integer mit N 1en rechts in der Bitdarstellung (entspricht dem Brett)
	private int cpu;											// Anzahl der gewünschten Threads (Anzahl der Kerne)
	private long old_solvecounter = 0;
	private int symmetry = 8;									// Vielfachheit der gefundenen Lösung
	int[] currentRows;											// beschreibt für aktuelle Startpos die Belegung der N Zeilen (als Int in Bitdarstellung)
	private ArrayDeque<BoardProperties> boardPropertiesList;	// Bretteigenschaften (Belegung einzelner Zeilen) zu jeder Startposition

	Set<Integer> startConstellations = new HashSet<Integer>();					// checkt, ob aktuelle Startposition schon gefunden wurde ( beachte Symmetrie)
	ArrayList<AlgorithmThread> threadlist;
	
	private long startConstCount = 0, calculatedStartConstCount = 0;
	
	//Variablen für den Speicher- und Ladevorgang
	//private long startConstCount;
	private boolean load = false;

	//Prozesszustands-Regelung
	private long start = 0, end = 0;
	private boolean ready = false, pause = false;


	public AlgorithmStarter(int N, int cpu) {
		this.N = N;
		this.cpu = cpu;

		startConstellations = new HashSet<Integer>();

		boardPropertiesList = new ArrayDeque<BoardProperties>();
	}

	public void startAlgorithm() {
		//Garbage-Collection; einmal Müll aufsammeln bitte
		System.gc();
		
		//Speichere Start-Zeit
		start = System.currentTimeMillis();
		
		if(!load) {
			int halfN = (N + (N % 2)) / 2;				// Dame nur links setzen, Rest eh symmetrisch
			int mask = (1 << N) - 1;
			int col, ld, rd, row;
			

			//Start-Konstellationen berechnen für 1.Dame auf Feld (0, 0)
			for(int j = 1; j < N-2; j++) {
				for(int l = j+1; l < N-1; l++) {

					currentRows = new int[N-2];					// 1, wenn belegt, 0 sonst
					row = 1;
					ld = 0;
					rd = (1 << (N-1)) | (1 << l);
					col = (1 << (N-1)) | 1 | (1 << (N-1-j));

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

					currentRows[l-1] = 1;

					boardPropertiesList.add(new BoardProperties(currentRows, 8, 0, l));	
					startConstellations.add((1<<24) + (j<<16) + (1<<8) + l);
				}
			}
			
			
			//Start-Konstellationen berechnen für 1.Dame ist nicht in der oberen linken Ecke (hier muss man Symmetrie checken)
			for(int i = 1; i < halfN; i++) {			// erste Zeile durchgehen		
				for(int j = i+1; j < N-1; j++) {		// letzte Zeile durchgehen
					for(int k = i+1; k < N-1; k++) {				// erste Spalte durchgehen
						if(k == N-1-j)
							continue;
						for(int l = N-i-2; l > 0; l--) {						// letzte Spalte durchgehen
							if(l==k || l == j)
								continue;
							
							if(!checkRotations(i, j, k, l)) {		// wenn zul. und neu, dann neue Startpos. gefunden
								
								if(i == N-1-j && k == N-1-l)		// 180° symmetrisch?
									if(symmetry90(i, j, k, l))		// sogar 90° symmetrisch?
										symmetry = 2;
									else
										symmetry = 4;
								else
									symmetry = 8;					// gar nicht symmetrisch

								currentRows = new int[N-2];					// 1, wenn belegt, 0 sonst
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
								
								currentRows[k-1] = 1 << (N-1);					// überschreibe die Belegungen in Zeile und Spalte 1 und N
								currentRows[l-1] = 1;

								boardPropertiesList.add(new BoardProperties(currentRows, symmetry, k, l));	// boeardIntegersList enthät für jede startpos. zu jeder zeile einen integer der die belegung angibt
								startConstellations.add((i<<24) + (j<<16) + (k<<8) + l);						// Sachen wieder freigeben	
							}
						}
					}
				}
			}

			//speichere anzahl der startkonstellationen in startConstCount
			startConstCount = boardPropertiesList.size();
			//Ausgabe in Gui
			Gui.print(startConstCount + " Start-Konstellationen gefunden in " + Gui.getTimeStr(), true);
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
		ExecutorService executor = Executors.newFixedThreadPool(cpu);
		
		threadlist = new ArrayList<AlgorithmThread>();
		for(ArrayDeque<BoardProperties> constellations : threadConstellations) {
			AlgorithmThread algThread = new AlgorithmThread(N, constellations);
			threadlist.add(algThread);
			executor.submit(algThread);
		}
		
		//threadlist erstellt, alles ready
		ready = true;
		
		//Warte auf Beendigung des executors
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
		
		//Zeit stoppen, da 100% erreicht
		end = System.currentTimeMillis();
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

	// true, wenn konstellation 90° drehsymmetrisch
	private boolean symmetry90(int i, int j, int k, int l) {
		if(((i << 24) + (j << 16) + (k << 8) + (l))   ==   (((N-1-k)<<24) + ((N-1-l)<<16) + (j<<8) + i))
			return true;
		return false;
	}

	//	//	//
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
