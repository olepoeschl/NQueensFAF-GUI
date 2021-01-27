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
	
	private int[] boardIntegers;		// occupancy of squares for rows 1,...,N-2 from starting constellation; hop rows and hop sizes
	private int max, mark1, mark2, hop1, hop2;
	
	// list of uncalculated starting positions, their indices
	private ArrayDeque<BoardProperties> boardPropertiesList;
	
	// for canceling and pausing 
	private boolean pause = false, cancel = false, respond = false;
	
	
	public AlgorithmThread(int N, ArrayDeque<BoardProperties> boardPropertiesList) {
		this.N = N;
		this.boardPropertiesList = boardPropertiesList;		
		boardIntegers = new int[N-3];
	}
	
	// Recursive functions for Placing the Queens
	// this is if all are there as one piece
	private void SetQueen1(int ld, int rd, int col, int idx, int free) {
		if(idx > max) {
			tempcounter++;
			return;
		}
		
		// calculate free squares for this line and bit is the rightmost free square (Queen will be placed at bit)
		int nextfree;
		int bit;
		
		// while there are free squares in this row
		while(free > 0) {
			// set a Queen at bit
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			
			// go to the next row and occupy diagonals and column)
			if(nextfree > 0)
				SetQueen1((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	// if rows are grouped in to pieces
	private void SetQueen21(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		if(idx > mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				
				nextfree = ~(((ld|bit)<<hop1) | ((rd|bit)>>hop1) | (col|bit)) & boardIntegers[idx+1];
				if(nextfree > 0)
					SetQueen22((ld|bit)<<hop1, (rd|bit)>>hop1, col|bit, idx+1, nextfree);
			}
			return;
		}
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen21((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SetQueen22(int ld, int rd, int col, int idx, int free) {
		if(idx > max) {
			tempcounter++;
			return;
		}
		int bit;
		int nextfree;
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen22((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	// if rows are grouped in 3 pieces
	private void SetQueen31(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		if(idx > mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<hop1) | ((rd|bit)>>hop1) | (col|bit)) & boardIntegers[idx+1];
				if(nextfree > 0)
					SetQueen32((ld|bit)<<hop1, (rd|bit)>>hop1, col|bit, idx+1, nextfree);
			}
			return;
		}
		while(free > 0) {
			bit = free & (-free);
			free -= bit;

			nextfree = ~( ((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen31((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SetQueen32(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		if(idx > mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<hop2) | ((rd|bit)>>hop2) | (col|bit)) & boardIntegers[idx+1];
				if(nextfree > 0)
					SetQueen33((ld|bit)<<hop2, (rd|bit)>>hop2, col|bit, idx+1, nextfree);
			}
			return;
		}
		while(free > 0) {
			bit = free & (-free);
			free -= bit;

			nextfree = ~( ((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen32((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
		
	private void SetQueen33(int ld, int rd, int col, int idx, int free) {
		if(idx > max) {
			tempcounter++;
			return;
		}
		int bit;
		int nextfree;
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~( ((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen33((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	
	// same stuff with the possibility to stop when a solution is found
	// this is slightly slower, but good for large N where a starting position might take several minutes or even longer
	private void SetQueen1Big(int ld, int rd, int col, int idx, int free) {
		if(idx > max) {
			tempcounter++;
			return;
		}
		
		int bit;
		int nextfree;
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen1Big((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SetQueen21Big(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		if(idx > mark1) {
			// check, if the user wants to pause or interrupt and wait until he wants to continue
			if(pause) {
				respond = true;
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
				respond = true;
				return;
			}
			
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				
				nextfree = ~(((ld|bit)<<hop1) | ((rd|bit)>>hop1) | (col|bit)) & boardIntegers[idx+1];
				if(nextfree > 0)
					SetQueen22Big((ld|bit)<<hop1, (rd|bit)>>hop1, col|bit, idx+1, nextfree);
			}
			return;
		}
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen21Big((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SetQueen22Big(int ld, int rd, int col, int idx, int free) {
		if(idx > max) {
			tempcounter++;
			return;
		}
		int bit;
		int nextfree;
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen22Big((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SetQueen31Big(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		if(idx > mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<hop1) | ((rd|bit)>>hop1) | (col|bit)) & boardIntegers[idx+1];
				if(nextfree > 0)
					SetQueen32Big((ld|bit)<<hop1, (rd|bit)>>hop1, col|bit, idx+1, nextfree);
			}
			return;
		}
		while(free > 0) {
			bit = free & (-free);
			free -= bit;

			nextfree = ~( ((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen31Big((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SetQueen32Big(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		if(idx > mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<hop2) | ((rd|bit)>>hop2) | (col|bit)) & boardIntegers[idx+1];
				if(nextfree > 0)
					SetQueen33Big((ld|bit)<<hop2, (rd|bit)>>hop2, col|bit, idx+1, nextfree);
			}
			return;
		}
		while(free > 0) {
			bit = free & (-free);
			free -= bit;

			nextfree = ~( ((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen32Big((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SetQueen33Big(int ld, int rd, int col, int idx, int free) {
		if(idx > max) {
			tempcounter++;
			return;
		}
		int bit;
		int nextfree;
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~( ((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen33Big((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}

	
	@Override
	public void run() {
		int listsize = boardPropertiesList.size();
		BoardProperties bp;
		
		loop:
		for(int i = 0; i < listsize; i++) {
			//initalize bp for this iteration
			bp = boardPropertiesList.getFirst();
			
			// get occupancy of the board for each starting constellation and the hops and max from board Properties
			boardIntegers = bp.boardIntegers;
			mark1 = bp.mark1;
			mark2 = bp.mark2;
			hop1 = bp.hop1;
			hop2 = bp.hop2;
			max = bp.max;
			tempcounter = 0;								// set counter of solutions for this starting constellation to 0
			
			// use SetQueenBig - methods for large N
			// skip SetQueen1 (or SetQueen1Big) if k = 0
			if(N < 24) {
				if(hop2 == 0) {
					if(hop1 == 0) 
						SetQueen1(0, 0, 0, 0, boardIntegers[0]);
					else 
						SetQueen21(0, 0, 0, 0, boardIntegers[0]);
				}
				else
					SetQueen31(0, 0, 0, 0, boardIntegers[0]);
					
			}
			else {
				if(hop2 == 0) {
					if(hop1 == 0) 
						SetQueen1Big(0, 0, 0, 0, boardIntegers[0]);
					else 
						SetQueen21Big(0, 0, 0, 0, boardIntegers[0]);
				}
				else
					SetQueen31Big(0, 0, 0, 0, boardIntegers[0]);
			}

			// one start constellation is done
			startConstIndex++;
			
			// sum up solutions
			solvecounter += tempcounter * bp.symmetry;
			
			// for saving and loading progress remove the finished starting constellation
			boardPropertiesList.removeFirst();
			
			// check if the user wants to pause or break
			if(pause) {
				respond = true;
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
				respond = true;
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
	public void dontCancel() {
		cancel = false;
	}

	// for progress
	public int getStartConstIndex() {
		return startConstIndex;
	}
	public long getSolvecounter() {
		return solvecounter;
	}
	public ArrayDeque<BoardProperties> getUncalculatedStartConstellations(){
		return boardPropertiesList;
	}
	
	public boolean responds() {
		if( ! respond)
			return false;
		
		respond = false;
		return true;
	}
}
