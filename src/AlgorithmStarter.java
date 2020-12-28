

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

public class AlgorithmStarter {

	private int N, mask;												//Brettgröße
	private int cpu;											//Anzahl der gewünschten Threads (Anzahl der Kerne)
	private int symmetry = 8;
	private boolean[] rowNotFree, colNotFree, diaLeftNotFree, diaRightNotFree;
	private ArrayDeque<BoardProperties> boardPropertiesList;

	ArrayDeque<int[]> startConstellations;


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
		int halfN = (N + (N % 2)) / 2;

		//Start-Konstellationen berechnen für 1.Dame ist nicht in der oberen linken Ecke
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
							
							colNotFree[0] = true;
							colNotFree[N-1] = true;
							
							int[] temp = new int[N];					// 1, wenn belegt, 0 sonst
							for(int m = 0; m < N; m++) {				// wird an AlgorithmThgread übergeben damit man weiß, welche Felder durch die Startpos. 
								for(int n = 0; n < N; n++) {			// schon belegt sind
									if(!SquareIsSafe(m, n)) 
										temp[m] += 1 << (N-1-n);
								}
							}
							
							colNotFree[0] = false;
							colNotFree[N-1] = false;
							
							temp[k] = mask >> 1;
							temp[l] = (mask >> 1) << 1;
							temp[0] = mask - (1<<(N-1-i));
							temp[N-1] = mask - (1<<(N-1-j));
							
							boardPropertiesList.add(new BoardProperties(temp, symmetry));				// boeardIntegersList enthät für jede startpos. zu jeder zeile einen integer der die belegung angibt
							
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
				if( SquareIsSafe(l, N-1) && !checkRotations(0, j, 0, l) && !checkDiaLeft(0, j, 0, l)) {
					rowNotFree[l] = true;
					diaRightNotFree[l] = true;
					diaLeftNotFree[l + N-1] = true;
					
					colNotFree[0] = true;
					colNotFree[N-1] = true;
					
					int[] temp = new int[N];					// 1, wenn belegt, 0 sonst
					for(int m = 0; m < N; m++) {				// wird an AlgorithmThgread übergeben damit man weiß, welche Felder durch die Startpos. 
						for(int n = 0; n < N; n++) {			// schon belegt sind
							if(!SquareIsSafe(m, n)) 
								temp[m] += 1 << (N-1-n);
						}
					}
					
					colNotFree[0] = false;
					colNotFree[N-1] = false;
					
					temp[0] = mask >> 1;
					temp[N-1] = ~(1 << (N-1-j)) & mask;
					temp[l] = (mask >> 1) << 1;
					boardPropertiesList.add(new BoardProperties(temp, 8));	
			
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
		colNotFree[0] = false;
		colNotFree[N-1] = false;
		
		
		//---
		ArrayList< ArrayDeque<BoardProperties> > threadConstellations = new ArrayList< ArrayDeque<BoardProperties>>(cpu);
		for(int i = 0; i < cpu; i++) {
			threadConstellations.add(new ArrayDeque<BoardProperties>());
		}

		//startConstellations in cpu viele Teile aufteilen
		System.out.println("Länge von startConstellations = " + startConstellations.size());
		Iterator<BoardProperties> iterator = boardPropertiesList.iterator();
		int i = 0;
		while(iterator.hasNext()) {
			threadConstellations.get((i++) % cpu).add(iterator.next());
		}
		
		long start = System.currentTimeMillis();
		
		//Thread starten und auf ihre Beendung warten
		ArrayList<AlgorithmThread> threadlist = new ArrayList<AlgorithmThread>();
		for(ArrayDeque<BoardProperties> constellations : threadConstellations) {
			AlgorithmThread algThread = new AlgorithmThread(N, constellations);
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
		long trycounter = 0, solvecounter = 0;
		for(AlgorithmThread algThread : threadlist) {
			trycounter += algThread.getTrycounter();
			solvecounter += algThread.getSolvecounter();
		}
		
		System.out.println(timestr + "\tfertig, solvecounter = " + solvecounter + ", trycounter = " + trycounter);
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
		for(int[] constellation : startConstellations) {
			if(Arrays.equals( new int[]{N-1-k, N-1-l, j, i}, constellation)) {
				return true;
			}
		}
		//Drehung um 180°
		for(int[] constellation : startConstellations) {
			if(Arrays.equals( new int[]{N-1-j, N-1-i, N-1-l, N-1-k}, constellation)) {
				return true;
			}
		}
		//Drehung um 270°
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
		AlgorithmStarter algStarter = new AlgorithmStarter(18, 2, false);
		algStarter.startAlgorithm();
	}
}
