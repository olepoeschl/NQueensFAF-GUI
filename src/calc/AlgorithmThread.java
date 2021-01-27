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
	private int max, mark1, mark2, hop1, hop2;
	
	// list of uncalculated starting positions, their indices
	private ArrayDeque<Integer> startConstellations;
	
	// for canceling and pausing 
	private boolean pause = false, cancel = false, respond = false;
	
	
	public AlgorithmThread(int N, ArrayDeque<Integer> startConstellations) {
		this.N = N;	
		this.L = 1 << (N-1);
		smallmask = (1 << (N-2)) - 1;
		this.startConstellations = startConstellations;
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
	private void SetQueen2(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		if(idx > mark2) {
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
			
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen2((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	// if rows are grouped in 3 pieces
	private void SetQueen3(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		if(idx > mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<hop1) | ((rd|bit)>>hop1) | (col|bit)) & boardIntegers[idx+1];
				if(nextfree > 0)
					SetQueen2((ld|bit)<<hop1, (rd|bit)>>hop1, col|bit, idx+1, nextfree);
			}
			return;
		}
		while(free > 0) {
			bit = free & (-free);
			free -= bit;

			nextfree = ~( ((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen3((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	
	// same stuff with the possibility to stop when a solution is found
	// this is slightly slower, but good for large N where a starting position might take several minutes or even longer
	private void SetQueen1Big(int ld, int rd, int col, int idx, int free) {
		if(idx > max) {
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
	
	private void SetQueen2Big(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		if(idx > mark2) {
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
			
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen2Big((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	private void SetQueen3Big(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		if(idx > mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<hop1) | ((rd|bit)>>hop1) | (col|bit)) & boardIntegers[idx+1];
				if(nextfree > 0)
					SetQueen2((ld|bit)<<hop1, (rd|bit)>>hop1, col|bit, idx+1, nextfree);
			}
			return;
		}
		while(free > 0) {
			bit = free & (-free);
			free -= bit;

			nextfree = ~( ((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & boardIntegers[idx+1];
			if(nextfree > 0)
				SetQueen3Big((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}
	
	
	private void SQk01B(int ld, int rd, int col, int idx, int free) {
		if(idx > max) {
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
				SQk01B((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
		}
	}

	private void SQk02B_1(int ld, int rd, int col, int idx, int free) {
		int bit;
		int nextfree;
		
		if(idx > mark1) {
			while(free > 0) {
				bit = free & (-free);
				free -= bit;
				nextfree = ~(((ld|bit)<<hop1) | ((rd|bit|L)>>hop1) | (col|bit)) & smallmask;
				if(nextfree > 0)
					SQk01B((ld|bit)<<hop1, (rd|bit|L)>>hop1, col|bit, idx+1, nextfree);
			}
			return;
		}
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			nextfree = ~(((ld|bit)<<1) | ((rd|bit)>>1) | (col|bit)) & smallmask;
			if(nextfree > 0)
				SQk02B_1((ld|bit)<<1, (rd|bit)>>1, col|bit, idx+1, nextfree);
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
				ijkl = rot180(ijkl);
				i = geti(ijkl); j = getj(ijkl); k = getk(ijkl); l = getl(ijkl);
				
				if(k == 1) {
					ld = 1 << (N-i);
					rd = (1 << (N-4)) | (1 << (N-3)) | (1 << (N-4-i));
					col = (1 << (N-2-i));
					free = (~(ld|rd|col)) & smallmask;
					symmetry = 8;
					max = N-5;
					SQk01B(ld, rd, col, 0, free);
				}
				
				else {
					ld = (1 << (N-i-1)) | (L >> k);
					rd = (1 << (N-3)) | (1 << (N-3-i));
					col = (1 << (N-2-i));
					free = (~(ld|rd|col)) & smallmask;
					symmetry = 8;
					max = N-5;
					hop1 = 2;
					mark1 = k - 3;
					SQk02B_1(ld, rd, col, 0, free);
				}
			}
			// if queen not in corner
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
						mark2 = l-4;
						hop2 = 2;
					}
				}
				else {
					mark1 = k-3;
					hop1 = 2;
					if(l == k+1) {
						hop1 = 3;
					}
					else {
						mark2 = l-4;
						hop2 = 2;
					}
					
				}
				
				max = N-6;
				
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
						SetQueen3(0, 0, 0, 0, boardIntegers[0]);
						
				}
				
				else {
					if(hop2 == 0) {
						if(hop1 == 0) 
							SetQueen1Big(0, 0, 0, 0, boardIntegers[0]);
						else 
							SetQueen2Big(0, 0, 0, 0, boardIntegers[0]);
					}
					else
						SetQueen3Big(0, 0, 0, 0, boardIntegers[0]);
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
	
	private int rot180(int ijkl) {
		return ((N-1-getj(ijkl))<<24) + ((N-1-geti(ijkl))<<16) + ((N-1-getl(ijkl))<<8) + N-1-getk(ijkl);
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
}
