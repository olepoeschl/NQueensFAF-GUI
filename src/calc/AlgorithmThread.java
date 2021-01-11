package calc;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayDeque;

import gui.Gui;

public class AlgorithmThread extends Thread implements Serializable {

	private static final long serialVersionUID = 1L;
	
	
	//Brettgröße, Lösungszähler, Symmetrie-Faktor, Bitmaske
	private int N;
	private long tempcounter = 0, solvecounter = 0;	
	private int startConstIndex = 0;
	private int mask, row1, row2;
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
	@SuppressWarnings("unused")
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
	
	
	@SuppressWarnings("unused")
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
			SetQueenBig((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	

	@Override
	public void run() {
		int const_delay_index = 200;
		Method method = null;
		try {
			if(N < 20) {
				if(row1>0)
					method = this.getClass().getDeclaredMethod("SetQueen1", int.class, int.class, int.class, int.class);
				else
					method = this.getClass().getDeclaredMethod("SetQueen2", int.class, int.class, int.class, int.class);
			} else {
				const_delay_index = 1;
				method = this.getClass().getDeclaredMethod("SetQueenBig", int.class, int.class, int.class, int.class);
			}
		} catch(NoSuchMethodException e) {
			e.printStackTrace();
		} catch(SecurityException e) {
			e.printStackTrace();
		}
		
		loop:
		for(BoardProperties boardProperties : boardPropertiesList) {
			//übernimm Parameter von boardProperties
			boardIntegers = boardProperties.boardIntegers;
			tempcounter = 0;
			if(boardProperties.k > boardProperties.l) {
				row1 = boardProperties.l;
				row2 = boardProperties.k;
			}
			else {
				row1 = boardProperties.k;
				row2 = boardProperties.l;
			}
			
			//suche alle Lösungen für die aktuelle Start-Konstellation, beginne ab Zeile 1
			try {
				
				method.invoke(this, 0, 0, 0, 1);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e1) {
				e1.printStackTrace();
			}
			
			//wieder eine Startpos. geschafft
			startConstIndex++;
			if(startConstIndex % (const_delay_index) == 0)
				Gui.progressUpdateQueue.add(128f);
			
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
