# Design decisions: thermostat and grid

Decisions from the working sessions of 2026-11-05 and 2026-11-06, on branches `add-environment` and `grid-agent`.

Each decision is marked with its status:

- **Merged**: in `main`.
- **On `grid-agent`**: committed and pushed to `grid-agent`.
- **Uncommitted**: done on `grid-agent`, but not committed yet.

## Current status

- The thermostat structure is merged in `main` through PR #4.
- `grid-agent` contains `main`, Paul's `feature/first-grid-agent`, and the grid restructure. It is pushed to `origin/grid-agent`.
- The A* policy, the bumpers writing into the known map, `see()` and the grid tests are uncommitted.
- The grid agent reaches the target against a simulated lab, but **it has not been run against the real DBA Lab yet**.

## Decisions

### Project structure

| Decision | Why | Status |
| --- | --- | --- |
| Five roles per challenge: client, environment, agent, behaviour, policy | Each class has one responsibility, and the parts can be understood and changed separately | Merged |
| `DBAClient` only knows the API | It is shared by every challenge, so it holds nothing specific to one of them | Merged |
| The environment owns the connection and the run, and exposes sensors and actuators | The other classes do not need to know about `runId` or HTTP calls | Merged |
| The environment does not hold the policy | Decisions are not part of the environment, so it should not depend on the policy | Merged |
| The agent creates the parts and closes the run | Closing belongs in `takeDown()`, which runs however the agent ends | Merged |
| The behaviour holds `see`, `decide` and `execute` | The whole cycle can be read in one place | Merged |
| The grid uses the same layout as the thermostat: `src/dba_practicas/grid/{agents,environment,policy}` | Both parts are organised the same way, instead of a separate `src/grid/` with its own conventions | On `grid-agent` |
| `Environment` renamed to `GridEnvironment` | Matches `ThermoEnvironment` | On `grid-agent` |
| `Position` and `CellState` live in `environment/` | It avoids a folder the thermostat does not have. The policy imports them from there | On `grid-agent` |
| Inline `doDelete()` in `GridBehaviour` instead of `onEnd()` | Matches `ThermostatBehaviour` | On `grid-agent` |

### Run lifecycle

| Decision | Why | Status |
| --- | --- | --- |
| Starting the run is separate from the constructor: `new ...Environment()`, then `start()` | If a reading fails after the run has started, the agent already holds the object and can close the run. Otherwise the run stays open and can cause HTTP `409` on the next attempt | Merged for the thermostat, on `grid-agent` for the grid |
| Setup errors propagate to the agent instead of being swallowed | Otherwise the agent carries on half-initialised, with no policy | Merged for the thermostat, on `grid-agent` for the grid |
| The behaviour calls `doDelete()` when the run ends or an error occurs | Without it, `takeDown()` never runs and an open run is never closed | Merged for the thermostat, on `grid-agent` for the grid |
| `close()` only contacts the lab if the run is still active | A run the lab has already finished does not need closing | Merged for the thermostat, on `grid-agent` for the grid |
| `GridEnvironment.start()` only reads the target. `see()` reads the position, bumpers and energy at every step | The target is fixed, but the rest changes after each move, and the action response does not include it | Uncommitted |

### Grid bugs fixed

| Bug | Effect | Status |
| --- | --- | --- |
| `GridAgent` never called the environment's `setup()` | The client and policy were null, so the first cycle failed | On `grid-agent` |
| The `Bumpers` constructor took `(east, north, ...)` but was called with `(north, east, ...)` | North and east were swapped, so the agent would have seen walls on the wrong side. A collision ends the run. `Bumpers` has since been removed | On `grid-agent` |

### Branching

| Decision | Why | Status |
| --- | --- | --- |
| `grid-agent` is based on `origin/main`, not on `feature/first-grid-agent` alone | It needs both the thermostat restructure and Paul's grid code | On `grid-agent` |
| `grid-agent` tracks `origin/grid-agent`, never `main` | It cannot push to or pull from `main` by accident | On `grid-agent` |

### A* design

| Decision | Why | Status |
| --- | --- | --- |
| `CellState` is `enum { FREE, BLOCKED }` | A cell missing from the map is unknown. The target is already in `targetPos`. A* does not need `VISITED` | Uncommitted |
| Do not store `x`, `y`, directions, `g` or `h` in `CellState` | `x` and `y` are the map key. Directions are the neighbours' states. `g` belongs to a single search. `h` can be calculated when needed | Uncommitted |
| `Map<Position, CellState>` in `GridEnvironment` is the known map | It is the graph A* searches. Textbook A* receives the graph as an input, but here the agent has to build it from its bumper readings | Uncommitted |
| Open and closed exist as well as the map | They are A*'s working memory and start empty on every search, so only the map remembers obstacles between searches | Uncommitted |
| Open is a `PriorityQueue<Node>` ordered by `f`, ties broken by the lower `h`. Closed is a `Set<Position>` | Fast access to the lowest `f` and fast membership checks. Among equal `f`, the node closest to the target is expanded first | Uncommitted |
| A cheaper path to a cell in open adds a new node instead of updating the existing one | Java's `PriorityQueue` does not reorder an element whose cost changes. Duplicates are skipped when taken from open if the position is already closed | Uncommitted |
| `Node` is a private record inside `GridPolicy`, with `position`, `g`, `h` and `parent` | Nothing outside the search needs it | Uncommitted |
| `h` is the Manhattan distance | The agent moves in four directions, one cell at a time, so it never overestimates the remaining cost | Uncommitted |
| Replan at every step and take only the first move | The map is only partly known, so a path can become invalid as obstacles are discovered | Uncommitted |
| Treat unknown cells as free when planning, but never enter `BLOCKED` cells | The agent has to plan through unexplored cells to make progress, and the bumpers are read before each move | Uncommitted |
| `Bumpers` removed, `readBumpers()` writes directly into the map | "North is blocked" just means the cell above is `BLOCKED`, so there is no need to store it twice | Uncommitted |
| `GridPolicy` keeps no state between calls: `decide(current, target, known)` | It only needs `Position` and `CellState`, so A* can be tested on fake maps with no server or JADE | Uncommitted |
| Skip negative coordinates and cap the cells expanded per search (100,000 by default) | The upper size of the map is unknown, so a walled-off target could make the search run forever | Uncommitted |
| If no path is found, `decide` throws an exception | `GridBehaviour` already catches it, prints the error and closes the run | Uncommitted |

### Testing

| Decision | Why | Status |
| --- | --- | --- |
| Tests in `test/` as plain Java programs with a `main`, instead of JUnit | No test library is in `lib/`, and the project is not set up for NetBeans tests | Uncommitted |
| `GridEnvironment` has a second constructor that takes a `DBAClient` | The test can use a fake lab on localhost instead of `.env`, so it never touches the real lab or a token | Uncommitted |

## Grid structure

```text
src/dba_practicas/grid/
├── README.md
├── agents/
│   ├── GridAgent.java
│   └── GridBehaviour.java
├── environment/
│   ├── GridEnvironment.java
│   ├── Position.java
│   └── CellState.java
└── policy/
    └── GridPolicy.java
test/dba_practicas/grid/
├── environment/
│   └── GridEnvironmentTest.java
└── policy/
    └── GridPolicyTest.java
```

| File | Contents |
| --- | --- |
| `agents/GridAgent.java` | `setup()` creates the environment and policy, calls `env.start()` and adds the behaviour. `takeDown()` closes the run |
| `agents/GridBehaviour.java` | The cycle. `see()` calls `readPosition()`, `readBumpers()` and `readEnergy()`. `decide()` calls the policy with the position, target and map. `execute()` sends the action, logs the position, energy, move and status, and returns the control part |
| `environment/GridEnvironment.java` | The run, the known map, the target and the energy. `start()`, the sensors, `sendAction()`, `close()` and getters. `readBumpers()` marks the four neighbours in the map |
| `environment/Position.java` | Record with `x`, `y` and `move()` |
| `environment/CellState.java` | `enum { FREE, BLOCKED }` |
| `policy/GridPolicy.java` | Action codes, the `Node` record, `decide()`, and the helpers `manhattan()`, `neighbours()` and `firstAction()` |
| `README.md` | Notes on the design |
| `test/.../GridPolicyTest.java` | A* on fake maps, including a walk with partial perception |
| `test/.../GridEnvironmentTest.java` | Bumpers written into the map, against a fake lab |

### How A* uses each part

| Textbook A* | In this project | Lifetime |
| --- | --- | --- |
| Graph | The known map plus the four moves | Whole run, grows as the agent explores |
| Start node | Current position | Changes each step |
| Goal node | Target position | Fixed |
| `h(n)` | Manhattan distance | Calculated when needed |
| Open set | `PriorityQueue<Node>` | One search |
| Closed set | `Set<Position>` | One search |
| `g(n)` and parent | Fields of `Node` | One search |

Coordinates: `x` is the column counted from the left and `y` is the row counted from the top, so north is `y - 1`.

## What's left to do

### Git

- [x] Commit and push the grid restructure.
- [ ] Commit and push the A* implementation and the tests.
- [ ] Update local `main`, which is behind `origin/main`.
- [ ] Delete the local `add-environment` branch. It is merged and already deleted on GitHub.

### Grid implementation

- [x] Remove `Bumpers` and write the bumpers into the map.
- [x] Simplify `CellState`.
- [x] Reduce `start()` so the cycle's `see()` reads the position, bumpers and energy.
- [x] Implement `see()` in `GridBehaviour`.
- [x] Implement A* in `GridPolicy`, including the expansion cap.
- [x] Print the remaining energy at each step.
- [x] Update `src/dba_practicas/grid/README.md`, and add the grid run command to the main `README.md`.
- [ ] Let Paul know that `Bumpers` was removed, since Paul raised the question.

### Testing

- [x] Test `GridPolicy` on fake maps: open map, obstacle in the way, wall that forces a detour, partial perception, enclosed start and walled-off target.
- [x] Test that `readBumpers()` writes into the map, against a fake lab.
- [x] Run the full agent against a simulated lab.
- [ ] Run the full agent against the real lab.
- [ ] Run the NetBeans/Ant build. So far everything has only been compiled with `javac`.

### To check against the real lab

- [ ] How much energy each move costs. The handout's guide says the budget depends on the challenge. The simulated lab used 1 per move.
- [ ] Whether the energy budget is enough for the real maps. If it is not, the policy may need to explore less.
- [ ] A suitable value for the expansion cap, based on real map sizes.

## Known limitations

- The JADE process does not exit when the agent finishes, because `doDelete()` ends only the agent and not the platform. Stop it with Ctrl+C once the result appears.
- A* with unknown cells treated as free does not guarantee the shortest real route. When the bumpers reveal a wall, the agent may have to walk back. In the simulated world below, it took 29 moves where the shortest route, knowing the whole map in advance, takes 27. Route length is not scored separately, but the agent still has to reach the target before its energy runs out.
- The energy printed in each step line is the reading taken before that move.
- `ThermoPolicy` stores `lower` and `upper` but only compares against `target`. This is valid, so it was left as it is.

## What has been tested

| Test | Result |
| --- | --- |
| `javac` on the whole project, including `test/` | Passes |
| `GridPolicyTest`: 10 checks | All pass. The test against negative coordinates was confirmed to fail when the policy's filter is removed |
| `GridEnvironmentTest`: 5 checks | All pass |
| Grid agent in JADE against a simulated 10 × 10 world with three walls, 100 energy | `SUCCEEDED` in 29 moves, no collisions |
| Same world with 8 energy | `FAILED (ENERGY_EXHAUSTED)`, the run ends cleanly |
| Thermostat against a fake lab: normal run, limits reading fails, temperature reading fails | Runs end correctly, and the run is closed after both errors |

The simulated labs are not in the repository. No grid code has been run against the real DBA Lab.
