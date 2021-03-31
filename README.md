# NQueensFAF
<!--img src="https://www.student.hs-mittweida.de/~opoeschl/data/queenFire_FAF.png" width="200" height="200" align="right" /-->

A really fast and highly optimized Gui-Program for calculating the number of solutions of the N-Queens Problem.
<br>Supports CPU multithreading and GPU.

### Download
See the "Release" section for the latest stable build. <br>
Download the latest nightly build (may be unstable):     
&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://www.student.hs-mittweida.de/~opoeschl/executables/NQueensFaf.jar"> NQueensFaf.jar (Java 15) </a>
<br>&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://www.student.hs-mittweida.de/~opoeschl/executables/NQueensFaf_Java8.jar"> NQueensFaf_Java8.jar (Java 8) </a>
<br>(if you don't already have Java, download it <a href="https://www.java.com/en/download/manual.jsp">here</a> or use the version below)
<br>&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://www.student.hs-mittweida.de/~opoeschl/executables/NQueensFaf.exe"> (Self contained executable) NQueensFaf.exe (uses Java 15) </a>
 
<br> It's an eclipse project, so you can clone it, import it into eclipse and run it.
For lower Java JDK versions than 15, you have to edit the build path and the compiler appliance of the eclipse project first, after that it should run properly too.

# Current Times

|      Board size N     |        16       |     17    |     18    |     19    |      20      |      21      |      22      |       23       |
|      :----------:     |       :-:       |    :-:    |    :-:    |    :-:    |      :-:     |      :-:     |      :-:     |       :-:      |
|  __single-threaded__  |      1.32s      |   8.95s   |   1:05m   |   8:20m   | not measured | not measured | not measured |  not measured  |
|   __multi-threaded__  |      0.25s      |   1.75s   |   12.5s   |   1:35m   |    13:05m    |     1:52h    |     16:18h   |  not measured  |
|        __GPU__        |      0.03s      |   0.18s   |   1.05s   |   7.35s   |      56s     |     7:50m    |     1:07h    |     10:25h     |

CPU: *i5-9300h @4GHz undervolted* <br>
GPU: GTX-1650 (Laptop) <br>
(Attention: when testing times on GPU, your graphics card may go into another power state. To avoid this, you can use a tool such as "nvidiainfo". Also, for some nvidia cards there is a big startup time for the kernel, depending also on the boardsize N. )

# General
This solution is based on two methods:

- using bits to represent the occupancy of the board; based on the <a href="http://users.rcn.com/liusomers/nqueen_demo/nqueens.html">implementation by Jeff Somers </a>
      
- calculating start constellations, in which the borders of the board are already occupied by 3 or 4 queens; based on the <a href="https://tu-dresden.de">implementation by the TU Dresden</a> (a very good description of this method can be found <a href="http://www.nqueens.de/sub/SearchAlgoUseSymm.en.html">here</a>)

### Antivirus false positive
Maybe your antivirus software identifies this program as a virus. No worries! That is the case because of the way this program works. Here you can read the reason: <br>
When it starts, it copies the needed native OpenCL file to the "temp directory". That is necessary because this way the program works both times, when it is packed inside a jar and when it is executed from the IDE. But now, the copied native file has to be deleted so that your disk is not filled with trash like this. For now, this is done using the AutoIt-Script clear_temp_data.exe located in /res/bin. What it does, is, it finds all temporary created files from this program and deletes them all at once, so that when the program crashes one time, the next time it will delete the temporary file that was left over from the last time. Currently, this only works with Windows because AutoIt is used.

# Versions
unless its stated otherwise, following times are referring to *single-threaded*

## Version X (yet to come):
      - definitely N = 16 in < 1 sec
      
## Distributed
      - test using board size 23 was successfull !
      
      - currently: further developing of the client executable
      
      - once everything works fine and we got a pretty looking program, we will upload it
        and start tests for even bigger board sizes
        
      - STAY TUNED !
      
## Version 10:
      - included support for GPU's using OpenCL through lwjgl
      
      - insanely fast thanks to optimized parallel programming

      - for performance reasons, there are no progress updates in the "OpenCL" - Tab.
## Version 9:
      - REMARKABLE IMPROVEMENT!!! (~35%)

      - implemented case distinction for the different start Constellations in order to get rid of arrays
      
      - now only 0 - 3 class variables (int) per start constellation instead of int[N-3] and 5 int
      
      - now using the HPPC-library for primitive type collections (link to the repository can be found below)
      
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
<a href="https://github.com/carrotsearch/hppc">HPPC library</a> (High Performance Primitive Collections) <br>
Icon by Jojo

