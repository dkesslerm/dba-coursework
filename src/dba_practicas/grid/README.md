# Grid agent

The agent moves through a 2D grid it does not know in advance. At every step it reads its sensors, adds what it sees to its known map, plans a path with A* and takes the first move of that path.

Coordinates follow DBA Lab: `x` is the column counted from the left and `y` is the row counted from the top, so north is `y - 1`.

### 1. GridEnvironment
Owns the connection and the run, and exposes the sensors (`readPosition`, `readBumpers`, `readEnergy`) and the actuator (`sendAction`), the same way `ThermoEnvironment` does. `start()` only reads the target, since it never changes. The challenge to play can be chosen with `DBA_CHALLENGE_ID` in `.env`.

### 2. Position
A record to save the x and y coordinates in the entire program. It is the key of the known map.

### 3. Known map
A `Map<Position, Boolean>` in the environment: `true` is blocked, `false` is free, and a cell that is not in the map is unknown. `readBumpers()` writes the four neighbours of the current position into it, so it grows as the agent explores. It is the only thing A* remembers between steps.

### 4. GridPolicy
A* from the current position to the target over the known map:
- Unknown cells are assumed free and blocked cells are never entered.
- `h` is the Manhattan distance, and every move costs 1. Each search node (`Node`) keeps its position, `g`, `h` and parent.
- Open is a priority queue ordered by `f = g + h` (ties broken by `g`), and closed is a set of positions. Both are created again at every call.
- A cell can be added to open more than once, because Java's `PriorityQueue` does not reorder an element whose cost changes. When a position comes out of open and is already closed, it is skipped.
- When the target comes out of open, the parents are followed back to the start and the first move is returned. If open runs out first, there is no path: `decide` throws an exception and the agent closes the run.

### 5. GridAgent and GridBehaviour
The agent creates the environment and the policy, and closes the run when it ends. The behaviour runs the see → decide → execute cycle: `see` reads the position, then the bumpers (which need the position), then the energy.
