# DBA Coursework — Practice 2

JADE agents that interact with **DBA Lab** (UGR) to solve the Practice 2 challenges:

- **Thermostat**: keep a refrigerated chamber within its permitted temperature interval until the lab reports the run as finished.
- **Grid**: 2D navigation towards a target, avoiding obstacles and managing energy. *(in progress)*

## Project structure

```
src/dba_practicas/
├── common/
│   └── DBAClient.java              # DBA Lab REST API client, shared by every challenge
└── thermostat/
    ├── agents/
    │   ├── ThermoAgent.java        # Agent: setup, sensors (see), actuators (execute)
    │   └── ThermostatBehaviour.java  # Perception → decision → action cycle
    └── policy/
        └── ThermoPolicy.java       # Decision rule (refrigeration on / off)
```

The responsibilities are split so that the parts can be reused for Grid:
`DBAClient` only knows the API, the agent only knows its sensors and actuators, the behaviour only runs the cycle, and the policy only decides.

## Setup

### 1. Requirements
- **JDK 21 or later**
- **NetBeans** (or Ant)
- **JADE 4.6.0** (`jade.jar`)
- **Gson 2.11.0**: [gson-2.11.0.jar](https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar)
- Connection to the **UGR network or the UGR VPN** (required to reach DBA Lab)

### 2. Get the project
1. Clone the repository.
2. Copy `jade.jar` and `gson-2.11.0.jar` into the `lib/` folder (with exactly those names). The jars are gitignored; the project already points to them, so there is no need to add libraries in NetBeans.
3. In NetBeans: **File → Open Project** → select the repository folder.

### 3. Add your own token
1. At the root of the project, copy `.env.example` to `.env`.
2. Fill in the lab URL and **your personal DBA Lab token**:
   ```
   DBA_LAB_URL=http://orca.ugr.es:8080
   DBA_TOKEN=your-token-here
   ```

> ⚠️ **Each member uses their own token. Never share or commit it.** `.env` is gitignored; never put a real token in `.env.example` or in the code.

### 4. Build and run
1. **Clean and Build** in NetBeans (or `ant clean jar`) and check for `BUILD SUCCESSFUL`.
2. From the project root (where `.env` is):

   Linux / macOS:
   ```bash
   java -cp "lib/jade.jar:lib/gson-2.11.0.jar:dist/dba-coursework-git.jar" jade.Boot \
        -local-host localhost -port 1200 \
        -agents thermo:dba_practicas.thermostat.agents.ThermoAgent
   ```

   Windows (PowerShell):
   ```powershell
   java -cp "lib\jade.jar;lib\gson-2.11.0.jar;dist\dba-coursework-git.jar" jade.Boot -local-host localhost -port 1200 -agents thermo:dba_practicas.thermostat.agents.ThermoAgent
   ```

## Troubleshooting
- **HTTP `409`**: only one active run per student. The agent closes its run when it stops; if a run was left open anyway (e.g. the process was killed), it expires 5 minutes after the last applied action.
- **`Cannot bind ... port`**: another JADE platform is still running. Close it, or change `-port`.
- **`Class ... not found`**: check the package names and do a **Clean and Build**.
- **`FileNotFoundException: .env`**: the `.env` file is missing, or the command was not run from the project root.
- **Connection error**: check the VPN.
