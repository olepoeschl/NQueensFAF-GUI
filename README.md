# NQueensFAF

Gui-Program for calculating the number of solutions of the N-Queens Problem.
It's an eclipse project, you can clone it, import it into eclipse and run it with Java 15.
For lower Java versions, you have to edit the build path and the compiler appliance of the eclipse project first, after that it should run properly too.

# Contact
We're happy about every comment, question, idea or whatever - use the issue templates for bug reporting and feature suggesting or reach out to us directly!
Mail: olepoeschl.developing@gmail.com

# Version 1: 
      - board as NxN-boolean
      
      - occupy each square individually
      
      - single threading only
      
      - N=16 in ~ 10 minutes
      
      
# Version 2:
      - reduce to half by only going to the half of the first row
      
      - N = 16 in ~ 5 min
      
# Version 3:
      - represent the board with diagonals and cols 
      
      - N = 16 in ~ some min
      
# Version 4:
      - multithreading by setting Queen in the first row on different places
      
# Version 5:
      - bether use of symmetry of solutions by using starting constelaions
      
      - set Queens on the outer rows and cols
      
      - multithreading by distributing the starting positions to the threads
      
      - N=16 in ~ 1 min (single)
      
# Version 6:
      - bit representation of integers for modelling the board
      
      - rest of the program stays the same
      
      - has a gui now
      
      - N = 16 in ~ 4 sec (single)
      
# Version 7:
      - many minor changes to reduce memory and cache misses
      
      - ability to save progress and continue later
      
      - use newest java jdk 15
      
      - N= 16 in ~2.5 sec (single)
      
# Version 8 (yet to come):
      - definitely N=16 in < 1 sec (single)
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
      
