package calc;

import java.io.Serializable;
import java.util.ArrayDeque;

public class AlgorithmThread extends Thread implements Serializable {

	private static final long serialVersionUID = 1L;
	
	
	//Brettgröße, Lösungszähler, Symmetrie-Faktor, Bitmaske
	private int N;
	private long tempcounter = 0, solvecounter = 0;	
	private int startConstIndex = 0;
	private int mask;
	//Array, enthält die zur Angabe besetzter Felder von AlgorithmStarter berechneten Integers
	private int[] boardIntegers;
	//Liste der von AlgorithmStarter berechneten Start-Konstellationen
	private ArrayDeque<BoardProperties> boardPropertiesList, uncalculatedStartConstList;
	
	//Sachen fürs Pausieren und Speichern
	private boolean pause = false, cancel = false;
	

	public AlgorithmThread(int N, ArrayDeque<BoardProperties> boardPropertiesList) {
		
		this.N = N;
		this.boardPropertiesList = boardPropertiesList;
		uncalculatedStartConstList = boardPropertiesList;
		mask = (1 << N) - 1;						//Setze jedes Bit von mask auf 1
		boardIntegers = new int[N];
	}
	
	//Rekursive Funktion
	private void SetQueen(int ld, int rd, int col, int row) {
		//jedes gesetzte Bit in free entspricht einem freien Feld
		int free = ~(ld | rd | col | boardIntegers[row]) & mask;
		
		if(row == N-2) {
			if(free > 0)
				tempcounter++;
			return;
		}
		
		int bit;
		
		//Solange es in der aktuellen Zeile freie Positionen gibt...
		while(free > 0) {
			//setze Dame an Stelle bit
			bit = free & (-free);
			free -= bit;
			
			//gehe zu nächster Zeile
			SetQueen((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	private void SetQueenBig(int ld, int rd, int col, int row) {
		//prüfe, ob Benutzer pausieren oder abbrechen will
		if(pause) {
			while(pause) {
				if(cancel)
					return;
				
				try {
					sleep(50);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		} else if(cancel) {
			return;
		}
		
		//Berechnungen		//		//
		//jedes gesetzte Bit in free entspricht einem freien Feld
		int free = ~(ld | rd | col | boardIntegers[row]) & mask;

		if(row == N-2) {
			if(free > 0)
				tempcounter++;
			return;
		}

		int bit;

		//Solange es in der aktuellen Zeile freie Positionen gibt...
		while(free > 0) {
			//setze Dame an Stelle bit
			bit = free & (-free);
			free -= bit;

			//gehe zu nächster Zeile
			SetQueen((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	

	@Override
	public void run() {
		loop:
		for(BoardProperties boardProperties : boardPropertiesList) {
			//übernimm Parameter von boardProperties
			boardIntegers = boardProperties.boardIntegers;
			tempcounter = 0;
			
			//suche alle Lösungen für die aktuelle Start-Konstellation, beginne ab Zeile 1
			if(N < 20)
				SetQueen(0, 0, 0, 1);
			else
				SetQueenBig(0, 0, 0, 1);
			
			startConstIndex++;
			solvecounter += tempcounter * boardProperties.symmetry;
			
			//für Speicher- und Ladefunktion
			uncalculatedStartConstList.remove(boardProperties);
			
			//prüfe, ob Benutzer pausieren oder abbrechen will
			if(pause) {
				while(pause) {
					if(cancel)
						break loop;
					
					try {
						sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			} else if(cancel) {
				break;
			}
		}
	}
	
	public void pause() {
		pause = true;
	}
	public void go() {
		pause = false;
	}
	public void cancel() {
		cancel = true;
	}
	

	public int getStartConstIndex() {
		return startConstIndex;
	}
	public long getSolvecounter() {
		return solvecounter;
	}
	
	public ArrayDeque<BoardProperties> getUncalculatedStartConstellations(){
		return uncalculatedStartConstList;
	}
}
