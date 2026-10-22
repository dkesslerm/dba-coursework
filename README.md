## Setup (for group members)

### 1. Requirements
- **Java 11 or later** (the project uses JDK 25)
- **NetBeans**
- **JADE 4.6.0** (`jade.jar`)
- **Gson 2.11.0**: [download gson-2.11.0.jar](https://repo1.maven.org/maven2/com/google/code/gson/gson/2.11.0/gson-2.11.0.jar)
- Connection to the **UGR network or the UGR VPN** (required to reach DBA Lab)

### 2. Get the project
1. Clone the repository.
2. In NetBeans: **File → Open Project** → select the `dba-coursework-git` folder.

### 3. Add the libraries
The library paths are specific to each computer, so you must add them yourself:
- Right-click **Libraries → Add JAR/Folder** → add `jade.jar` and `gson-2.11.0.jar`.
- If NetBeans shows broken references, remove them first (right-click → **Remove**).

### 4. Add your own token
In `ThermoAgent.java`, replace `"XXXX"` with **your personal DBA Lab token**:
```java
private static final String DBA_TOKEN = "XXXX";
```
⚠️ **Each member uses their own token. Never commit or share it.** Put `"XXXX"` back before every commit.

### 5. Build and run
1. In NetBeans: **Clean and Build** (check for `BUILD SUCCESSFUL`).
2. From the project folder, run (adapt the paths to your computer):
```powershell
java -cp "C:\path\to\jade.jar;C:\path\to\gson-2.11.0.jar;dist\dba-coursework-git.jar" jade.Boot -local-host localhost -port 1200 -agents thermo:dba_practicas.thermostat.agents.ThermoAgent
```

### 6. Good to know
- **Only one active run per student.** If a run stays open, you'll get an HTTP `409` error until it expires (5 minutes).
- **`Cannot bind ... port`**: another JADE is still running. Close it, or change `-port`.
- **`Class ... not found`**: check the package names and do a **Clean and Build**.
- **Connection error**: check the VPN.