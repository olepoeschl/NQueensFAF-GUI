# NQueensFAF
<!--img src="https://www.student.hs-mittweida.de/~opoeschl/data/queenFire_FAF.png" width="200" height="200" align="right" /-->

A really fast and highly optimized Gui-Program for calculating the number of solutions of the N-Queens Problem.
<br>Built using the [NQueensFAF library](https://github.com/olepoeschl/NQueensFAF-Library).
<br>Supports CPU multithreading and GPU.

### Download
See the "Release" section for the latest stable build.
<br> It's an eclipse project, so you can clone it, import it into eclipse and run it.
For lower Java JDK versions than 17, you have to edit the build path and the compiler appliance of the eclipse project first, after that it should run properly too.

# Current Times

|      Board size N     |        16       |     17    |     18    |     19    |      20      |      21      |      22      |       23       |
|      :----------:     |       :-:       |    :-:    |    :-:    |    :-:    |      :-:     |      :-:     |      :-:     |       :-:      |
|  __single-threaded__  |      1.32s      |   8.95s   |   1:05m   |   8:20m   |     1:10h    | not measured | not measured |  not measured  |
|   __multi-threaded__  |      0.25s      |   1.75s   |   12.5s   |   1:35m   |    13:05m    |     1:52h    |     16:18h   |  not measured  |
|        __GPU__        |      0.03s      |   0.18s   |   1.05s   |   7.35s   |      56s     |     7:50m    |     1:07h    |     10:25h     |

CPU: *i5-9300h @4GHz undervolted (8 logical cores)* <br>
GPU: GTX-1650 (Laptop) <br>
(Attention: when testing times on GPU, your graphics card may go into another power state. To avoid this, you can use a tool such as "nvidiainfo". Also, for some nvidia cards there is a big startup time for the kernel, depending also on the boardsize N. )

# General
This solution is based on two methods:

- using bits to represent the occupancy of the board; based on the <a href="http://users.rcn.com/liusomers/nqueen_demo/nqueens.html">implementation by Jeff Somers </a>
      
- calculating start constellations, in which the borders of the board are already occupied by 3 or 4 queens; based on the <a href="https://github.com/preusser/q27">implementation by the TU Dresden</a> (a very good description of this method can be found <a href="http://www.nqueens.de/sub/SearchAlgoUseSymm.en.html">here</a>)

The program copies the needed lwjgl-binaries to the temp-folder of the system and tries to delete it later when the program is done. This function is done using self-deleting scripts and is successsfully tested for windows and linux. If you recognize that the temporary folder (named "NQueensFaf*") is still existing later than 10 seconds after the program is closed (not crashed!), feel free to open an issue. If the program crashes, it's obviously not able to delete the folder. However, this is not a problem because the program deletes not only one, but all folders in your temp-directory whose names start with "NQueensFaf".

# Versions
unless its stated otherwise, following times are referring to *single-threaded* and the CPU mentioned above

## Distributed (yet to come):
      - test using board size 23 was successfull!
      
      - currently: including GPU computation into the client

      - thus revise the distribution of workloads (computational power varies a lot between different CPU's and GPU's)
 
      - due to new ideas and requirements, we started again from zero and reprogram the whole application; 
        this is gonna take some time till a final version is released, but the strategy behind the program already exists
        
      - there will be a website with a ranking of all participants and the current progress of the project when it runs
     
We are very excited!

## Version X (yet to come):
      - definitely N = 16 in < 1 sec
       
## Version 11 (latest):
      - splitted into the Gui program (this) and the NQueensFAF library (link above) 
      - the GPU solver now rounds the global work size up to the next matching number of constellations 
        and solves all constellations using GPU instead of solving remaing constellations using CPU
        (take a look at the NQueensFAF library)
      - code (especially of the Gui class) is much cleaner now
## Version 10:
      - included support for GPU's using OpenCL through lwjgl
      
      - insanely fast thanks to optimized parallel programming
      
      - realtime progress updates using OpenCL read operations
## Version 9:
      - REMARKABLE IMPROVEMENT!!! (~35%)

      - implemented case distinction for the different start Constellations in order to get rid of arrays
      
      - now only 0 - 3 class variables (int) per start constellation instead of int[N-3] and 5 int
      
      - currently only works for board sizes up to N=23, this will be updated soon
      
      - N = 16 in ~1.35 sec --> broke the 2-second-Barrier!
## Version 8:
      - better handling of the different start constellation cases
      
      - optimization of the recursion functions
      
      - many further little optimizations

      - N = 16 in ~2.1 sec
## Version 7:
      - many minor changes to reduce memory and cache misses
      
      - ability to save progress and continue later
      
      - use newest java jdk 15
      
      - N = 16 in ~2.5 sec
## Version 6:
      - bit representation of integers for modelling the board
      
      - rest of the program stays the same
      
      - has a gui now
      
      - N = 16 in ~ 4 sec
## Version 5:
      - better use of symmetry of solutions by using starting constellations
      
      - set Queens on the outer rows and cols
      
      - multithreading by distributing the starting positions to the threads
      
      - N = 16 in ~ 1 min     
## Version 4:
      - multithreading by setting Queen in the first row on different places
## Version 3:
      - represent the board with diagonals and cols 
      
      - N = 16 in ~ some min
## Version 2:
      - reduce to half by only going to the half of the first row
      
      - N = 16 in ~ 5 min
## Version 1: 
      - board as NxN-boolean
      
      - occupy each square individually
      
      - single threading only
      
      - N = 16 in ~ 10 minutes
      
 
# Contact
We're happy about every comment, question, idea or whatever - if you have such a thought or need help running the program, you can use the issue templates, the discussion section or reach out to us directly!
Mail: olepoeschl.developing@gmail.com
      
# Credits
<a href="http://legacy.lwjgl.org/"> LWJGL 2.x library </a> (Lightweight Java Game Library, enables the use of OpenCL in Java) <br>
Icon by Jojo
