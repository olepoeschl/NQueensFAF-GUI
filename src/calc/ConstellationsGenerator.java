package calc;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;

class ConstellationsGenerator {
	
	private int N, L, mask, LD, RD, counter, solvecounter = 0, sym, k, l, ld_mem, rd_mem, notfree, notmask, solvecounter2 = 0, iter = 0, start;
	private int kbit, lbit, kmask, lmask; 	// belegt de diagoanle auf der später k bzw. l dame stehen soll
	private int[] bits;
	private int[][] klcounter;
	private HashSet<Integer> startConstellations;
	private ArrayDeque<Integer> ld_list, rd_list, col_list, LD_list, RD_list, kl_list, sym_list, start_list;
	
	// calculate start constellations and return them as a HashSet<Integer> for cpu computation
	HashSet<Integer> genConstellationsCpu(int N){
		startConstellations = new HashSet<Integer>();
		
		// halfN half of N rounded up
		this.N = N;
		final int halfN = (N + 1) / 2;

		// calculating start constellations with the first Queen on square (0,0)
		for(int j = 1; j < N-2; j++) {						// j is idx of Queen in last row				
			for(int l = j+1; l < N-1; l++) {				// l is idx of Queen in last col
				startConstellations.add(toijkl(0, j, 0, l));
			}
		}

		// calculate starting constellations for no Queens in corners
		// look above for if missing explanation
		for(int k = 1; k < halfN; k++) {						// gothrough first col
			for(int l = k+1; l < N-1; l++) {					// go through last col
				for(int i = k+1; i < N-1; i++) {				// go through first row
					if(i == N-1-l)								// skip if occupied
						continue;
					for(int j = N-k-2; j > 0; j--) {			// go through last row
						if(j==i || l == j)
							continue;

						if(!checkRotations(i, j, k, l)) {		// if no rotation-symmetric starting constellation already found
							startConstellations.add(toijkl(i, j, k, l));
						}
					}
				}
			}
		}
		
		return startConstellations;
	}
	
	// calculate occupancy of starting row for gpu computation
	void genConstellationsGpu(int N) {
		ld_list = new ArrayDeque<Integer>();
		rd_list = new ArrayDeque<Integer>();
		col_list = new ArrayDeque<Integer>();
		LD_list = new ArrayDeque<Integer>();
		RD_list = new ArrayDeque<Integer>();
		kl_list = new ArrayDeque<Integer>();
		sym_list = new ArrayDeque<Integer>();
		start_list = new ArrayDeque<Integer>();
		
		int ld, rd, col, kl;
		L = (1 << (N-1));
		mask = (L << 1) - 1;
		notmask = ~mask;
		kmask = mask - L;	// hält nur ganz links frei für die dame
		lmask = mask - 1;	// ganz rechts das gleiche
		bits = new int[N];
		klcounter = new int[N][N];
		
		// set N, halfN half of N rounded up, collection of startConstellations
		this.N = N;
		final int halfN = (N + 1) / 2;
		startConstellations = new HashSet<Integer>();
		
		// start time
//		long starttime = System.currentTimeMillis();
		
		// calculating start constellations with the first Queen on square (0,0)
		for(int j = 1; j < N-2; j++) {						// j is idx of Queen in last row				
			for(int l = j+1; l < N-1; l++) {				// l is idx of Queen in last col
				startConstellations.add(toijkl(0, j, 0, l));

				ld = 0;
				rd = (L >>> 1) | (1 << (l-1));
				col = 1 | L | (L >>> j);
				LD = (L >>> j) | (L >>> l);
				RD = (L >>> j) | 1;
				
				bits[0] = L;
				bits[l] = 1;
				bits[N-1] = L >>> j;
				
//				System.out.println("ijkl: " + 0 + " " + j + " " + 0 + " " + " " + l);
//				System.out.println("ld: \t" + String.format("%"+N+"s", Integer.toBinaryString(ld)).replace(' ', '0'));
//				System.out.println("rd: \t" + String.format("%"+N+"s", Integer.toBinaryString(rd)).replace(' ', '0'));
//				System.out.println("col: \t" + String.format("%"+N+"s", Integer.toBinaryString(col)).replace(' ', '0'));
//				System.out.println("LD: \t" + String.format("%"+N+"s", Integer.toBinaryString(LD)).replace(' ', '0'));
//				System.out.println("RD: \t" + String.format("%"+N+"s", Integer.toBinaryString(RD)).replace(' ', '0'));
//				System.out.println();
//				printboard(0, j, 0, l);
				
				counter = 0;
				sym = 8;
				kbit = (1 << (N-0-1));
				lbit = (1 << l);
				sq5(ld, rd, col, 0, l, 1, 3);
				kl = (k << 8) | l;
	
//				if(l > 3)
//					kl = ((2 << 8) | l);
//				else
//					kl = ((2 << 8) | 3);
				
				LD = (L >>> j);
				RD = (L >>> j);
				
				for(int a = 0; a < counter; a++) {
					LD_list.add(LD);
					RD_list.add(RD);
					kl_list.add(kl);
					sym_list.add(8);
					klcounter[k][l]++;
				}
			}
		}
		
//		System.out.println("Solutions: \t" + solvecounter);
		
		// calculate starting constellations for no Queens in corners
		// look above for if missing explanation
		for(int k = 1; k < halfN; k++) {						// go through first col
			for(int l = k+1; l < N-1; l++) {					// go through last col
				for(int i = k+1; i < N-1; i++) {				// go through first row
					if(i == N-1-l)								// skip if occupied
						continue;
					for(int j = N-k-2; j > 0; j--) {			// go through last row
						if(j==i || l == j)
							continue;

						if(!checkRotations(i, j, k, l)) {		// if no rotation-symmetric starting constellation already found
							startConstellations.add(toijkl(i, j, k, l));
							
							ld = (L >>> (i-1)) | (1 << (N-k));
							rd = (L >>> (i+1)) | (1 << (l-1));
							col = 1 | L | (L >>> j) | (L >>> i);
							LD = (L >>> j) | (L >>> l);
							RD = (L >>> j) | (1 << k);
							
							bits[0] = L >>> i;
							bits[N-1] = L >>> j;
							bits[k] = L;
							bits[l] = 1;

							kbit = (1 << (N-k-1));
							lbit = (1 << l);				// nach zeile start verschieben müssen wir es noch (das kopierte war für zeile 1 und ich hab es noch start-1 anch unten egschoben
							
							counter = 0;
							sym = symmetry(toijkl(i, j, k, l));
							sq5(ld, rd, col, k, l, 1, 4);
							kl = (k << 8) | l;
//							if(l <= 3)
//								kl = ((2 << 8) | 3);
//							else if(k >= 2)
//								kl = ((k << 8) | l);
//							else
//								kl = ((2 << 8) | l);
							
							RD = (L >>> j);
							LD = (L >>> j);
							
							for(int a = 0; a < counter; a++) {
								LD_list.add(LD);
								RD_list.add(RD);
								kl_list.add(kl);
								sym_list.add(symmetry(toijkl(i, j , k, l)));
								klcounter[k][l]++;
							}
						}
					}
				}
			}
		}

		sortConstellations();
		
//		System.out.println("Gefundene Startkonstellationen: " + ld_list.size());
		
		
		// java solver
//		int a = ld_list.size();
//		for(int i = 0; i < a; i++) {
//			sym = sym_list.removeFirst();
//			kl = kl_list.removeFirst();
//			LD = LD_list.removeFirst();
//			RD = RD_list.removeFirst();
//			start = start_list.removeFirst();
//			
//			k = kl >>> 8;
//			l = kl & 255;
//			ld_mem = 0;
//			rd_mem = 0;
//			
//			// wir shiften das ja in der zeile immer (im solver), aslo muss es hier einfach in der 0-ten zeile die diagonale der dame belegen EASY
//			kbit = (1 << (N-k-1));
//			lbit = (1 << l);	
//			
//			bits[k] = L;
//			bits[l] = 1;
//			bits[N-1] = RD & LD;
//
//			ld = ld_list.removeFirst();
//			rd = rd_list.removeFirst();
//			col = col_list.removeFirst();
//			
////			solver(ld, rd, col, start, i);			// ach i war ausgabe
//			 itersolver(ld, rd, col, i);
//			
//			iter++;
//		}
//		long endtime = System.currentTimeMillis();
		
//		System.out.println("Solutions: \t" + solvecounter);
//		System.out.println("IterSolutions: \t" + solvecounter2);
//		System.out.println("Time in ms: " + (-starttime + endtime));
	}
	
	void genConstellationsGpu(int N, HashSet<Integer> ijkls) {
		ld_list = new ArrayDeque<Integer>();
		rd_list = new ArrayDeque<Integer>();
		col_list = new ArrayDeque<Integer>();
		LD_list = new ArrayDeque<Integer>();
		RD_list = new ArrayDeque<Integer>();
		kl_list = new ArrayDeque<Integer>();
		sym_list = new ArrayDeque<Integer>();
		start_list = new ArrayDeque<Integer>();
		
		int ld, rd, col, kl, i, j, k, l;
		L = (1 << (N-1));
		mask = (L << 1) - 1;
		notmask = ~mask;
		kmask = mask - L;	// hält nur ganz links frei für die dame
		lmask = mask - 1;	// ganz rechts das gleiche
		bits = new int[N];
		klcounter = new int[N][N];
		
		// set N, halfN half of N rounded up, collection of startConstellations
		this.N = N;
		
		for(int ijkl : ijkls) {
			i = geti(ijkl);
			j = getj(ijkl);
			k = getk(ijkl);
			l = getl(ijkl);
			
			// 3 queens
			if(i == k) {
				ld = 0;
				rd = (L >>> 1) | (1 << (l-1));
				col = 1 | L | (L >>> j);
				LD = (L >>> j) | (L >>> l);
				RD = (L >>> j) | 1;
				
				bits[0] = L;
				bits[l] = 1;
				bits[N-1] = L >>> j;
				
				counter = 0;
				sym = 8;
				kbit = (1 << (N-0-1));
				lbit = (1 << l);
				sq5(ld, rd, col, 0, l, 1, 3);
				kl = (k << 8) | l;
				
				LD = (L >>> j);
				RD = (L >>> j);
				
				for(int a = 0; a < counter; a++) {
					LD_list.add(LD);
					RD_list.add(RD);
					kl_list.add(kl);
					sym_list.add(8);
					klcounter[k][l]++;
				}
			}
			// 4 queens
			else {
				ld = (L >>> (i-1)) | (1 << (N-k));
				rd = (L >>> (i+1)) | (1 << (l-1));
				col = 1 | L | (L >>> j) | (L >>> i);
				LD = (L >>> j) | (L >>> l);
				RD = (L >>> j) | (1 << k);
				
				bits[0] = L >>> i;
				bits[N-1] = L >>> j;
				bits[k] = L;
				bits[l] = 1;

				kbit = (1 << (N-k-1));
				lbit = (1 << l);				// nach zeile start verschieben müssen wir es noch (das kopierte war für zeile 1 und ich hab es noch start-1 anch unten egschoben
				
				counter = 0;
				sym = symmetry(toijkl(i, j, k, l));
				sq5(ld, rd, col, k, l, 1, 4);
				kl = (k << 8) | l;
				
				RD = (L >>> j);
				LD = (L >>> j);
				
				for(int a = 0; a < counter; a++) {
					LD_list.add(LD);
					RD_list.add(RD);
					kl_list.add(kl);
					sym_list.add(symmetry(toijkl(i, j , k, l)));
					klcounter[k][l]++;
				}
			}
		}
		
		sortConstellations();
	}
	
	void sortConstellations() {
		int len = ld_list.size();
		ArrayList<BoardProperties> list = new ArrayList<BoardProperties>(len);
		for(int i = 0; i < len; i++) {
			list.add(new BoardProperties(ld_list.removeFirst(), rd_list.removeFirst(), col_list.removeFirst(), start_list.removeFirst(), kl_list.removeFirst(), LD_list.removeFirst(), RD_list.removeFirst(), sym_list.removeFirst()));
		}
		Collections.sort(list, new Comparator<BoardProperties>() {
		    @Override
		    public int compare(BoardProperties o1, BoardProperties o2) {
		        if(o1.start > o2.start) {
		        	return 1;
		        } else if(o1.start < o2.start) {
		        	return -1;
		        } else {
		        	if((o1.kl >> 8) > (o2.kl >> 8)) {
		        		return 1;
		        	} else if((o1.kl >> 8) < (o2.kl >> 8)) {
		        		return -1;
		        	}
		        	return 0;
		        }
		    }
		});
		for(int i = 0; i < len; i++) {
			ld_list.add(list.get(i).ld);
			rd_list.add(list.get(i).rd);
			col_list.add(list.get(i).col);
			start_list.add(list.get(i).start);
			kl_list.add(list.get(i).kl);
			LD_list.add(list.get(i).LD);
			RD_list.add(list.get(i).RD);
			sym_list.add(list.get(i).sym);
		}
	}
	
	// true, if starting constellation rotated by any angle has already been found
	private boolean checkRotations(int i, int j, int k, int l) {
		// rot90
		if(startConstellations.contains(((N-1-k)<<24) + ((N-1-l)<<16) + (j<<8) + i)) 
			return true;

		// rot180
		if(startConstellations.contains(((N-1-j)<<24) + ((N-1-i)<<16) + ((N-1-l)<<8) + N-1-k)) 
			return true;

		// rot270
		if(startConstellations.contains((l<<24) + (k<<16) + ((N-1-i)<<8) + N-1-j)) 
			return true;

		return false;
	}
	
	// wrap i, j, k and l to one integer using bitwise movement
	private int toijkl(int i, int j, int k, int l) {
		return (i<<24) + (j<<16) + (k<<8) + l;
	}
	
	private void printboard(int i, int j, int k, int l) {
		System.out.println(String.format("%"+N+"s", Integer.toBinaryString(L >>> i)).replace(' ', '0'));
		for(int a = 1; a < N-1; a++) {
			if(a == k)
				System.out.println(String.format("%"+N+"s", Integer.toBinaryString(L)).replace(' ', '0'));
			else if(a == l)
				System.out.println(String.format("%"+N+"s", Integer.toBinaryString(1)).replace(' ', '0'));
			else
				System.out.println(String.format("%"+N+"s", Integer.toBinaryString(0)).replace(' ', '0'));
		}
		System.out.println(String.format("%"+N+"s", Integer.toBinaryString(L >>> j)).replace(' ', '0'));
		System.out.println();
	}
	
	private void printbits() {
		for(int i = 0; i < N; i++) {
			if(i == k)
				System.out.println("k " + String.format("%"+N+"s", Integer.toBinaryString(L)).replace(' ', '0'));
			else if(i == l)
				System.out.println("l " + String.format("%"+N+"s", Integer.toBinaryString(1)).replace(' ', '0'));
			else
				System.out.println(i + " " + String.format("%"+N+"s", Integer.toBinaryString(bits[i])).replace(' ', '0'));
		}
		System.out.println();
	}
	
	private void sq5(int ld, int rd, int col, int k, int l, int row, int queens) { 	// jo
		if(queens == 5) {
			ld &= ~(kbit << row);
			rd &= ~(lbit >>> row);
			col &= ~(1 | L);
			if(k < row) {
				rd |= (L >> (row-k));
				col |= L;
			}
			if(l < row) {
				ld |= (1 << (row-l));
				col |= 1;
			}
//			if(l <= row) {
//				ld |= (1 << (row-l));
//				col |= 1;
//			}
			
			ld_list.add(ld);
			rd_list.add(rd);
//			System.out.println("k = " + k + " und l = " + l + " und row = " + row);
//			System.out.println(Integer.toBinaryString(ld) + "  " + Integer.toBinaryString(ld & ~(1 << (N-k+row-1))));
//			System.out.println(Integer.toBinaryString(rd) + "  " + Integer.toBinaryString(rd & ~(1 << (l-row))));
//			System.out.println("kbit = " + Integer.toBinaryString(kbit << row) + " und lbit = " + Integer.toBinaryString(lbit >>> row));
		
			// gucken, ob hier die richtige diagonale frei gemacht wird
			
			col_list.add(col);
			start_list.add(row);
			counter++;
			return;
		}
		if(row == k || row == l) {
			sq5(ld<<1, rd>>>1, col, k, l, row+1, queens);
			return;
		}
		else {
			int free = ~(ld | rd | col | (LD >>> (N-1-row)) | (RD << (N-1-row))) & mask;
			int bit;
		
			while(free > 0) {
				bit = free & (-free);
//				bits[row] = bit;
				free -= bit;
				sq5((ld|bit) << 1, (rd|bit) >>> 1, col|bit, k, l, row+1, queens+1);
			}
		}
	}
	
	private void solver(int ld, int rd, int col, int row, int i) {
		if(row == N-1) {
			solvecounter += sym;
			return;
		}
		int klguard = ((kbit << row) ^ L) | ((lbit >>> row) ^ 1) | ((((kbit << row)&L) >>> (N-1)) * kmask) | (((lbit >>> row)&1) * lmask);	
		int free = ~(ld | rd | col | (LD >>> (N-1-row)) | (RD << (N-1-row)) | klguard) & mask;
		int bit = 0;
		
		while(free > 0) {
			bit = free & (-free);
			free -= bit;
			bits[row] = bit;
			solver((ld|bit) << 1, (rd|bit) >>> 1, col|bit, row+1, i);
		}
	}
	
	
	private void itersolver(int ld, int rd, int col, int i) {		
		int row = start;
		int klguard = ((kbit << start) ^ L) | ((lbit >>> start) ^ 1) | ((((kbit << start)&L) >>> (N-1)) * kmask) | (((lbit >>> start)&1) * lmask);
		notfree = ld | rd | col | notmask | (LD >>> (N-1-start)) | (RD << (N-1-start)) | klguard;
		int temp = bits[row] = (notfree + 1) & ~notfree;
		
		// iterative loop representing the recursive setqueen-function
		while(row >= start){
			if(temp != 0) {																	// if bit is on board
				col |= temp;															// new col
				ld_mem <<= 1;
				rd_mem >>>= 1;
				if(row == k) {
					rd_mem |= rd << 31;
				} else if(row == l) {
					ld_mem |= ld >>> 31;
				} else {
					ld_mem |= ld >>> 31;
					rd_mem |= rd << 31;
				}
//				ld_mem = (ld_mem << 1) | (ld >>> 31);									// remember the diagonals leaving the board here
//				rd_mem = (rd_mem >>> 1) | (rd << 31);
				ld = (ld | temp) << 1;													// shift diagonals to next line
				rd = (rd | temp) >>> 1;													

				row++;
				klguard = ((kbit << row) ^ L) | ((lbit >>> row) ^ 1) | ((((kbit << row)&L) >>> (N-1)) * kmask) | (((lbit >>> row)&1) * lmask);
				notfree = ld | rd | col | notmask | (LD >>> (N-1-row)) | (RD << (N-1-row)) | klguard | temp;					// calculate occupancy of next row
				bits[row] = (notfree + 1) & ~notfree;								// set new queen in next row
				temp = bits[row];	
			}
			else {
				if(row == N-1)
					solvecounter2 += sym;
				
				row--;																	// one row back
				temp = bits[row];													// this saves 2 reads from local array
				col &= ~temp;															// remove queen from col
				ld = ((ld >>> 1) | (ld_mem << 31)) & ~temp;								// shift diagonals one row up
				rd = ((rd << 1) | (rd_mem >>> 31)) & ~temp;								// if there was a diagonal leaving the board in the line before, occupy it again
				ld_mem >>= 1;															// shift those as well
				rd_mem <<= 1;

				klguard = ((kbit << row) ^ L) | ((lbit >>> row) ^ 1) | ((((kbit << row)&L) >>> (N-1)) * kmask) | (((lbit >>> row)&1) * lmask);
				notfree = ld | rd | col | notmask | (LD >>> (N-1-row)) | (RD << (N-1-row)) | klguard | temp;					// calculate occupancy of next row
				temp = (notfree + temp) & ~notfree;									// set the queen
				bits[row] = temp;												// this saves 2 reads from local array
			}
		}
	}
	
	private void printklcounter() {
		for(int i = 0; i < N; i++) {
			System.out.println();
			for(int j = 0; j < N; j++) {
				System.out.printf(String.format("%4s", klcounter[i][j]) + " ");
			}
		}
		System.out.println();
	}
	
	// how often does a found solution count for this start constellation
	private int symmetry(int ijkl) {
		if(geti(ijkl) == N-1-getj(ijkl) && getk(ijkl) == N-1-getl(ijkl))		// starting constellation symmetric by rot180?
			if(symmetry90(ijkl))		// even by rot90?
				return 2;
			else
				return 4;
		else
			return 8;					// none of the above?
	}
	
	private int geti(int ijkl) {
		return ijkl >>> 24;
	}
	private int getj(int ijkl) {
		return (ijkl >>> 16) & 255;
	}
	private int getk(int ijkl) {
		return (ijkl >>> 8) & 255;
	}
	private int getl(int ijkl) {
		return ijkl & 255;
	}
	
	// true, if starting constellation is symmetric for rot90
	private boolean symmetry90(int ijkl) {
		if(((geti(ijkl) << 24) + (getj(ijkl) << 16) + (getk(ijkl) << 8) + getl(ijkl)) == (((N-1-getk(ijkl))<<24) + ((N-1-getl(ijkl))<<16) + (getj(ijkl)<<8) + geti(ijkl)))
			return true;
		return false;
	}


	// getters
	ArrayDeque<Integer> getld_list() {
		return ld_list;
	}
	ArrayDeque<Integer> getrd_list() {
		return rd_list;
	}
	ArrayDeque<Integer> getcol_list() {
		return col_list;
	}
	ArrayDeque<Integer> getLD_list() {
		return LD_list;
	}
	ArrayDeque<Integer> getRD_list() {
		return RD_list;
	}
	ArrayDeque<Integer> getkl_list() {
		return kl_list;
	}
	ArrayDeque<Integer> getstart_list() {
		return start_list;
	}
	ArrayDeque<Integer> getsym_list() {
		return sym_list;
	}
}
