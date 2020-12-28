

import java.util.ArrayDeque;

public class AlgorithmThread extends Thread {

	//Globale Variablen
	private int N;
	private long tempcounter = 0, solvecounter = 0;
	private int index = 0;
	private ArrayDeque<BoardProperties> boardPropertiesList;
	private int symmetry = 8;
	private int mask;
	private int[] boardIntegers;


	public AlgorithmThread(int N, ArrayDeque<BoardProperties> boardPropertiesList) {
		this.N = N;
		
		this.boardPropertiesList = boardPropertiesList;
		mask = (int) (Math.pow(2, N) - 1);
		boardIntegers = new int[N];
	}
	
	private void SetQueen(int ld, int rd, int col) {
		if(index == N-1) {
			tempcounter++;
			return;
		}
		
		int free = ~(ld | rd | col | boardIntegers[index]);
		
		while((free & mask) > 0) {
			int bit = free & (-free);
			free -= bit;
			index++;
			SetQueen((ld|bit)<<1, (rd|bit)>>1, col|bit);
			index--;
		}
	}


	@Override
	public void run() {
		for(BoardProperties boardProperties : boardPropertiesList) {
			symmetry = boardProperties.symmetry;
			boardIntegers = boardProperties.boardIntegers;
			tempcounter = 0;
			index = 0;
			SetQueen(0, 0, 0);
			solvecounter += tempcounter * symmetry;
		}
	}
	
	public long getSolvecounter() {
		return solvecounter;
	}
}
