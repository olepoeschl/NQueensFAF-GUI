package calc;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;

import gui.Gui;

public class AlgorithmThread extends Thread implements Serializable {

	private static final long serialVersionUID = 1L;
	
	
	//Brettgr��e, L�sungsz�hler, Symmetrie-Faktor, Bitmaske
	private int N;
	private long tempcounter = 0, solvecounter = 0;	
	private int startConstIndex = 0;
	private int mask;
	private int[] rows_k_l;
	//Array, enth�lt die zur Angabe besetzter Felder von AlgorithmStarter berechneten Integers
	private int[] boardIntegers;
	//Liste der von AlgorithmStarter berechneten Start-Konstellationen
	private ArrayDeque<BoardProperties> boardPropertiesList, uncalculatedStartConstList;
	
	//Sachen f�rs Pausieren und Speichern
	private boolean pause = false, cancel = false;
	

	public AlgorithmThread(int N, ArrayDeque<BoardProperties> boardPropertiesList) {
		
		this.N = N;
		this.boardPropertiesList = boardPropertiesList;
		uncalculatedStartConstList = boardPropertiesList;
		mask = (1 << N) - 1;						//Setze jedes Bit von mask auf 1
		boardIntegers = new int[N];
		
		rows_k_l = new int[2];
	}
	
	//Rekursive Funktion
	private void SetQueen1(int ld, int rd, int col, int row) {
		
		if(row == rows_k_l[0]) {
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
			
			//gehe zu n�chster Zeile
			SetQueen1((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	
	private void SetQueen2(int ld, int rd, int col, int row) {
		
		if(row == rows_k_l[1]) {
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
			
			//gehe zu n�chster Zeile
			SetQueen2((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	
	
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
			
			//gehe zu n�chster Zeile
			SetQueen3((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	
	
	private void SetQueenBig(int ld, int rd, int col, int row) {
		//pr�fe, ob Benutzer pausieren oder abbrechen will
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

			//gehe zu n�chster Zeile
			SetQueenBig((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	

	@Override
	public void run() {
		int const_delay_index;
		if(N < 20)
			const_delay_index = 200;
		else
			const_delay_index = 1;
		
		loop:
		for(BoardProperties boardProperties : boardPropertiesList) {
			//�bernimm Parameter von boardProperties
			boardIntegers = boardProperties.boardIntegers;
			tempcounter = 0;
			if(boardProperties.k > boardProperties.l) {
				rows_k_l[0] = boardProperties.l;
				rows_k_l[1] = boardProperties.k;
			}
			else {
				rows_k_l[0] = boardProperties.k;
				rows_k_l[1] = boardProperties.l;
			}
			
			//�berspringe SetQueen1 ggf.
			if(N < 20) {
				if(rows_k_l[0] > 0)
					SetQueen1(0, 0, 0, 1);
				else
					SetQueen2(0, 0, 0, 1);
			} else {
				SetQueenBig(0, 0, 0, 1);
			}
			
			//wieder eine Startpos. geschafft
			startConstIndex++;
			if(startConstIndex % (const_delay_index) == 0)
				Gui.progressUpdateQueue.add(128f);
			
			//aktualisiere solvecounter
			solvecounter += tempcounter * boardProperties.symmetry;
			
			//f�r Speicher- und Ladefunktion
			uncalculatedStartConstList.remove(boardProperties);
			
			//pr�fe, ob Benutzer pausieren oder abbrechen will
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
