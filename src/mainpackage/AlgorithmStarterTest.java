package mainpackage;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class AlgorithmStarterTest {

	private int N;												//Brettgr��e
	private int cpu;											//Anzahl der gew�nschten Threads (Anzahl der Kerne)
	private int symmetry = 8;

	ArrayDeque<BoardProperties> boardPropertiesList;
	ArrayDeque<int[]> startConstellations;
	
	private ArrayList<Integer> liste, freeRows;
	private boolean[] rowNotFree, colNotFree;
	private boolean[] diaRightNotFree;					// diadonalen nach rechts unten, 2N - 1 st�ck
	// zeichnen sich dadurch aus, dass innerhalb einer diagonale gilt: zeile - spalte = konst.
	private boolean[] diaLeftNotFree;					// diagonalen nach links unten, 2N - 1 st�ck
	// zeichnen sich dadurch aus, dass innerhalb einer diagonale gilt: zeile + spalte = konst.


	public AlgorithmStarterTest(int N, int cpu, boolean pausable) {
		this.N = N;
		this.cpu = cpu;

		boardPropertiesList = new ArrayDeque<BoardProperties>();				//Reihenfolge der Indize: oben, unten, links, rechts
		startConstellations = new ArrayDeque<int[]>();
		
		liste = new ArrayList<Integer>();
		for(int i = 1; i < N-1; i++) {
			liste.add(i);
		}
		rowNotFree = new boolean[N];
		colNotFree = new boolean[N];
		diaRightNotFree = new boolean[2*N - 1];
		diaLeftNotFree = new boolean[2*N - 1];
	}

	public void startAlgorithm() {
		
		int num = 0;
		ArrayList<AlgorithmThreadTest> threadlist = new ArrayList<AlgorithmThreadTest>(cpu);
		for(int i = 0; i < cpu; i++) {
			AlgorithmThreadTest algThread = new AlgorithmThreadTest(N);
			threadlist.add(algThread);
			algThread.start();
		}

		//Zeit starten
		long start = System.currentTimeMillis();
		
		
		//Start-Konstellationen berechnen f�r 1.Dame ist nicht in der oberen linken Ecke
		int halfN = (N + (N % 2)) / 2;
		
		for(int i = 1; i < halfN; i++) {
			colNotFree[i] = true;					// Spalte wird belegt
			diaRightNotFree[-i+N-1] = true;			// dia right wird belegt
			diaLeftNotFree[i] = true;				// dia left wird belegt
			
			for(int j = i+1; j < N-1; j++) {
				if( ! SquareIsSafe(N-1, j))
					continue;
				
				colNotFree[j] = true;							// Spalte wird belegt
				diaRightNotFree[(N-1)-j+N-1] = true;			// dia right wird belegt
				diaLeftNotFree[(N-1)+j] = true;					// dia left wird belegt
				
				for(int k = i+1; k < N-1; k++) {
					if( ! SquareIsSafe(k, 0))
						continue;
					rowNotFree[k] = true;								// Zeile wird belegt
					diaRightNotFree[k+N-1] = true;						// dia right wird belegt
					diaLeftNotFree[k] = true;							// dia left wird belegt

					for(int l = 1; l < N-1; l++) {
						if( SquareIsSafe(l, N-1) && !checkRotations(i, j, k, l) && !checkDiaLeft(i, j, k, l)) {
							rowNotFree[l] = true;
							diaRightNotFree[l] = true;
							diaLeftNotFree[l + N-1] = true;

							if(i == N-1-j && k == N-1-l)
								if(symmetry90(i, j, k, l))
									symmetry = 2;
								else
									symmetry = 4;
							else
								symmetry = 8;
							
							freeRows = (ArrayList<Integer>) liste.clone();
							freeRows.remove((Object) k);
							freeRows.remove((Object) l);
							
							threadlist.get(num++%cpu).addTask( new BoardProperties(freeRows, colNotFree, diaRightNotFree, diaLeftNotFree, symmetry) );
							
							startConstellations.add(new int[]{i, j, k, l} );
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
				if( SquareIsSafe(l, N-1) && !checkRotations(0, j, 0, l) && !checkDiaLeft(0, j, 0, l)) {
					rowNotFree[l] = true;
					diaRightNotFree[l] = true;
					diaLeftNotFree[l + N-1] = true;

					freeRows = (ArrayList<Integer>) liste.clone();
					freeRows.remove((Object) l);
					
					threadlist.get(num++%cpu).addTask( new BoardProperties(freeRows, colNotFree, diaRightNotFree, diaLeftNotFree, 8) );
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
		
		diaRightNotFree[N-1] = false;
		diaLeftNotFree[0] = false;
		//---
		//Auf Beendung der Threads warten
		for(AlgorithmThreadTest algThread : threadlist) {
			algThread.end();
		}
		System.out.println("Gefundene startConstellations: " + startConstellations.size());
		System.out.println("End-Signale gesendet");
		for(AlgorithmThreadTest algThread : threadlist) {
			try {
				algThread.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//Zeit stoppen
		long end = System.currentTimeMillis();
		long time = end - start;
		String timestr = "[" + ( time/1000/60 ) + ":" + (time/1000%60) + "." + (time%1000) + "]";
		
		//Counter berechnen und Ergebnis ausgeben
		long trycounter = 0, solvecounter = 0;
		for(AlgorithmThreadTest algThread : threadlist) {
			trycounter += algThread.getTrycounter();
			solvecounter += algThread.getSolvecounter();
		}
		System.out.println(timestr + "\tfertig, solvecounter = " + solvecounter + ", trycounter = " + trycounter);
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
	private boolean checkDiaLeft(int i, int j, int k, int l) {
		for(int[] constellation : startConstellations) {
			if(Arrays.equals(new int[]{N-1-l, N-1-k, N-1-j, N-1-i}, constellation)) {
				return true;
			}
		}
		
		return false;
	}
	private boolean symmetry90(int i, int j, int k, int l) {
		if(Arrays.equals(new Integer[] {i, j, k, l}, new Integer[]{N-1-k, N-1-l, j, i}))
			return true;
		return false;
	}
	
	
	
	public static void main(String[] args) {
		AlgorithmStarterTest algStarter = new AlgorithmStarterTest(16, 8, false);
		algStarter.startAlgorithm();
	}
}
