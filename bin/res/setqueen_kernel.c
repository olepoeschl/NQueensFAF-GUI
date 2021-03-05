__kernel void run(__global int *params, __global uint *result){
	const int g_id = get_global_id(0);
	const int l_id = get_local_id(0);
	
	// variables
	__const uint mask = (1 << N) - 1; 
	uint ld = params[3 * g_id];
	uint rd = params[3 * g_id + 1];
	uint col = params[3 * g_id + 2];
	local uint bits[N-PRE_ROWS+1][BLOCK_SIZE];
	uint notfree = ld | rd | col;								 	// init notfree
	bits[0][l_id] = (notfree + 1) & ~notfree;							 	// init bit
	uint ld_big = 0;
	uint rd_big = 0;
	int row = 0;
	uint solvecounter = 0;
	
	// iterative loop representing the recursive setqueen-function
	while(row >= 0){
		if((bits[row][l_id] & mask)) {							// if bit is on board
			col |= bits[row][l_id];									// new col
			ld_big = (ld_big << 1) | (ld >> 31);
			ld = (ld | bits[row][l_id]) << 1;							// new ld
			rd_big = (rd_big >> 1) | (rd << 31);
			rd = (rd | bits[row][l_id]) >> 1;							// new rd
			notfree = ld | rd | col;							// new notfree
			row++;
			bits[row][l_id] = (notfree + 1) & ~notfree;				// new bit
		}
		else {
			if(row == N-PRE_ROWS)
				solvecounter++;
				
			row--;																			// one row back
			if(row >= 0){
				col &= ~bits[row][l_id];															// remove bit from col khauifbkdlshgni
				ld = ((ld >> 1) | (ld_big << 31)) & ~bits[row][l_id];						// reset ld
				ld_big >>= 1;
				rd = ((rd << 1) | (rd_big >> 31)) & ~bits[row][l_id];						// reset rd
				rd_big <<= 1;
				notfree = ld | rd | col | bits[row][l_id];										// calculate new notfree
				bits[row][l_id] = (notfree + bits[row][l_id]) & ~notfree;								// calculate new bit
			}
		}
	}
	result[g_id] = solvecounter;
	//printf("%i \n", solvecounter);
}