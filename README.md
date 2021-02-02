# N-Queens Solver FAF
<!--img src="https://www.student.hs-mittweida.de/~opoeschl/data/queenFire_FAF.png" width="200" height="200" align="right" /-->

Gui-Program for calculating the number of solutions of the N-Queens Problem.

Download the .jar :     
&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://www.student.hs-mittweida.de/~opoeschl/executables/NQueensFaf.jar"> NQueensFaf.jar (Java 15) </a>
<br>&nbsp;&nbsp;&nbsp;&nbsp;<a href="https://www.student.hs-mittweida.de/~opoeschl/executables/NQueensFaf_Java8.jar"> NQueensFaf_Java8.jar (Java 8) </a>
<br>(if you don't already have Java, download it <a href="https://www.java.com/en/download/manual.jsp">here</a>)

It's an eclipse project, so you can clone it, import it into eclipse and run it.
For lower Java JDK versions than 15, you have to edit the build path and the compiler appliance of the eclipse project first, after that it should run properly too.



# Contact
We're happy about every comment, question, idea or whatever - if you have such a thought or need help running the program, you can use the issue templates, the discussion section or reach out to us directly!
Mail: olepoeschl.developing@gmail.com


# General
This solution is based on two methods:

- using bits to represent the occupancy of the board; based on the <a href="http://users.rcn.com/liusomers/nqueen_demo/nqueens.html">implementation by Jeff Somers </a>
      
- calculating start constellations, in which the borders of the board are already occupied by 3 or 4 queens; based on the <a href="https://tu-dresden.de">implementation by the TU Dresden</a> (a very good description of this method can be found <a href="http://www.nqueens.de/sub/SearchAlgoUseSymm.en.html">here</a>)

# Versions
----> *times* are referring to *i5-9300h @4GHz undervolted, single threaded* !

## Version X (yet to come):
      - definitely N = 16 in < 1 sec 
      
## Distributed
      - currently testing for 23 
      
      - if succesfull, we will approach boardsize 24
      
      - please contact us if you want to help and speed up the process :)
      
## Version 9:
      - REMARKABLE IMPROVEMENT!!! (~35%)

      - implemented case distinction for the different start Constellations in order to get rid of arrays
      
      - now only 0 - 3 class variables (int) per start constellation instead of int[N-3] and 5 int
      
      - currently only works for board sizes up to N=23, this will be updated soon
      
      - N = 16 in ~1.3 sec --> broke the 2-second-Barrier!
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
      
 
      
# Credits
Icon by Jojo
      
      
      
      
      
      
      
     
      
      
      
      
