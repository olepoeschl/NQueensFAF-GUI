# NQueensFAF
<!--img src="https://www.student.hs-mittweida.de/~opoeschl/data/queenFire_FAF.png" width="200" height="200" align="right" /-->

A really fast and highly optimized program for calculating the number of solutions of the N-Queens problem. Can be used with a gui or via command line.
<br>Built using the [NQueensFAF library](https://github.com/olepoeschl/NQueensFAF-Library).
<br>Supports CPU multithreading and GPU.

### Download
See the "Release" section for the latest stable build.
<br>There you can also find a x64 stand alone executable for Windows, generated using [jarbatexe](https://github.com/olepoeschl/jarbatexe).
<br> It's an eclipse project, so you can clone it, import it into eclipse and run it.
For lower Java JDK versions than 17, you have to edit the build path and the compiler appliance of the eclipse project first, after that it should run properly too.

# Current Times
Some new hardware enabled us to refurbish the computation times table. 
Especially the RTX 3080 shows, whats actually possible with only 16 hours for the 24 queens problem. 
We are looking forward to the distributed project and hope to solve the 26 queens problem this year with your help. 

<b>GPUs</b>
|      Board size N     |   18    |     19    |      20      |      21      |      22      |       23       |       24       |   25 |
|      :----------:     |   :-:   |    :-:    |      :-:     |      :-:     |      :-:     |       :-:      |      :-:       |  :-: |
|      RTX 3080 FE      |  0.17s  |   1.46s   |    10.75s    |     1:25m    |     11:45m   |      1:43h     |     16:08h     | not measured |
|     RTX 3060 Ti FE    |  0.19s  |   2.08s   |    16.43s    |     2:14m    |     19:20m   |      2:48h     |     26:37h     | 10d 16h |
|       GTX 1650        |  0.67s  |   5.05s   |    39.83s    |     5:30m    |     48:57m   |      7:16h     |  not measured  | not measured |

<b>CPUs</b>
|      Board size N     |        16       |     17    |     18    |     19    |      20      |      21      |      22      |
|      :----------:     |       :-:       |    :-:    |    :-:    |    :-:    |      :-:     |      :-:     |      :-:     |
|  i5 - 12600k single   |      1.12s      |   7.04s   |   49.92s  |   6:21m   |    57:47m    | not measured | not measured |
|  i5 - 12600k multi    |      0.203s     |   0.79s   |   4.91s   |   37.1s   |     4:59m    |    42:20m    |     6:09h    |
|   i5 - 9300h single   |      1.32s      |   8.95s   |   1:05m   |   8:20m   |     1:10h    | not measured | not measured |
|   i5 - 9300h multi    |      0.25s      |   1.75s   |   12.5s   |   1:35m   |    13:05m    |     1:52h    |     16:18h   |

The CPU's and the GPU's are used as is, without overclocking. Only the i5 - 9300h got slightly undervolted. 
Single stands for single core and multi for Multi-Core. 
(Attention: when testing times on GPU, your graphics card may go into another power state. To check this and avoid this, you can use a tool such as "nvidiainfo".)

# News 

We are happy to have passed a small milestone today. 
A little bit of overclocking made it possible to finally reach the previous aim of this program: to solve the 16-queens-problem single threaded under 1 second. 
At 5.3GHz, the i5-12600k was able to perform the computation in 0.992s only - even with unique solutions included. 

# General

This solution is based on two methods and one idea:

- using bits to represent the occupancy of the board; based on the <a href="http://users.rcn.com/liusomers/nqueen_demo/nqueens.html">implementation by Jeff Somers </a>
      
- calculating start constellations, in which the borders of the board are already occupied by 3 or 4 queens; based on the <a href="https://github.com/preusser/q27">implementation by the TU Dresden</a> (a very good description of this method can be found <a href="http://www.nqueens.de/sub/SearchAlgoUseSymm.en.html">here</a>)

- GPU: remember board-leaving diagonals, when going forward, so that they can be reinserted, when we go backwards. This has also been done in Ping Che Chen's implementation (https://forum.beyond3d.com/threads/n-queen-solver-for-opencl.47785/) of the N Queens Problem for GPU's and it reduces the use of memory. 

# Versions
This section shows the improvements we made from version to version related to the performance.
Unless its stated otherwise, following times are referring to *single-threaded* and the i5-9300h mentioned above.

## Distributed (yet to come):
      - we already had a working version for cpu, but due to new ideas and requirements, we started again from zero and reprogram the whole application
     
      - this is gonna take some time till a final version is released, but the strategy behind the program already exists

      - we aim to have a working version till august 2022
        
      - there will be a website with a ranking of all participants and the current progress of the project when it runs
     
We are very excited!
   
## 1.16 (latest):
      - implemented (fast!) SymSolver for finding solutions that are symmetric with respect to 90 or 180 degree rotation 
      - enable counting of unique solutions 
      - extend capabilities of the command line version, for example auto saves, unique solution counter and config file 
## 1.15:
      - migrated from LWJGL 2 to LWJGL 3 -> much less overhead when starting the GpuSolver
      - for low board sizes, noticeably faster times
## 1.14:
      - command line support
## 1.13:
      - BIG IMPROVEMENT in GPU-Solver (about 30%)
      
      - swapped j with k in GpuConstellationsGenerator (-> NQueensFAF library)
      
      - group constellations by j, putting them into the same OpenCL workgroup
      
      - reduced overhead of starting the GPU-Solver by using a better method of filling the workgroups with "pseudo" constellations
## 1.12:
      - the OpenCL workgroup size used by the GpuSolver is now editable
      
      - some small changes to the Gpu Solver with little improvement
      
      - some new Gui features
## 1.11:
      - splitted into the Gui program (this repo) and the NQueensFAF library (link above) 
      
      - the GPU solver now rounds the global work size up to the next matching number of constellations 
        and solves all constellations using GPU instead of solving remaing constellations using CPU
        (take a look at the NQueensFAF library)
        
      - code (especially of the Gui class) is much cleaner now
## 1.10:
      - included support for GPU's using OpenCL through lwjgl
      
      - insanely fast thanks to optimized parallel programming
      
      - realtime progress updates using OpenCL read operations
## 1.9:
      - REMARKABLE IMPROVEMENT!!! (~35%)

      - implemented case distinction for the different start Constellations in order to get rid of arrays
      
      - now only 0 - 3 class variables (int) per start constellation instead of int[N-3] and 5 int
      
      - currently only works for board sizes up to N=23, this will be updated soon
      
      - N = 16 in ~1.35 sec --> broke the 2-second-Barrier!
## 1.8:
      - better handling of the different start constellation cases
      
      - optimization of the recursion functions
      
      - many further little optimizations

      - N = 16 in ~2.1 sec
## 1.7:
      - many minor changes to reduce memory and cache misses
      
      - ability to save progress and continue later
      
      - use newest java jdk 15
      
      - N = 16 in ~2.5 sec
## 1.6:
      - bit representation of integers for modelling the board
      
      - rest of the program stays the same
      
      - has a gui now
      
      - N = 16 in ~ 4 sec
## 1.5:
      - better use of symmetry of solutions by using starting constellations
      
      - set Queens on the outer rows and cols
      
      - multithreading by distributing the starting positions to the threads
      
      - N = 16 in ~ 1 min     
## 1.4:
      - multithreading by setting Queen in the first row on different places
## 1.3:
      - represent the board with diagonals and cols 
      
      - N = 16 in ~ some min
## 1.2:
      - reduce to half by only going to the half of the first row
      
      - N = 16 in ~ 5 min
## 1.1 (actually 1.0): 
      - board as NxN-boolean
      
      - occupy each square individually
      
      - single threading only
      
      - N = 16 in ~ 10 minutes
      
 
# Contact
We're happy about every comment, question, idea or whatever - if you have such a thought or need help running the program, you can use the issue templates, the discussion section or reach out to us directly!
Mail: olepoeschl.developing@gmail.com
      
# Credits
<a href="http://www.lwjgl.org/"> LWJGL 3 </a> (Lightweight Java Game Library, enables the use of OpenCL in Java) <br>
Icon by Jojo
