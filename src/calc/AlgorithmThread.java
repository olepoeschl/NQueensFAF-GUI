package calc;

import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class AlgorithmThread extends Thread implements Serializable {

	private static final long serialVersionUID = 1L;
	
	
	//Brettgröße, Lösungszähler, Symmetrie-Faktor, Bitmaske
	private int N;
	private long solvecounter = 0;	
	private int startConstIndex = 0;
	private int mask;
	//Liste der von AlgorithmStarter berechneten Start-Konstellationen
	private ArrayDeque<BoardProperties> boardPropertiesList, uncalculatedStartConstList;
	
	//Sachen fürs Pausieren und Speichern
	private boolean pause = false, cancel = false;
	

	public AlgorithmThread(int N, ArrayDeque<BoardProperties> boardPropertiesList) {
		
		this.N = N;
		this.boardPropertiesList = boardPropertiesList;
		uncalculatedStartConstList = boardPropertiesList;
		mask = (1 << N) - 1;						//Setze jedes Bit von mask auf 1
	}

	@Override
	public void run() {
//		int const_delay_index;
//		if(N < 20)
//			const_delay_index = 200;
//		else
//			const_delay_index = 1;
		
		
		//Executor für die Abwicklung der SetQueen()-Aufrufe als Threads
		ExecutorService executor = Executors.newCachedThreadPool();
		ArrayDeque<SetQueenTask> tasks = new ArrayDeque<SetQueenTask>();
		SetQueenTask task;
		
//		loop:
		for(BoardProperties boardProperties : boardPropertiesList) {
			//neuer Task in die Warteschlange
			task = new SetQueenTask(boardProperties);
			tasks.add(task);
			executor.submit(task);
		}
		
		
		//Warte auf Beendigung der Threads
		executor.shutdown();
		try {
			executor.awaitTermination(1, TimeUnit.DAYS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		//tempcounter zusammenzählen
		for(SetQueenTask sqtask : tasks) {
			solvecounter += sqtask.tempcounter * sqtask.symmetry;
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
	
	
	
	
	
	//	//	//	//	//-------------------
	private class SetQueenTask implements Runnable {
		
		public long tempcounter = 0;
		public int symmetry;
		private int[] boardIntegers;
		private int row1, row2;
		
		public SetQueenTask(BoardProperties bp) {
			symmetry = bp.symmetry;
			boardIntegers = bp.boardIntegers;
			
			if(bp.k > bp.l) {
				row1 = bp.l;
				row2 = bp.k;
			}
			else {
				row1 = bp.k;
				row2 = bp.l;
			}
		}
		
		@Override
		public void run() {
			if(N < 20) {
				if(row1 > 0) {
					SetQueen1(0, 0, 0, 1);
				}
				else {
					SetQueen2(0, 0, 0, 1);
				}
			} else {
				SetQueenBig(0, 0, 0, 1);
			}
		}
		
		//	//	//
		//Rekursive Funktionen
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
	}
	//	//	//	//	//-------------
}
