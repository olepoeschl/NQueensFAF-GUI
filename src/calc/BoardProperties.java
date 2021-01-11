package calc;

import java.io.Serializable;

// dient zum Übergeben der Startpositionen an AlgorithmThread

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
