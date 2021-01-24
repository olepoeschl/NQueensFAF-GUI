package calc;

import java.io.Serializable;

// passes the information about each starting constellation to AlgorithmThread

// boardIntegers are N-2 Integers for the row 1,...,N-2
// their bit representation shows, which squares of the NxN - chess - board are attacked by the Queens in the starting constellation
// 1 is free, 0 is occupied
// if there already is a Queen in that specific row (row k or l), then only the bit where the Queen stands is 1

// symmetry is the count of the solution depending on the symmetry of the starting constellation and thus the count of any solution found from this starting constellation
// 8 for unsymmetric starting positions, 4 if sym. for rot180, 2 if sym for rot90

// k is idx of the line where the Queen is on the first column
// l is the idx where the Queen is in thelast column

public class BoardProperties implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	
	public int[] boardIntegers;			// N Integer representing the occupancy for each row 1,...,N-2 (in bit representation)
	public int mark1, mark2;			// marks the row, where we will skip hop1 or hop2 many rows in AlgorithmThread
	public int hop1, hop2;				
	public int symmetry;				// solution counts as 8, 4 or 2 depending on symmetry
	public int max;						// max idx, N-4 or N-5 if queen in corner or not
	
	public BoardProperties(int[] boardIntegers, int[] hopmarker, int[] hopsize, int[] sym_max) {
		this.boardIntegers = boardIntegers;
		this.mark1 = hopmarker[0];
		this.mark2 = hopmarker[1];
		this.hop1 = hopsize[0];
		this.hop2 = hopsize[1];
		this.symmetry = sym_max[0];
		this.max = sym_max[1];
	}
	
}
