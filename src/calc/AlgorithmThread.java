package calc;

import java.io.Serializable;
import java.util.ArrayDeque;

// this is the solver
// we use recursive functions for Backtracking

public class AlgorithmThread extends Thread implements Serializable {

	private static final long serialVersionUID = 1L;

	private int N;											// boardsize
	private long tempcounter = 0, solvecounter = 0;			// tempcounter is #(unique solutions) of current start constellation, solvecounter is #(all solutions)
	private int startConstIndex = 0;						// #(done start constellations)
	private int mask;										// marks the board, N '1's' in bit representation
	private int row1, row2;									// rows between 1,...,N-2 where Queen placed already
	
	private int[] boardIntegers;							// occupancy of squares for rows 1,...,N-2 from starting constellation
	
	// list of uncalculated starting positions, their indices
	private ArrayDeque<BoardProperties> boardPropertiesList, uncalculatedStartConstList;
	
	// for canceling and pausing 
	private boolean pause = false, cancel = false;
	
	
	public AlgorithmThread(int N, ArrayDeque<BoardProperties> boardPropertiesList) {
		this.N = N;
		this.boardPropertiesList = boardPropertiesList;
		uncalculatedStartConstList = boardPropertiesList;
		mask = (1 << N) - 1;					
		boardIntegers = new int[N];
	}
	
	// Recursive function for Placing the Queens
	// SetQueen1 places Queens until reaching row k or l, then skips to the next row and calls SetQueen2
	private void SetQueen1(int ld, int rd, int col, int row) {
		if(row == row1) {
			SetQueen2(ld<<1, rd>>1, col, row+1);
			return;
		}
		
		// calculate free squares for this line and bit is the rightmost free square (Queen will be placed at bit)
		int free = ~(ld | rd | col) & boardIntegers[row-1];
		int bit;
		
		// while there are free squares in this row
		while(free > 0) {
			// set a Queen at bit
			bit = free & (-free);
			free -= bit;
			
			// go to the next row and occupy diagonals and column)
			SetQueen1((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	
	// places Queens until reaching row k or l, then calls SetQueen3
	// we start with SetQueen2, if k = 0
	private void SetQueen2(int ld, int rd, int col, int row) {
		if(row == row2) {
			SetQueen3(ld<<1, rd>>1, col, row+1);
			return;
		}
		
		int free = ~(ld | rd | col) & boardIntegers[row-1];
		int bit;
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			SetQueen2((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	
	// places Queens until boatrd is full
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
		
		int free = ~(ld | rd | col) & boardIntegers[row-1];
		int bit;
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			SetQueen3((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	
	// same stuff with the possibility to stop when a solution is found
	// this is slightly slower, but good for large N where a starting position might take several minutes or even longer
	private void SetQueen1Big(int ld, int rd, int col, int row) {
		if(row == row1) {
			SetQueen2Big(ld<<1, rd>>1, col, row+1);
			return;
		}
		
		int free = ~(ld | rd | col) & boardIntegers[row-1];
		int bit;
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			SetQueen1Big((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	
	private void SetQueen2Big(int ld, int rd, int col, int row) {
		if(row == row2) {
			SetQueen3Big(ld<<1, rd>>1, col, row+1);
			return;
		}
		
		int free = ~(ld | rd | col) & boardIntegers[row-1];
		int bit;
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			SetQueen2Big((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	
	
	private void SetQueen3Big(int ld, int rd, int col, int row) {
		if(row > N-3) {
			// check, if the user wants to pause or interrupt and wait until he wants to continue
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
			}															// end of checking the pause condition
			
			if(row == N-2) {
				if((~(ld | rd | col) & boardIntegers[row-1])>0)
					tempcounter++;
			}
			else
				tempcounter++;
			return;
		}
		
		int free = ~(ld | rd | col | boardIntegers[row]) & mask;
		int bit;

		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			SetQueen3Big((ld|bit)<<1, (rd|bit)>>1, col|bit, row+1);
		}
	}
	

	@Override
	public void run() {
		loop:
		for(BoardProperties boardProperties : boardPropertiesList) {
			// get occupancy of the board for each starting constellation from board Properties
			boardIntegers = boardProperties.boardIntegers;
			tempcounter = 0;								// set counter of solutions for this starting constellation to 0
			// row1 is the smaller one
			if(boardProperties.k > boardProperties.l) {
				row1 = boardProperties.l;
				row2 = boardProperties.k;
			}
			else {
				row1 = boardProperties.k;
				row2 = boardProperties.l;
			}
			
			// use SetQueenBig - methods for large N
			// skip SetQueen1 (or SetQueen1Big) if k = 0
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
			
			// one start constellation is done
			startConstIndex++;
			
			// sum up solutions
			solvecounter += tempcounter * boardProperties.symmetry;
			
			// for saving and loading progress remove the finished starting constellation
			uncalculatedStartConstList.remove(boardProperties);
			
			// check if the user wants to pause or break
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
	
	// for pause and continue
	public void pause() {
		pause = true;
	}
	public void go() {
		pause = false;
	}
	public void cancel() {
		cancel = true;
	}

	// for progress
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
