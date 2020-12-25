package mainpackage;

import java.util.ArrayList;

public class BoardProperties {
	
	public ArrayList<Integer> freeRows;
	public boolean[] colNotFree, diaRightNotFree, diaLeftNotFree;
	public int symmetry;
	
	public BoardProperties(ArrayList<Integer> freeRows, boolean[] colNotFree, boolean[] diaRightNotFree, boolean[] diaLeftNotFree, int symmetry) {
		this.freeRows = (ArrayList<Integer>) freeRows.clone();
		this.colNotFree = colNotFree.clone();
		this.diaRightNotFree = diaRightNotFree.clone();
		this.diaLeftNotFree = diaLeftNotFree.clone();
		this.symmetry = symmetry;
	}
	
}
