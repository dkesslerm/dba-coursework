# Design decisions: thermostat and grid

Decisions from the working session of 2026-11-05, on branches `add-environment` and `grid-agent`.

Each decision is marked with its status:

- **Merged**: in `main`.
- **Uncommitted**: done on `grid-agent`, but not committed or pushed yet.
- **Planned**: agreed as the direction, or proposed, but not implemented.

## Current status

- The thermostat structure is merged in `main` through PR #4.
- `grid-agent` contains `main` plus Paul's `feature/first-grid-agent` (merge commit `245b4ff`). That commit exists only locally.
- The grid restructure is uncommitted on `grid-agent`.
- The grid agent runs its cycle but **cannot solve a challenge yet**: `see()` is empty and `GridPolicy.decide()` returns the placeholder `"no lo sé"`.
- **Do not run the grid agent against the real lab until the policy is written.** It would send an invalid action, and if the lab rejects it while keeping the run `ACTIVE`, the agent could keep sending it in a loop.

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
| The grid uses the same layout as the thermostat: `src/dba_practicas/grid/{agents,environment,policy}` | Both parts are organised the same way, instead of a separate `src/grid/` with its own conventions | Uncommitted |
| `Environment` renamed to `GridEnvironment` | Matches `ThermoEnvironment` | Uncommitted |
| `Position`, `CellState` and `Bumpers` live in `environment/` | It avoids a folder the thermostat does not have. The policy can import them from there | Uncommitted |
| Inline `doDelete()` in `GridBehaviour` instead of `onEnd()` | Matches `ThermostatBehaviour` | Uncommitted |

### Run lifecycle

| Decision | Why | Status |
| --- | --- | --- |
| Starting the run is separate from the constructor: `new ...Environment()`, then `start()` | If a reading fails after the run has started, the agent already holds the object and can close the run. Otherwise the run stays open and can cause HTTP `409` on the next attempt | Merged for the thermostat, uncommitted for the grid |
| Setup errors propagate to the agent instead of being swallowed | Otherwise the agent carries on half-initialised, with no policy | Merged for the thermostat, uncommitted for the grid |
| The behaviour calls `doDelete()` when the run ends or an error occurs | Without it, `takeDown()` never runs and an open run is never closed | Merged for the thermostat, uncommitted for the grid |
| `close()` only contacts the lab if the run is still active | A run the lab has already finished does not need closing | Merged for the thermostat, uncommitted for the grid |

### Grid bugs fixed

| Bug | Effect | Status |
| --- | --- | --- |
| `GridAgent` never called the environment's `setup()` | The client and policy were null, so the first cycle failed | Uncommitted |
| The `Bumpers` constructor took `(east, north, ...)` but was called with `(north, east, ...)` | North and east were swapped, so the agent would have seen walls on the wrong side. A collision ends the run | Uncommitted |

### Branching

| Decision | Why | Status |
| --- | --- | --- |
| `grid-agent` is based on `origin/main`, not on `feature/first-grid-agent` alone | It needs both the thermostat restructure and Paul's grid code | Done locally |
| `grid-agent` has no upstream set yet | It cannot push to or pull from `main` by accident | Done locally |

### A* design

| Decision | Why | Status |
| --- | --- | --- |
| Reduce `CellState` to `enum { FREE, BLOCKED }` | A cell missing from the map is unknown. The target is already in `targetPos`. A* does not need `VISITED` | Planned |
| Do not store `x`, `y`, directions, `g` or `h` in `CellState` | `x` and `y` are the map key. Directions are the neighbours' states. `g` belongs to a single search. `h` can be calculated when needed | Planned |
| Keep `Map<Position, CellState>` as the known map | It is the graph A* searches. Textbook A* receives the graph as an input, but here the agent has to build it from its bumper readings | Planned |
| Keep open and closed as well as the map | They are A*'s working memory and start empty on every search, so only the map remembers obstacles between searches | Planned |
| Open is a `PriorityQueue<Node>` ordered by `f`, and closed is a `Set<Position>` | Standard A*, with fast access to the lowest `f` and fast membership checks | Planned |
| `Node` is a private record inside `GridPolicy`, with `position`, `g`, `h` and `parent` | Nothing outside the search needs it | Planned |
| `h` is the Manhattan distance | The agent moves in four directions, one cell at a time, so it never overestimates the remaining cost | Planned |
| Replan at every step and take only the first move | The map is only partly known, so a path can become invalid as obstacles are discovered | Planned |
| Treat unknown cells as free when planning, but never enter `BLOCKED` cells | The agent has to plan through unexplored cells to make progress, and the bumpers are read before each move | Planned |
| Remove `Bumpers` and have `readBumpers()` write directly into the map | "North is blocked" just means the cell above is `BLOCKED`, so there is no need to store it twice. **To confirm with Paul**, who raised this question | Planned |
| `GridPolicy` keeps no state between calls: `decide(current, target, known)` | It only needs `Position` and `CellState`, so A* can be tested on fake maps with no server or JADE | Planned |
| Skip negative coordinates and cap the cells expanded per search | The upper size of the map is unknown, so a walled-off target could make the search run forever | Planned |
| If no path is found, `decide` throws an exception | `GridBehaviour` already catches it, prints the error and closes the run | Planned |

## Grid structure

### Current

```text
src/dba_practicas/grid/
├── README.md
├── agents/
│   ├── GridAgent.java
│   └── GridBehaviour.java
├── environment/
│   ├── GridEnvironment.java
│   ├── Position.java
│   ├── Bumpers.java
│   └── CellState.java
└── policy/
    └── GridPolicy.java
```

### Planned

The same, without `Bumpers.java`:

| File | Contents |
| --- | --- |
| `agents/GridAgent.java` | `setup()` creates the environment and policy, calls `env.start()` and adds the behaviour. `takeDown()` closes the run. No changes needed |
| `agents/GridBehaviour.java` | The cycle. `see()` calls `readPosition()`, `readBumpers()` and `readEnergy()`. `decide()` calls the policy with the position, target and map. `execute()` sends the action, logs it and returns the control part |
| `environment/GridEnvironment.java` | The run, the known map, the target and the energy. `start()`, the sensors, `sendAction()`, `close()` and getters. `readBumpers()` marks the four neighbours in the map |
| `environment/Position.java` | Record with `x`, `y` and `move()`. No changes needed |
| `environment/CellState.java` | `enum { FREE, BLOCKED }` |
| `policy/GridPolicy.java` | Action codes, the `Node` record, `decide()`, and the helpers `manhattan()`, `neighbours()` and `firstAction()` |
| `README.md` | Notes on the design, updated to match |

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

- [ ] Review and commit the grid restructure. The latest edits to `GridBehaviour` and `GridEnvironment` are not staged yet, so run `git add` on them first.
- [ ] Push `grid-agent` with `git push -u origin grid-agent`.
- [ ] Update local `main`, which is behind `origin/main`.
- [ ] Delete the local `add-environment` branch. It is merged and already deleted on GitHub.

### Grid implementation

- [ ] Confirm with Paul that `Bumpers` can be removed.
- [ ] Simplify `CellState` and update the code that uses `VISITED` and `TARGET` in `GridEnvironment.start()`.
- [ ] Change `readBumpers()` to write into the map, then delete `Bumpers.java` and `getBumpers()`.
- [ ] Reduce `start()` so the cycle's `see()` reads the position, bumpers and energy.
- [ ] Implement `see()` in `GridBehaviour`.
- [ ] Implement A* in `GridPolicy`, including the expansion cap.
- [ ] Print the remaining energy after each move.
- [ ] Update `src/dba_practicas/grid/README.md`, and add the grid run command to the main `README.md`.

### Testing

- [ ] Test `GridPolicy` on fake maps: open map, obstacle in the way, dead end that forces a detour, and walled-off target.
- [ ] Run the full agent against a fake lab, then against the real lab.
- [ ] Run the NetBeans/Ant build. So far everything has only been compiled with `javac`.

The fake labs used so far were temporary and are not in the repository. A repeatable test needs to be added to the project.

### To check against the real lab

- [ ] How much energy each move costs. The handout's guide says the budget depends on the challenge, and the plan assumes each step uses energy.
- [ ] How the lab responds to an invalid action.
- [ ] A suitable value for the expansion cap, based on real map sizes.

## Known limitations

- The JADE process does not exit when the agent finishes, because `doDelete()` ends only the agent and not the platform. Stop it with Ctrl+C once the result appears.
- A* with unknown cells treated as free does not guarantee the shortest real route. That is acceptable because route length is not scored separately, but the agent still has to reach the target before its energy runs out.
- `ThermoPolicy` stores `lower` and `upper` but only compares against `target`. This is valid, so it was left as it is.

## What has been tested

| Test | Result |
| --- | --- |
| `javac` on the whole project | Passes |
| Thermostat against a fake lab: normal run, limits reading fails, temperature reading fails | Runs end correctly, and the run is closed after both errors |
| Grid against a fake lab: normal run, target reading fails, action fails | Runs end correctly, and the run is closed after both errors |
| Bumpers with only north blocked | `north=true`, all others `false` |

These tests do not exercise A*, because it has not been written. No code from this session was run against the real DBA Lab during the session.
