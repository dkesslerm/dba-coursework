# Testing the grid agent without DBA Lab

The grid agent can be tested on your own machine at three levels. None of them contacts DBA Lab, so you do not need a token, the UGR VPN or a free run, and you cannot leave a run open by mistake.

| Level | What it tests | File |
| --- | --- | --- |
| 1. Policy | A* on maps built in code, without any network | `test/dba_practicas/grid/policy/GridPolicyTest.java` |
| 2. Environment | That `GridEnvironment` writes the bumpers into the known map, against a small fake lab | `test/dba_practicas/grid/environment/GridEnvironmentTest.java` |
| 3. Full agent | The real `GridAgent` in JADE, against a fake lab that simulates a whole grid run | `test/dba_practicas/grid/FakeGridLab.java` |

The tests are plain Java programs with a `main`, so no test library is needed. Each check prints `PASS` or `FAIL`, and the program exits with code `1` if any check failed.

## 1. Compile

Run everything from the project root. Recompile after every change to `src/` or `test/`.

```bash
# Linux / macOS
rm -rf build/test-classes
javac -d build/test-classes -cp "lib/jade.jar:lib/gson-2.11.0.jar" $(find src test -name "*.java")
CP="$PWD/lib/jade.jar:$PWD/lib/gson-2.11.0.jar:$PWD/build/test-classes"
```

On Windows, use `;` instead of `:` in the classpath, and list the source files with `Get-ChildItem -Recurse src,test -Filter *.java` instead of `find`.

`build/` is gitignored, so nothing compiled here ends up in a commit.

## 2. Policy and environment tests

```bash
java -cp "$CP" dba_practicas.grid.policy.GridPolicyTest
java -cp "$CP" dba_practicas.grid.environment.GridEnvironmentTest
```

Both should end with `ALL TESTS PASSED`.

The policy test checks that the agent:

- moves toward the target in each of the four directions,
- goes around a known obstacle and a known wall by the shortest path,
- never plans through negative coordinates,
- reaches the target when it only learns the four cells around it at each step, like with the real bumpers, without colliding,
- throws an exception when its start position is enclosed.

There is no test for a walled-off target: every map in this practice has a solution. With a solution, A* always finds a path, because cells are only marked blocked when the bumpers report them, so the real path is never blocked in the known map.

## 3. Full agent against the fake lab

### Start the fake lab

In a first terminal:

```bash
java -cp "$CP" dba_practicas.grid.FakeGridLab 8099 100
```

The arguments are all optional:

| Argument | Default | Meaning |
| --- | --- | --- |
| Port | `8099` | Where the fake lab listens |
| Energy | `100` | Starting energy. Each move costs 1 |
| Map file | Built-in 10 × 10 map | A text file with your own map, see below |

The fake lab prints the map, with the agent shown as `@`. It serves **one run**, so restart it before each new test.

### Run the agent

The agent reads `DBA_LAB_URL` from the `.env` file in the folder it starts in. To point it at the fake lab without touching your real `.env`, run it from `build/fakelab`, which has its own `.env`.

In a second terminal, from the project root:

```bash
CP="$PWD/lib/jade.jar:$PWD/lib/gson-2.11.0.jar:$PWD/build/test-classes"
mkdir -p build/fakelab && printf 'DBA_LAB_URL=http://localhost:8099\nDBA_TOKEN=fake\n' > build/fakelab/.env
cd build/fakelab && java -cp "$CP" jade.Boot -local-host localhost -port 1299 -nomtp \
     -agents grid:dba_practicas.grid.agents.GridAgent
```

You only need the `mkdir` and `printf` line the first time. On Windows, create `build\fakelab\.env` by hand with these two lines:

```properties
DBA_LAB_URL=http://localhost:8099
DBA_TOKEN=fake
```

The JADE process does not exit when the agent finishes. Stop both terminals with Ctrl+C once the result appears.

### Expected output

With the built-in map and 100 energy, the agent prints one line per move and finishes like this:

```text
Run fake-run | Target Position (4, 6)
Position: (3, 0) | energy 100.0 -> EAST | ACTIVE
...
End of run : SUCCEEDED (reason : TARGET_REACHED)
```

The fake lab prints `End of run after 29 moves : SUCCEEDED (TARGET_REACHED)` and the final map.

To check what happens when the energy runs out, start the fake lab with `20` instead of `100`. The run should end with `FAILED (reason : ENERGY_EXHAUSTED)`.

### Your own maps

Write a text file with one row per line:

| Character | Meaning |
| --- | --- |
| `#` | Wall |
| `.` | Free cell |
| `A` | Where the agent starts |
| `T` | Target |

For example, `build/fakelab/map1.txt`:

```text
A....#....
.###.#.##.
...#...#..
##.#####.#
...#.....T
```

Then start the fake lab with it:

```bash
java -cp "$CP" dba_practicas.grid.FakeGridLab 8099 100 build/fakelab/map1.txt
```

Anything outside the map counts as the edge of the world. It is reported by the bumpers and ends the run if the agent moves into it, as in DBA Lab.

## How the fake lab differs from DBA Lab

The fake lab follows the API described in `guide_grid.md`, but some details are assumptions, so a run that succeeds here can still fail on the real lab:

- **Energy:** every move costs exactly 1. The real cost and budget depend on each challenge's configuration.
- **Invalid actions:** an unknown action code is answered with `REJECTED`, and the run stays `ACTIVE`. The real lab may behave differently.
- **Reason codes:** `TARGET_REACHED`, `COLLISION` and `ENERGY_EXHAUSTED` are made up for the fake lab. The real lab's codes may have other names.
- **Runs:** there is only one challenge and one run, and no limit of one active run per student.

After the tests pass, the last step is still a run against the real DBA Lab.
