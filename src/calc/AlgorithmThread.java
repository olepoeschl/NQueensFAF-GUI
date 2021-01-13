package calc;

import java.io.Serializable;
import java.util.ArrayDeque;

import gui.Gui;

public class AlgorithmThread extends Thread implements Serializable {

	private static final long serialVersionUID = 1L;
	
	
	//Brettgröße, Lösungszähler, Symmetrie-Faktor, Bitmaske
	private int N;
	private long tempcounter = 0, solvecounter = 0;	
	private int startConstIndex = 0;
	private int mask;
	private int row1, row2;
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
	
	//Rekursive Funktionen zum Setzen der Damen
	// SetQueen1, SetQueen2 und SetQueen 3 werden für das füllen eines Bretts benötigt
	
	// Setze Damen, bis man die erste Zeile erreicht, wo schon eine Dame steht
	// dann geht man einfach noch eine zeile weiter und ruft SetQueen2 auf
	private void SetQueen1(int ld, int rd, int col, int row) {
		
		if(row == row1) {
			SetQueen2(ld<<1, rd>>1, col, row+1);
			return;
		}
		
		//jedes gesetzte Bit in free entspricht einem freien Feld
		int free = ~(ld | rd | col) & boardIntegers[row-1];
		int bit;
		
		//Solange es in der aktuellen Zeile freie Positionen gibt...
		while(free > 0) {
			//setze Dame an Stelle bit
			bit = free & (-free);
			free -= bit;
			
			//gehe zu nächster Zeile
			SetQueen1((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	
	// Setze Damen, bis man die zweite Zeile erreicht, wo schon eine Dame steht
	// dann geht man einfach noch eine zeile weiter und ruft SetQueen3 auf
	// man beginnt bei SetQueen2, wenn die Dame der ersten Zeile in der Ecke steht
	private void SetQueen2(int ld, int rd, int col, int row) {
		
		if(row == row2) {
			SetQueen3(ld<<1, rd>>1, col, row+1);
			return;
		}
		
		//jedes gesetzte Bit in free entspricht einem freien Feld
		int free = ~(ld | rd | col) & boardIntegers[row-1];
		int bit;
		
		//Solange es in der aktuellen Zeile freie Positionen gibt...
		while(free > 0) {
			//setze Dame an Stelle bit
			bit = free & (-free);
			free -= bit;
			
			//gehe zu nächster Zeile
			SetQueen2((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	
	// läuft bis zum Ende (theoretisch N-2, aber es kann ausnahmen geben, daher "if(row > N-3)"
	private void SetQueen3(int ld, int rd, int col, int row) {
		
		if(row > N-3) {
			if(row == N-2) {
				if((~(ld | rd | col) & boardIntegers[row-1])>0)
					tempcounter++;
			}
			else
				tempcounter++;
			return;
		}
		
	
		//jedes gesetzte Bit in free entspricht einem freien Feld
		int free = ~(ld | rd | col) & boardIntegers[row-1];
		int bit;
		
		//Solange es in der aktuellen Zeile freie Positionen gibt...
		while(free > 0) {
			//setze Dame an Stelle bit
			bit = free & (-free);
			free -= bit;
			
			//gehe zu nächster Zeile
			SetQueen3((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	
	// gleiche Funktionsweise wie beit den oberen 3 Methoden
	// aber mit der Möglichkeit beim finden einer lösung abzubrechen
	private void SetQueen1Big(int ld, int rd, int col, int row) {
		
		if(row == row1) {
			SetQueen2Big(ld<<1, rd>>1, col, row+1);
			return;
		}
		
		//jedes gesetzte Bit in free entspricht einem freien Feld
		int free = ~(ld | rd | col) & boardIntegers[row-1];
		int bit;
		
		//Solange es in der aktuellen Zeile freie Positionen gibt...
		while(free > 0) {
			//setze Dame an Stelle bit
			bit = free & (-free);
			free -= bit;
			
			//gehe zu nächster Zeile
			SetQueen1Big((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	
	private void SetQueen2Big(int ld, int rd, int col, int row) {
		
		if(row == row2) {
			SetQueen3Big(ld<<1, rd>>1, col, row+1);
			return;
		}
		
		//jedes gesetzte Bit in free entspricht einem freien Feld
		int free = ~(ld | rd | col) & boardIntegers[row-1];
		int bit;
		
		//Solange es in der aktuellen Zeile freie Positionen gibt...
		while(free > 0) {
			//setze Dame an Stelle bit
			bit = free & (-free);
			free -= bit;
			
			//gehe zu nächster Zeile
			SetQueen2Big((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	
	
	private void SetQueen3Big(int ld, int rd, int col, int row) {
		
		if(row > N-3) {
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
			
			if(row == N-2) {
				if((~(ld | rd | col) & boardIntegers[row-1])>0)
					tempcounter++;
			}
			else
				tempcounter++;
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
			SetQueen3Big((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	

	@Override
	public void run() {
		int const_delay_index;
		
		// passe Aktualisierungsrate an N an
		if(N < 16)
			const_delay_index = 1 << 12;
		else if(N < 18)
			const_delay_index = 200;
		else if(N == 18)
			const_delay_index = 50;
		else if(N == 19)
			const_delay_index = 10;
		else if(N == 20)
			const_delay_index = 3;
		else
			const_delay_index = 1;
		
		loop:
		for(BoardProperties boardProperties : boardPropertiesList) {
			//übernimm Parameter von boardProperties
			boardIntegers = boardProperties.boardIntegers;
			tempcounter = 0;
			// row1 ist die kleinere
			if(boardProperties.k > boardProperties.l) {
				row1 = boardProperties.l;
				row2 = boardProperties.k;
			}
			else {
				row1 = boardProperties.k;
				row2 = boardProperties.l;
			}
			
			// Überspringe SetQueen1 ggf.
			// wenn N groß, dann erlaube immer beim finden einer lösung dass abgebrochen wird (Big-Methoden)
			if(N < 25) {
				if(row1 > 0)
					SetQueen1(0, 0, 0, 1);
				else
					SetQueen2(0, 0, 0, 1);
			} else {
				if(row1 > 0)
					SetQueen1Big(0, 0, 0, 1);
				else
					SetQueen2Big(0, 0, 0, 1);
			}
			
			//wieder eine Startpos. geschafft
			startConstIndex++;
//			if(startConstIndex % (const_delay_index) == 0)
//				Gui.progressUpdateQueue.add(128f);
			
			//aktualisiere solvecounter
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
