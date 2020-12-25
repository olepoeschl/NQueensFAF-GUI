package mainpackage;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Date;
import java.util.LinkedList;

public class AlgorithmThread_alt extends Thread{
	
	//Threading-Zeug
	private static boolean paused = false;
	private static boolean pausable = false;
	private long pausetime = 0;
	private int id;
	//---
	
	
	//Globale Variablen
	private int N, halfN, colMin, colMax;
	private long trycounter = 0, solvecounter = 0;
	private static long midcounter;
//	private int[] damen;
	private boolean[] colNotFree;						// true, wenn spalte belegt; false wenn frei
												// wird also automatisch richtig initialisiert ('not free' am anfang false)
	private boolean[] diaRightNotFree;					// diadonalen nach rechts unten, 2N - 1 stück
												// zeichnen sich dadurch aus, dass innerhalb einer diagonale gilt: zeile - spalte = konst.
	private boolean[] diaLeftNotFree;					// diagonalen nach links unten, 2N - 1 stück
												// zeichnen sich dadurch aus, dass innerhalb einer diagonale gilt: zeile + spalte = konst.
	
	
	public AlgorithmThread_alt(int N, int halfN, int colMin, int colMax, int id) {		// Einfacher AlgorithmThread_alt Konstruktor
																
		this.N = N;
		this.halfN = halfN;
		this.colMin = colMin;
		this.colMax = colMax;
//		damen = new int[N];
//		for(int i = 0; i < N; i++) {										// -1 heißt in dieser zeile ist keine dame 
//			damen[i] = -1;
//		}
		
		colNotFree = new boolean[N];
		diaRightNotFree = new boolean[2*N-1];
		diaLeftNotFree = new boolean[2*N-1];
		
		this.id = id;
	}
	
	
	public void run()											// Aufruf Funktion des Algorithmus
	{
		long tempcounter = 0;
		for(int col = colMin; col <= colMax; col++) {
//			damen[0] = col;
			colNotFree[col] = true;
			diaRightNotFree[- col + N - 1] = true;
			diaLeftNotFree[col] = true;
			System.out.println("1. Dame auf Feld " + (col+1) + " / " + N);

			if(pausable) {
				SetQueenPausable(1);							// rekursiver Algorithmus wird aufgerufen
			} else {
				SetQueen(1);
			}
			
//			damen[0] = -1;
			colNotFree[col] = false;
			diaRightNotFree[- col + N - 1] = false;
			diaLeftNotFree[col] = false;
			
			if(col == halfN - 1) {
				midcounter = solvecounter - tempcounter;
			} else {
				tempcounter = solvecounter;
			}
		}
		
	}
	
	private void SetQueen(int row)								// Konzept zur Schrittweise Abarbeitung
	{
		for (int col = 0; col < N; col++) {	                    // für jede Spalte in der übergebenen Zeile (Zelle), wird folgendes ausgeführt
			if (SquareIsSafe(row, col)) {                        // Ist die übergebene Zelle nicht gedeckt...            
//				damen[row] = col;								// dann setzen wir hier eine dame hin
				colNotFree[col] = true;							// spalte wird belegt
				diaRightNotFree[row-col+N-1] = true;			// dia right wird belegt
				diaLeftNotFree[row+col] = true;					// dia left wird belegt
				
				trycounter++;									// Anzahl der Damensetzungen wird um 1 erhöht      
				if (row < N-1){
					SetQueen(row + 1);
                }
				else {											// neue Lösung gefunden
                	solvecounter++;						
                }
				
//				damen[row] = -1; 								// dort ist keine dame mehr
				colNotFree[col] = false;						// macht die spalte wieder frei
				diaLeftNotFree[row+col] = false;				// macht die dias wieder frei
				diaRightNotFree[row-col+N-1] = false;
			}	
		}
	}
	private void SetQueenPausable(int row)								// Konzept zur Schrittweise Abarbeitung
	{
		for (int col = 0; col < N; col++) {	                    // für jede Spalte in der übergebenen Zeile (Zelle), wird folgendes ausgeführt
			if (SquareIsSafe(row, col)) {                        // Ist die übergebene Zelle nicht gedeckt...            
//				damen[row] = col;								// dann setzen wir hier eine dame hin
				colNotFree[col] = true;							// spalte wird belegt
				diaRightNotFree[row-col+N-1] = true;			// dia right wird belegt
				diaLeftNotFree[row+col] = true;					// dia left wird belegt
				
				trycounter++;									// Anzahl der Damensetzungen wird um 1 erhöht      

				if (row < N-1){
					SetQueenPausable(row + 1);
                }
				else {											// neue Lösung gefunden
                	solvecounter++;						
                }
				
//				damen[row] = -1; 								// dort ist keine dame mehr
				colNotFree[col] = false;						// macht die spalte wieder frei
				diaLeftNotFree[row+col] = false;				// macht die dias wieder frei
				diaRightNotFree[row-col+N-1] = false;
			}	
			
			
			if( paused ) {
				long pauseStart = new Date().getTime();
//				System.out.println("Thread " + id +  " pausiert");
				
				while(paused){
					try {
						Thread.sleep(0);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}

//				System.out.println("Thread " + id + " läuft wieder");
				
				if(id == 1) {									//soll nur der 1. Thread machen, damit die anderen direkt weiterarbeiten können und man insgesamt Zeit spart
					long pauseEnd = new Date().getTime();
					pausetime += (pauseEnd - pauseStart);
				}
			}
		}
	}
	
	private boolean SquareIsSafe(int r, int c) {					//Prüft ob das übergebene Feld von einer anderen Dame gedeckt ist.
		
		if (colNotFree[c] || diaRightNotFree[r-c+N-1] || diaLeftNotFree[r+c])			// wenn beide diagonalen und die spalte frei sind ist alles klar
			return false;										

		return true;
	}
	
	public long getTrycounter() {
		return trycounter;
	}
	public long getSolvecounter() {
		return solvecounter;
	}
	public static long getMidcounter() {
		return midcounter;
	}
	
	public long getPausetime() {
		return pausetime;
	}
	
	//Pausiere alle Threads
	public static void hold() {
		paused = true;
	}
	//Lasse alle Threads weiterlaufen
	//Lasse sie weiterlaufen
	public static void go() {
		paused = false;
	}
	public static void setPausable(boolean pausable) {
		AlgorithmThread_alt.pausable = pausable;
	}

//	//Algorithmus-Threads starten
//	ArrayList<Integer> runningThreads = new ArrayList<Integer>();
//	for(int i = 0; i < cpu;  i++) {
//		threadlist.get(i).start();
//		runningThreads.add(i);
//	}
//
//	int threadsStarted = cpu;
//	int threadsFinished = 0;
//
//
//	while(threadsFinished < halfN - 1) {
//		for(int i = 0; i < runningThreads.size() - 1; i++) {
//
//			if( ! threadlist.get( runningThreads.get(i) ).isAlive() ) {
//				runningThreads.remove( i );
//				threadsFinished++;
//
//				if(threadsStarted < halfN) {
//					threadlist.get(threadsStarted).start();
//					threadsStarted++;
//					runningThreads.add(threadsStarted);
//				}
//				break;
//			}
//		}
//
//		try {
//			Thread.sleep(1);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//	}

	
	/*
	 * OLE:
	 * 		
	 * 		(N=15, 4 threads) -> ca. 3,2 sekunden im schnitt
	 * 		(N=16, 4 threads) -> ca. 20 sekunden im schnitt
	 * 		(N=17, 6 threads) -> 2 min 55 s im schnitt
	 */
	
}
