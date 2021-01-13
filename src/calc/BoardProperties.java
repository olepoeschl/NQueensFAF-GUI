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
	
	
	public int[] boardIntegers;			// N Integers, deren Bit-Darstellung jeweils der Belegung der Zeile entspricht
	public int symmetry;				// Lösung wird je nach Symmetrie 2x od. 4x od. 8x gezählt (symmetry ist 2 od. 4 od. 8)
	public int k, l;
	
	public BoardProperties(int[] boardIntegers, int symmetry, int k, int l) {
		this.boardIntegers = boardIntegers;
		this.symmetry = symmetry;
		this.k = k;
		this.l = l;
	}
	
}
