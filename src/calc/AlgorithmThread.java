package calc;

import java.io.Serializable;
import java.util.ArrayDeque;

public class AlgorithmThread extends Thread implements Serializable {

	
	//Brettgröße, Lösungszähler, Symmetrie-Faktor, Bitmaske
	private int N;
	private long tempcounter = 0, solvecounter = 0;	
	private int startConstIndex = 1;
	private int symmetry = 8;
	private int mask;
	//Array, enthält die zur Angabe besetzter Felder von AlgorithmStarter berechneten Integers
	private int[] boardIntegers;
	private int index = 0;				//index von boardIntegers[]; entspricht der akutellen Zeile
	//Liste der von AlgorithmStarter berechneten Start-Konstellationen
	private ArrayDeque<BoardProperties> boardPropertiesList;
	private ArrayDeque<BoardProperties> calculatedStartConstellations;
	
	//Sachen fürs Pausieren und Speichern
	private boolean pause = false, save = false;
	

	public AlgorithmThread(int N, ArrayDeque<BoardProperties> boardPropertiesList) {
		
		this.N = N;
		this.boardPropertiesList = boardPropertiesList;
		this.calculatedStartConstellations = boardPropertiesList;
		mask = (int) (Math.pow(2, N) - 1);						//Setze jedes Bit von mask auf 1
		boardIntegers = new int[N];
	}
	
	//Rekursive Funktion
	private void SetQueen(int ld, int rd, int col) {
		if(index == N-1) {
			//Lösung gefunden
			tempcounter++;
			return;
		}
		
		//jedes gesetzte Bit in free entspricht einem freien Feld
		int free = ~(ld | rd | col | boardIntegers[index]);
		
		//Solange es in der aktuellen Zeile freie Positionen gibt...
		while((free & mask) > 0) {
			//setze Dame an Stelle bit
			int bit = free & (-free);
			free -= bit;
			
			//prüfe, ob Benutzer pausieren will
			if(pause) {
				while(pause) {
					if(save) {
						
						
						save = false;
					}
					
					try {
						sleep(50);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
			//gehe zu nächster Zeile
			index++;
			SetQueen((ld|bit)<<1, (rd|bit)>>1, col|bit);
			index--;
		}
	}

	@Override
	public void run() {
		
		for(BoardProperties boardProperties : boardPropertiesList) {
			//übernimm Parameter von boardProperties
			symmetry = boardProperties.symmetry;
			boardIntegers = boardProperties.boardIntegers;
			tempcounter = 0;
			index = 0;
			
			//suche alle Lösungen für die aktuelle Start-Konstellation
			SetQueen(0, 0, 0);
			
			startConstIndex++;
			solvecounter += tempcounter * symmetry;
		}
	}
	
	public void pause() {
		pause = true;
	}
	public void go() {
		pause = false;
	}
	public void save() {
		save = true;
	}
	
	
	public long getSolvecounter() {
		return solvecounter;
	}
	public int getStartConstIndex() {
		return startConstIndex;
	}
}
