### 1. Environment
I took over all the functionalities that we put onto the agent last time.
The method names are the same ones from last time.
### 2. Position:
is a record to save the x and y coordinates in the entire program.
### 3. CellState
is an enum used in the grid to assign each cell a state. Depending on the execution of the algorithm, it could be a bool if we only have walls
and free spaces. The question is: do we need an extra target state? Are we going to backtrack and need to know which cells we have already visited?
Maybe an extra value that we assign to each cell from the heuristic?
### 4. Grid:
saved in the environment; the behaviour accesses it through a reference to the environment. I set it up as a Map of <Position, CellState>.

### 5. Bumpers:
the idea was to always have an object that holds the 4 fields the agent sees at that moment. I'm unsure whether that makes sense, and it also isn't consistent
with the grid setup with CellState. I left it in case you want it, and some bits are implemented with it.

### 6. GridPolicy
I didn't do anything here.

### 7. GridBehaviour
I stripped it down to its basics and moved all the functionality to the environment.