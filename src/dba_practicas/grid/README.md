### 1. GridEnvironment
Owns the connection and the run, and exposes the sensors (`readPosition`, `readBumpers`, `readEnergy`) and the actuator (`sendAction`), the same way `ThermoEnvironment` does.
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

### 7. GridAgent and GridBehaviour
The agent creates the environment and the policy, and closes the run when it ends. The behaviour runs the see → decide → execute cycle. `see` is still a TODO.
