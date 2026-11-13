# Grid agent

The agent moves through a 2D grid it does not know in advance. At every step it reads its sensors, adds what it sees to its known map, plans a path with A* and takes the first move of that path.

Coordinates follow DBA Lab: `x` is the column counted from the left and `y` is the row counted from the top, so north is `y - 1`.

### 1. GridEnvironment
Owns the connection and the run, and exposes the sensors (`readPosition`, `readBumpers`, `readEnergy`) and the actuator (`sendAction`), the same way `ThermoEnvironment` does. `start()` only reads the target, since it never changes.

### 2. Position
A record to save the x and y coordinates in the entire program. It is the key of the known map.

### 3. CellState
`FREE` or `BLOCKED`. A cell that is not in the map is unknown. The target is not a state: it is kept in `GridEnvironment`, and visited cells are not needed by A*.

### 4. Known map
A `Map<Position, CellState>` in the environment. `readBumpers()` writes the four neighbours of the current position into it, so it grows as the agent explores. It is the only thing A* remembers between steps.

### 5. GridPolicy
A* from the current position to the target over the known map:
- Unknown cells are assumed free, `BLOCKED` cells are never entered, and negative coordinates are skipped.
- `h` is the Manhattan distance, and every move costs 1.
- Open is a priority queue ordered by `f = g + h`, and closed is a set of positions. Both are created again at every call.
- A cheaper path to a cell already in open adds a second node instead of changing the first one, because Java's `PriorityQueue` does not reorder an element whose cost changes. When a position comes out of open and is already closed, it is skipped.
- The search stops after 100,000 expansions, since the size of the world is unknown. If no path is found, `decide` throws an exception and the agent closes the run.

### 6. GridAgent and GridBehaviour
The agent creates the environment and the policy, and closes the run when it ends. The behaviour runs the see → decide → execute cycle: `see` reads the position, then the bumpers (which need the position), then the energy.