
public class BoardProperties {
	
	public int[] boardIntegers;
	public int symmetry;
	
	public BoardProperties(int[] boardIntegers, int symmetry) {
		this.boardIntegers = boardIntegers.clone();
		this.symmetry = symmetry;
	}
	
}
