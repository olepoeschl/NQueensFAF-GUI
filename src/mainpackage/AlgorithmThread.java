package mainpackage;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class AlgorithmThread extends Thread {

	//Globale Variablen
	private int N;
	private long trycounter = 0, tempcounter = 0, solvecounter = 0;
	private int index = 0;
	private ArrayList<Integer> freeRows;
	private boolean[] colNotFree, diaRightNotFree, diaLeftNotFree;
	private ArrayDeque<BoardProperties> boardPropertiesList;
	private int symmetry = 8;


	public AlgorithmThread(int N, ArrayDeque<BoardProperties> boardPropertiesList) {
		this.N = N;
		freeRows = new ArrayList<Integer>();
		colNotFree = new boolean[N];
		diaRightNotFree = new boolean[2*N - 1];
		diaLeftNotFree = new boolean[2*N - 1];
		this.boardPropertiesList = boardPropertiesList;
	}

	private void setQueen(Integer row) {
		for (int col = 1; col < N-1; col++) {	               	// für jede Spalte in der übergebenen Zeile (Zelle), wird folgendes ausgeführt
			if (squareIsSafe(row, col)) {                  
				trycounter++;									// Anzahl der Damensetzungen wird um 1 erhöht   
				
				if(index < freeRows.size()-1) {
					colNotFree[col] = true;							// spalte wird belegt
					diaRightNotFree[row-col+N-1] = true;			// dia right wird belegt
					diaLeftNotFree[row+col] = true;					// dia left wird belegt
					
					setQueen(freeRows.get(++index));
					index--;

					colNotFree[col] = false;						// macht die spalte wieder frei
					diaRightNotFree[row-col+N-1] = false;
					diaLeftNotFree[row+col] = false;				// macht die dias wieder frei
				} else {
					tempcounter++;
				}
			}
		} 
	}
	private boolean squareIsSafe(int r, int c) {					//Prüft ob das übergebene Feld von einer anderen Dame gedeckt ist.
		if (colNotFree[c] || diaRightNotFree[r-c+N-1] || diaLeftNotFree[r+c])			// wenn beide diagonalen und die spalte frei sind ist alles klar
			return false;										

		return true;
	}

	@Override
	public void run() {
		for(BoardProperties boardProperties : boardPropertiesList) {
			//Zeilen, Diagonalen besetzen
			freeRows = boardProperties.freeRows;
			colNotFree = boardProperties.colNotFree;
			diaRightNotFree = boardProperties.diaRightNotFree;
			diaLeftNotFree = boardProperties.diaLeftNotFree;
			symmetry = boardProperties.symmetry;
			

			tempcounter = 0;
			index = 0;
			setQueen(freeRows.get(index));
			solvecounter += tempcounter * symmetry;
		}
	}
	
	public long getSolvecounter() {
		return solvecounter;
	}
	public long getTrycounter() {
		return trycounter;
	}
}
