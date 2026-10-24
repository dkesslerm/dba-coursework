# DBA Coursework — Practice 2

JADE agents that interact with **DBA Lab** (UGR) to solve the Practice 2 challenges of *Agent Based Design* (2026/2027).

| Part | Goal | Status |
| --- | --- | --- |
| **Thermostat** | Keep a refrigerated chamber within its permitted temperature interval until DBA Lab reports the run as finished. | In development |
| **Grid** | Reach a target in a 2D grid using cardinal moves, avoiding obstacles, with partial perception and limited energy. | Not started |

## How it works

DBA Lab is a remote server that holds the official state of each environment. An agent starts a **run** of a challenge, then repeats a perception → decision → action cycle:

1. **See**: read the sensors it needs (e.g. `thermostat.temperature`).
2. **Decide**: a policy chooses an action from the perceptions.
3. **Act**: send the action; DBA Lab applies it and returns the run status.

The cycle stops when DBA Lab reports that the run is no longer `ACTIVE`. The official result is the one returned by the lab, never a local prediction. If the agent stops early (e.g. after an error), it closes its run so it does not stay open.

## Project structure

```text
src/dba_practicas/
├── common/
│   └── DBAClient.java                # DBA Lab REST API client, shared by every challenge
└── thermostat/
    ├── agents/
    │   ├── ThermoAgent.java          # Agent: setup, sensors (see), actuators (execute), run closing
    │   └── ThermostatBehaviour.java  # Perception → decision → action cycle
    └── policy/
        └── ThermoPolicy.java         # Decision rule (refrigeration on / off)
lib/                                  # jade.jar and gson-2.11.0.jar (not committed, see Setup)
nbproject/, build.xml, manifest.mf    # NetBeans / Ant project files
.env.example                          # Template for your personal .env
```

Each class has one responsibility so the parts can be reused for Grid: `DBAClient` only knows the API, the agent only knows its sensors and actuators, the behaviour only runs the cycle, and the policy only decides.

## Setup

### 1. Requirements

- **JDK 21 or later**
- **NetBeans** (it includes Ant, used to build the project)
- **JADE 4.6.0** (`jade.jar`)
- **Gson 2.11.0**: [gson-2.11.0.jar](https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar)
- Connection to the **UGR network or the UGR VPN** (required to reach DBA Lab)

### 2. Libraries

Copy `jade.jar` and `gson-2.11.0.jar` into the `lib/` folder, with exactly those names:

```text
lib/
├── gson-2.11.0.jar
└── jade.jar
```

The jars are gitignored. The NetBeans project is already configured to use them from `lib/`, so there is nothing to add in **Libraries**. If NetBeans shows broken references or errors such as `package jade.core does not exist`, the jars are missing from `lib/` or have a different name.

### 3. Your token

1. Copy `.env.example` to `.env` at the root of the project.
2. Replace `your-token-here` with **your personal DBA Lab token**:

   ```properties
   DBA_LAB_URL=http://orca.ugr.es:8080
   DBA_TOKEN=your-token-here
   ```

> ⚠️ **Each member uses their own token. Never share or commit it.** `.env` is gitignored; never put a real token in `.env.example` or in the code.

## Build and run

Always run from the project root: the agent reads `.env` from the working directory.

### NetBeans

1. **File → Open Project** → select the repository folder.
2. **Clean and Build**, and check for `BUILD SUCCESSFUL`. This creates `dist/dba-coursework-git.jar`.
3. Run from a terminal in the project root:

   ```bash
   # Linux / macOS
   java -cp "lib/jade.jar:lib/gson-2.11.0.jar:dist/dba-coursework-git.jar" jade.Boot \
        -local-host localhost -port 1200 \
        -agents thermo:dba_practicas.thermostat.agents.ThermoAgent
   ```

   ```powershell
   # Windows (PowerShell)
   java -cp "lib\jade.jar;lib\gson-2.11.0.jar;dist\dba-coursework-git.jar" jade.Boot -local-host localhost -port 1200 -agents thermo:dba_practicas.thermostat.agents.ThermoAgent
   ```

### Command line (no IDE)

```bash
# Linux / macOS (on Windows, use ; instead of : in the classpath)
javac -d build/classes -cp "lib/jade.jar:lib/gson-2.11.0.jar" $(find src -name "*.java")
java -cp "lib/jade.jar:lib/gson-2.11.0.jar:build/classes" jade.Boot \
     -local-host localhost -port 1200 \
     -agents thermo:dba_practicas.thermostat.agents.ThermoAgent
```

### Expected output

Besides the raw responses that `DBAClient` logs (`Start response : ...`, `Sensor ... : ...`), the agent prints the run id, the limits and the target, then one line per step, and finally the official result:

```text
Run <runId> | limits [<lower>, <upper>] | target <target>
temp <t> -> ON  | ACTIVE
temp <t> -> OFF | ACTIVE
...
End of run : <status> (reason : <reasonCode>)
```

## Troubleshooting

- **HTTP `409`**: only one active run per student. The agent closes its run when it stops; if a run was left open anyway (e.g. the process was killed), it expires 5 minutes after the last applied action.
- **`Cannot bind ... port`**: another JADE platform is still running. Close it, or change `-port`.
- **`Class ... not found`** / **`package ... does not exist`**: check that both jars are in `lib/` with the right names, then rebuild.
- **`FileNotFoundException: .env`**: the `.env` file is missing, or the agent was not started from the project root.
- **Connection error / timeout**: check the UGR VPN.
