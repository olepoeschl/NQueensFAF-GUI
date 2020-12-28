package calc;

// dient zum �bergeben der Startpositionen an AlgorithmThread

public class BoardProperties {
	
	public int[] boardIntegers;			// N Integers, deren Bit-Darstellung jeweils der Belegung der Zeile entspricht
	public int symmetry;				// L�sung wird je nach Symmetrie 2x od. 4x od. 8x gez�hlt (symmetry ist 2 od. 4 od. 8)
	
	public BoardProperties(int[] boardIntegers, int symmetry) {
		this.boardIntegers = boardIntegers;
		this.symmetry = symmetry;
	}
	
}
