package calc;

import java.io.Serializable;
import java.util.ArrayDeque;

// this is the solver
// we use recursive functions for Backtracking

public class AlgorithmThread extends Thread implements Serializable {

	private static final long serialVersionUID = 1L;

	private final int N, smallmask, L;									// boardsize
	private long tempcounter = 0, solvecounter = 0;			// tempcounter is #(unique solutions) of current start constellation, solvecounter is #(all solutions)
	private int startConstIndex = 0;						// #(done start constellations)
	
	private int[] boardIntegers;		// occupancy of squares for rows 1,...,N-2 from starting constellation; hop rows and hop sizes
	private int max, N5, N4, N3, mark1, mark2, mark3, hop1, hop2;
	private int ld1, rd1, ld2, rd2;
	
	// list of uncalculated starting positions, their indices
	private ArrayDeque<Integer> startConstellations;
	
	// for canceling and pausing 
	private boolean pause = false, cancel = false, respond = false;
	
	
	public AlgorithmThread(int N, ArrayDeque<Integer> startConstellations) {
		this.N = N;	
		N4 = N - 4;
		N5 = N - 5;
		N3 = N - 3;
		this.L = 1 << (N-1);
		smallmask = (1 << (N-2)) - 1;
		this.startConstellations = startConstellations;
		boardIntegers = new int[N-3];
	}
	
	// Recursive functions for Placing the Queens
	// this is if all are there as one piece
	private void SetQueen1(int ld, int rd, int col, int idx, int free) {
		if(idx == max) {
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
	private void SetQueen2(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		if(idx == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				
				nextfree = ~(((ld|bit)<<hop1) | ((rd|bit)>>hop1) | (col|bit)) & boardIntegers[idx+1];
				if(nextfree > 0)
					SetQueen1((ld|bit)<<hop1, (rd|bit)>>hop1, col|bit, idx+1, nextfree);
			}
			return;
		}
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen2((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	// if rows are grouped in 3 pieces
	private void SetQueen31(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		if(idx == mark1) {
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
		if(idx == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<hop2) | ((rd|bit)>>hop2) | (col|bit)) & boardIntegers[idx+1];
				if(nextfree > 0)
					SetQueen1((ld|bit)<<hop2, (rd|bit)>>hop2, col|bit, idx+1, nextfree);
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
	
	
	// same stuff with the possibility to stop when a solution is found
	// this is slightly slower, but good for large N where a starting position might take several minutes or even longer
	private void SetQueen1Big(int ld, int rd, int col, int idx, int free) {
		if(idx == max) {
			// check if the user wants to pause or break
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
				SetQueen1Big((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	// if rows are grouped in to pieces
	private void SetQueen2Big(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		if(idx == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				
				nextfree = ~(((ld|bit)<<hop1) | ((rd|bit)>>hop1) | (col|bit)) & boardIntegers[idx+1];
				if(nextfree > 0)
					SetQueen1Big((ld|bit)<<hop1, (rd|bit)>>hop1, col|bit, idx+1, nextfree);
			}
			return;
		}
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen2Big((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	// if rows are grouped in 3 pieces
	private void SetQueen31Big(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		if(idx == mark1) {
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
		if(idx == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<hop2) | ((rd|bit)>>hop2) | (col|bit)) & boardIntegers[idx+1];
				if(nextfree > 0)
					SetQueen1Big((ld|bit)<<hop2, (rd|bit)>>hop2, col|bit, idx+1, nextfree);
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
	
	
	private void SQd0B(int ld, int rd, int col, int idx, int free) {
		if(idx == N4) {
			tempcounter++;
			return;
		}
		
		int bit;
		int nextfree;
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQd0B((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}

	private void SQd0BB(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<hop1) | ((rd|bit|L)>>hop1) | (col|bit)) & smallmask;
				if(nextfree > 0)
					SQd0B((ld|bit)<<hop1, (rd|bit|L)>>hop1, col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQd0BB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SQd1BB(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~((((ld|bit)<<hop2)|ld2) | (((rd|bit|L)>>hop2)|rd2) | (col|bit)) & smallmask;
				if(nextfree > 0)
					SQd1B(((ld|bit)<<hop2) | ld2, ((rd|bit|L)>>hop2) | rd2, col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQd1BB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SQd1B(int ld, int rd, int col, int idx, int free) {
		if(idx == N5) {
			tempcounter++;
			return;
		}
		
		int bit;
		int nextfree;
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQd1B((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	// all following SQ functions for N-1-j > 2
	private void SQBkBlBjrB(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | (1 << (N3))) & smallmask;
				if(nextfree > 0)
					SQBlBjrB(((ld|bit)<<2), ((rd|bit)>>2) | (1 << (N3)), col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQBkBlBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SQBlBjrB(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | 1) & smallmask;
				if(nextfree > 0)
					SQBjrB(((ld|bit)<<2) | 1, (rd|bit)>>2, col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQBlBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SQBjrB(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx == mark3) {
			free &= ~1;
			ld |= 1;
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
				if(nextfree > 0)
					SQB(((ld|bit)<<1), (rd|bit)>>1, col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SQB(int ld, int rd, int col, int idx, int free) {
		if(idx == N5) {
			tempcounter++;
			return;
		}
		
		int bit;
		int nextfree;
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SQBlBkBjrB(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | 1) & smallmask;
				if(nextfree > 0)
					SQBkBjrB(((ld|bit)<<2) | 1, (rd|bit)>>2, col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQBlBkBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SQBkBjrB(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | (1 << N3)) & smallmask;
				if(nextfree > 0)
					SQBjrB(((ld|bit)<<2), ((rd|bit)>>2) | (1 << N3), col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQBkBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SQBklBjrB(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<3) | ((rd|bit)>>3) | (col|bit) | (1 << N4) | 1) & smallmask;
				if(nextfree > 0)
					SQBjrB(((ld|bit)<<3) | 1, ((rd|bit)>>3) | (1 << N4), col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQBklBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SQBlkBjrB(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<3) | ((rd|bit)>>3) | (col|bit) | (1 << N3) | 2) & smallmask;
				if(nextfree > 0)
					SQBjrB(((ld|bit)<<3) | 2, ((rd|bit)>>3) | (1 << N3), col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQBlkBjrB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	// for N-1-j = 2
	private void SQd2BlkB(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<3) | ((rd|bit)>>3) | (col|bit) | (1 << N3) | 2) & smallmask;
				if(nextfree > 0)
					SQd2B(((ld|bit)<<3) | 2, ((rd|bit)>>3) | (1 << N3), col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQd2BlkB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SQd2BklB(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<3) | ((rd|bit)>>3) | (col|bit) | (1 << N4) | 1) & smallmask;
				if(nextfree > 0)
					SQd2B(((ld|bit)<<3) | 1, ((rd|bit)>>3) | (1 << N4), col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQd2BklB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SQd2BlBkB(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | 1) & smallmask;
				if(nextfree > 0)
					SQd2BkB(((ld|bit)<<2) | 1, (rd|bit)>>2, col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQd2BlBkB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SQd2BkBlB(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx == mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | (1 << (N3))) & smallmask;
				if(nextfree > 0)
					SQd2BlB(((ld|bit)<<2), ((rd|bit)>>2) | (1 << (N3)), col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQd2BkBlB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SQd2BlB(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | 1) & smallmask;
				if(nextfree > 0)
					SQd2B(((ld|bit)<<2) | 1, (rd|bit)>>2, col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQd2BlB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SQd2BkB(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx == mark2) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<2) | ((rd|bit)>>2) | (col|bit) | (1 << N3)) & smallmask;
				if(nextfree > 0)
					SQd2B(((ld|bit)<<2), ((rd|bit)>>2) | (1 << N3), col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQd2BkB((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SQd2B(int ld, int rd, int col, int idx, int free) {
		if(idx == N5) {
			if((free & (~1)) > 0) 
				tempcounter++;
			return;
		}
		
		int bit;
		int nextfree;
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQd2B((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	
	@Override
	public void run() {
		int listsize = startConstellations.size();
		int i, j, k, l, ijkl, ld, rd, col, row, symmetry = 0, diff, free, mask = (1 << N) - 1;
		
		loop:
		for(int a = 0; a < listsize; a++) {
			//initalize bp for this iteration
			ijkl = startConstellations.getFirst();
			i = geti(ijkl); j = getj(ijkl); k = getk(ijkl); l = getl(ijkl);
			boardIntegers = new int[N-3];
			hop1 = hop2 = mark1 = mark2 = 0;
			
			// if queen in corner
			if(k == 0) {
				ijkl = jasmin(ijkl);
				i = geti(ijkl); j = getj(ijkl); k = getk(ijkl); l = getl(ijkl);
				
				if(k == 1) {
					ld = 1 << (N-i);
					rd = (1 << (N-4)) | (1 << (N-3)) | (L >> (i+3));
					col = (1 << (N-2-i));
					free = (~(ld|rd|col)) & smallmask;
					symmetry = 8;
					max = N-4;
					SQd0B(ld, rd, col, 0, free);
				}
				
				else {
					ld = (1 << (N-i-1)) | (L >> k);
					rd = (1 << (N-3)) | (L >> (i+2));
					col = (1 << (N-2-i));
					free = (~(ld|rd|col)) & smallmask;
					symmetry = 8;
					max = N-4;
					hop1 = 2;
					mark1 = k - 2;
					SQd0BB(ld, rd, col, 0, free);
				}
			}
			// if queen not in corner
			else if( getj(jasmin(ijkl)) == N-2 && getl(jasmin(ijkl)) == getk(jasmin(ijkl))+1) {
				
				// if k < l after jasmin'ing the board
				if(getk(jasmin(ijkl)) < getl(jasmin(ijkl))) {
					
					// if theres no space between l and k
//					if(getl(jasmin(ijkl)) == getk(jasmin(ijkl))+1) {
						// initialize i,j,k,l
						ijkl = jasmin(ijkl);
						i = geti(ijkl);
						j = getj(ijkl);
						k = getk(ijkl);
						l = getl(ijkl);
						
						if(i == N-1-j && k == N-1-l)		// starting constellation symmetric by rot180?
							if(symmetry90(i, j, k, l))		// even by rot90?
								symmetry = 2;
							else
								symmetry = 4;
						else
							symmetry = 8;					// none of the above?
						
						ld = (1 << (N-i-1)) | (L >> k);
						rd = (1 << (N-2)) | (L >> (i+2)) | (1 << (l-2));
						col = (1 << (N-2-i)) | 1;
						free = (~(ld|rd|col)) & smallmask;
						
						mark2 = k - 2;
						hop2 = 3;
						ld2 = 1;
						rd2 = 1 << (N-4);
						
						max = N-5;
						
						SQd1BB(ld, rd, col, 0, free);
//					}
				}
			}
			
			// all cases for N-1-j > 2
			else if(N-1 - getj(jasmin(ijkl)) > 2) {
				ijkl = jasmin(ijkl);
				i = geti(ijkl); j = getj(ijkl); k = getk(ijkl); l = getl(ijkl);
				
				if(i == N-1-j && k == N-1-l)		// starting constellation symmetric by rot180?
					if(symmetry90(i, j, k, l))		// even by rot90?
						symmetry = 2;
					else
						symmetry = 4;
				else
					symmetry = 8;					// none of the above?
				
				ld = (1 << (N-i-1)) | (L >> k);
				rd = (1 << (N-1-j+N-3)) | (L >> (i+2)) | (1 << (l-2));
				col = (1 << (N-2-i)) | (1 << (N-2-j));
				free = (~(ld|rd|col)) & smallmask;
				
				mark3 = j - 2;
				
				if(k < l) {
					mark1 = k - 2;
					mark2 = l - 3;
					if(l == k+1) 
						SQBklBjrB(ld, rd, col, 0, free);
					else 
						SQBkBlBjrB(ld, rd, col, 0, free);
				}
				else {
					mark1 = l - 2;
					mark2 = k - 3;
					if(k == l+1) 
						SQBlkBjrB(ld, rd, col, 0, free);
					else 
						SQBlBkBjrB(ld, rd, col, 0, free);
				}
			}
			
			// cases with N-1-j = 2
			else if(N-1 - getj(jasmin(ijkl)) == 2) {
				ijkl = jasmin(ijkl);
				i = geti(ijkl); j = getj(ijkl); k = getk(ijkl); l = getl(ijkl);
				
				if(i == N-1-j && k == N-1-l)		// starting constellation symmetric by rot180?
					if(symmetry90(i, j, k, l))		// even by rot90?
						symmetry = 2;
					else
						symmetry = 4;
				else
					symmetry = 8;					// none of the above?
				
				ld = (1 << (N-i-1)) | (L >> k);
				rd = (1 << (N-1-j+N-3)) | (L >> (i+2)) | (1 << (l-2));
				col = (1 << (N-2-i)) | (1 << (N-2-j));
				free = (~(ld|rd|col)) & smallmask;
				
				if(k < l) {
					mark1 = k - 2;
					mark2 = l - 3;
					if(l == k+1) 
						SQd2BklB(ld, rd, col, 0, free);
					else 
						SQd2BkBlB(ld, rd, col, 0, free);
				}
				else {
					mark1 = l - 2;
					mark2 = k - 3;
					if(k == l+1) 
						SQd2BlkB(ld, rd, col, 0, free);
					else 
						SQd2BlBkB(ld, rd, col, 0, free);
				}
			}
			
			else {
				if(i == N-1-j && k == N-1-l)		// starting constellation symmetric by rot180?
					if(symmetry90(i, j, k, l))		// even by rot90?
						symmetry = 2;
					else
						symmetry = 4;
				else
					symmetry = 8;					// none of the above?

				row = 1;
				ld = (1 << (N-1-i)) | (1 << (N-1-k));
				rd = (1 << (N-1-i)) | (1 << l);
				col = (1 << (N-1)) | (1) | (1 << (N-1-i)) | (1 << (N-1-j));
				diff = 0;
				
				while(row < N-1) {
					ld = (ld<<1) & mask;
					rd >>= 1;
					if(row == k) {
						if(row == j)
							ld |= 1;
						rd |= (1 << (N-1));
						row++;
						diff++;
						continue;
					}
					if(row == l) {
						if(row == N-1-j)
							rd |= (1<<(N-1));
						ld |= 1;
						row++;
						diff++;
						continue;
					}
					if(row == j)
						ld |= 1;
					if(row == N-1-j)
						rd |= (1<<(N-1));
					boardIntegers[row-1-diff] = ~(ld | rd | col) & mask;
					row++;
				}
				
				if(k == 1) {
					if(l > 2 && l < N-2) {
						mark1 = l-3;
						hop1 = 2;
					}
				}
				else {
					mark1 = k-2;
					hop1 = 2;
					if(l == k+1) {
						hop1 = 3;
					}
					else {
						mark2 = l-3;
						hop2 = 2;
					}
					
				}
				
				max = N-5;
				
				// use SetQueenBig - methods for large N
				// skip SetQueen1 (or SetQueen1Big) if k = 0
				if(N < 25) {
					if(hop2 == 0) {
						if(hop1 == 0) 
							SetQueen1(0, 0, 0, 0, boardIntegers[0]);
						else 
							SetQueen2(0, 0, 0, 0, boardIntegers[0]);
					}
					else
						SetQueen31(0, 0, 0, 0, boardIntegers[0]);
						
				}
				
				else {
					if(hop2 == 0) {
						if(hop1 == 0) 
							SetQueen1Big(0, 0, 0, 0, boardIntegers[0]);
						else 
							SetQueen2Big(0, 0, 0, 0, boardIntegers[0]);
					}
					else
						SetQueen31Big(0, 0, 0, 0, boardIntegers[0]);
				}
			}
			
			// sum up solutions
			solvecounter += tempcounter * symmetry;
			
			// get occupancy of the board for each starting constellation and the hops and max from board Properties
			tempcounter = 0;								// set counter of solutions for this starting constellation to 0
			

			// one start constellation is done
			startConstIndex++;
			
			
			// for saving and loading progress remove the finished starting constellation
			startConstellations.removeFirst();

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
	
	private int geti(int ijkl) {
		return ijkl >> 24;
	}
	private int getj(int ijkl) {
		return (ijkl >> 16) & 255;
	}
	private int getk(int ijkl) {
		return (ijkl >> 8) & 255;
	}
	private int getl(int ijkl) {
		return ijkl & 255;
	}
	
	// true, if starting constellation is symmetric for rot90
	private boolean symmetry90(int i, int j, int k, int l) {
		if(((i << 24) + (j << 16) + (k << 8) + (l))   ==   (((N-1-k)<<24) + ((N-1-l)<<16) + (j<<8) + i))
			return true;
		return false;
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
	public ArrayDeque<Integer> getUncalculatedStartConstellations(){
		return startConstellations;
	}
	

	public boolean responds() {
		return respond;
	}
	public void resetRespond() {
		respond = false;
	}
	
	private int jasmin(int ijkl) {
		int min = Math.min(getj(ijkl), N-1 - getj(ijkl)), arg = 0;
		
		if(Math.min(geti(ijkl), N-1 - geti(ijkl)) < min) {
			arg = 2;
			min = Math.min(geti(ijkl), N-1 - geti(ijkl));
		}
		if(Math.min(getk(ijkl), N-1 - getk(ijkl)) < min) {
			arg = 3;
			min = Math.min(getk(ijkl), N-1 - getk(ijkl));
		}
		if(Math.min(getl(ijkl), N-1 - getl(ijkl)) < min) {
			arg = 1;
			min = Math.min(getl(ijkl), N-1 - getl(ijkl));
		}
		
		for(int i = 0; i < arg; i++) {
			ijkl = rot90(ijkl);
		}
		
		if(getj(ijkl) < N-1 - getj(ijkl))
			ijkl = mirvert(ijkl);
		
		return ijkl;
	}
	
	private int mirvert(int ijkl) {
		return toijkl(N-1-geti(ijkl), N-1-getj(ijkl), getl(ijkl), getk(ijkl));
	}
	
	private int rot90(int ijkl) {
		return ((N-1-getk(ijkl))<<24) + ((N-1-getl(ijkl))<<16) + (getj(ijkl)<<8) + geti(ijkl);
	}
	
	private int toijkl(int i, int j, int k, int l) {
		return (i<<24) + (j<<16) + (k<<8) + l;
	}
}
