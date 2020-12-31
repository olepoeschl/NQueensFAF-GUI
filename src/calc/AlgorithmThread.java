package calc;

import java.io.Serializable;
import java.util.ArrayDeque;

public class AlgorithmThread extends Thread implements Serializable {

	private static final long serialVersionUID = 1L;
	
	
	//Brettgr��e, L�sungsz�hler, Symmetrie-Faktor, Bitmaske
	private int N;
	private long tempcounter = 0, solvecounter = 0;	
	private int startConstIndex = 1;
	private int symmetry = 8;
	private int mask;
	//Array, enth�lt die zur Angabe besetzter Felder von AlgorithmStarter berechneten Integers
	private int[] boardIntegers;
	//Liste der von AlgorithmStarter berechneten Start-Konstellationen
	private ArrayDeque<BoardProperties> boardPropertiesList;
	
	//Sachen f�rs Pausieren und Speichern
	private boolean pause = false;
	

	public AlgorithmThread(int N, ArrayDeque<BoardProperties> boardPropertiesList) {
		
		this.N = N;
		this.boardPropertiesList = boardPropertiesList;
		mask = (int) (Math.pow(2, N) - 1);						//Setze jedes Bit von mask auf 1
		boardIntegers = new int[N];
	}
	
	//Rekursive Funktion
	private void SetQueen(int ld, int rd, int col, int row) {
		if(row == N-1) {
			//L�sung gefunden
			tempcounter++;
			return;
		}
		
		//jedes gesetzte Bit in free entspricht einem freien Feld
		int free = ~(ld | rd | col | boardIntegers[row]);
		
		//Solange es in der aktuellen Zeile freie Positionen gibt...
		while((free & mask) > 0) {
			//setze Dame an Stelle bit
			int bit = free & (-free);
			free -= bit;
			
			//gehe zu n�chster Zeile
			SetQueen((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}

	@Override
	public void run() {
		
		for(BoardProperties boardProperties : boardPropertiesList) {
			//�bernimm Parameter von boardProperties
			symmetry = boardProperties.symmetry;
			boardIntegers = boardProperties.boardIntegers;
			tempcounter = 0;
			
			//suche alle L�sungen f�r die aktuelle Start-Konstellation, beginne ab Zeile 1
			SetQueen(0, 0, 0, 1);
			
			startConstIndex++;
			solvecounter += tempcounter * symmetry;
			
			//pr�fe, ob Benutzer pausieren will
			if(pause) {
				while(pause) {
					try {
						sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}
	
	public void pause() {
		pause = true;
	}
	public void go() {
		pause = false;
	}
	
	
	public long getSolvecounter() {
		return solvecounter;
	}
	public int getStartConstIndex() {
		return startConstIndex;
	}
}
