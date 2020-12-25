package mainpackage;

import java.util.ArrayDeque;
import java.util.ArrayList;

public class AlgorithmThreadTest extends Thread {

	//Globale Variablen
	private int N;
	private long trycounter = 0, tempcounter = 0, solvecounter = 0;
	private int row_index = 0;
	private ArrayList<Integer> freeRows;
	private boolean[] colNotFree, diaRightNotFree, diaLeftNotFree;
	private int symmetry = 8;
	private ArrayList<BoardProperties> boardPropertiesList;
	private BoardProperties boardProperties;
	private int constellation_index = 0, tasknr = 0;
	private boolean new_task = false, end = false;


	public AlgorithmThreadTest(int N) {
		this.N = N;
		freeRows = new ArrayList<Integer>();
		colNotFree = new boolean[N];
		diaRightNotFree = new boolean[2*N - 1];
		diaLeftNotFree = new boolean[2*N - 1];
		boardPropertiesList = new ArrayList<BoardProperties>();
	}

	private void setQueen(Integer row) {
		for (int col = 1; col < N-1; col++) {	               	// für jede Spalte in der übergebenen Zeile (Zelle), wird folgendes ausgeführt
			if (squareIsSafe(row, col)) {                  
				trycounter++;									// Anzahl der Damensetzungen wird um 1 erhöht   
				
				if(row_index < freeRows.size()-1) {
					colNotFree[col] = true;							// spalte wird belegt
					diaRightNotFree[row-col+N-1] = true;			// dia right wird belegt
					diaLeftNotFree[row+col] = true;					// dia left wird belegt
					
					setQueen(freeRows.get(++row_index));
					row_index--;

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
		do {
			while(boardPropertiesList.size() > constellation_index) {
				boardProperties = boardPropertiesList.get(constellation_index);
				freeRows = boardProperties.freeRows;
				colNotFree = boardProperties.colNotFree;
				diaRightNotFree = boardProperties.diaRightNotFree;
				diaLeftNotFree = boardProperties.diaLeftNotFree;
				symmetry = boardProperties.symmetry;
				
				setQueen(freeRows.get(row_index));
				solvecounter += tempcounter * symmetry;
				tempcounter = 0;
				row_index = 0;
				constellation_index++;
				new_task = false;
			}
		} while( !end );
	}
	
	public void addTask(BoardProperties boardProperties) {
		boardPropertiesList.add(boardProperties);
		new_task = true;
	}
	public void end() {
		end = true;
	}
	
	public long getSolvecounter() {
		return solvecounter;
	}
	public long getTrycounter() {
		return trycounter;
	}
}
