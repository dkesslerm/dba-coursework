package dba_practicas.grid.environment;

import com.google.gson.JsonObject;
import dba_practicas.common.DBAClient;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class GridEnvironment {
    /*
    The agent's view of one grid run: it owns the connection, the run and what is
    known about the grid, and exposes the sensors and actuators without any decision logic
    */

    private static final String CHALLENGE_TYPE = "GRID_2D";

    // Link to the environment
    private final DBAClient client;
    private String runId;
    private boolean runActive = false;

    // State
    private Position currentPosition;
    private Position targetPos;
    // Known map: every cell the bumpers have reported. 1 is blocked, 0 is open
    private final Map<Position, Boolean> grid = new HashMap<>();

    // print in the beginning and then remainingEnergy after each move
    private String energyMode;
    private double remainingEnergy;

    // to change different maps in .env
    private long challengeId = -1;
    // Loads the secrets and creates the client, no run is started yet
    public GridEnvironment() throws Exception {
        Properties secrets = new Properties();
        try (FileInputStream in = new FileInputStream(".env")) {
            secrets.load(in);
            String id = secrets.getProperty("DBA_CHALLENGE_ID");
            if (id != null) challengeId = Long.parseLong(id.trim());
        }
        client = new DBAClient(secrets.getProperty("DBA_LAB_URL"),
                               secrets.getProperty("DBA_TOKEN"));
    }

    public GridEnvironment(DBAClient client) {
        this.client = client;
    }

    // Starts the run and reads the target, runs only once. The position, bumpers and
    // energy change at every step, so the behaviour reads them in each cycle (see).
    // Separated from the constructor so that the agent already holds this object
    // and can close the run if one of the readings fails
    public void start() throws Exception {
        long id = (challengeId != -1) ? challengeId : client.findChallengeId(CHALLENGE_TYPE);
        runId = client.startRun(id);
        runActive = true;

        JsonObject tPosition = client.readSensor(runId, "grid.target");
        targetPos = new Position(tPosition.get("x").getAsInt(), tPosition.get("y").getAsInt());
        System.out.println("Run " + runId + " | Target Position " + targetPos);
    }

    public Position getCurrentPosition() {
        return currentPosition;
    }

    public Position getTarget() {
        return targetPos;
    }

    public Map<Position, Boolean> getGrid() {
        return grid;
    }

    public String getEnergyMode() {
        return energyMode;
    }

    public double getRemainingEnergy() {
        return remainingEnergy;
    }

    public boolean isRunActive() {
        return runActive;
    }

    // Sensor: reads the current position
    public void readPosition() throws Exception {
        JsonObject position = client.readSensor(runId, "grid.position");
        currentPosition = new Position(position.get("x").getAsInt(), position.get("y").getAsInt());
    }

    // Sensor: reads which adjacent cells are blocked and writes them into the known map.
    // Uses the current position, so readPosition() must be called first.
    // y counts from the top, so north is y - 1
    public void readBumpers() throws Exception {
        JsonObject bumper = client.readSensor(runId, "grid.bumpers");
        grid.put(currentPosition.move(0, -1), bumper.get("north").getAsBoolean());
        grid.put(currentPosition.move(1, 0), bumper.get("east").getAsBoolean());
        grid.put(currentPosition.move(0, 1), bumper.get("south").getAsBoolean());
        grid.put(currentPosition.move(-1, 0), bumper.get("west").getAsBoolean());
    }

    // Sensor: reads the energy mode and the remaining energy
    public void readEnergy() throws Exception {
        JsonObject energy = client.readSensor(runId, "grid.energy");
        energyMode = energy.get("mode").getAsString();
        remainingEnergy = energy.get("remaining").getAsDouble();
    }

    // Actuator: sends the action and returns the control part of the response
    public JsonObject sendAction(String action) throws Exception {
        JsonObject control = client.sendAction(runId, action);
        runActive = control.get("runStatus").getAsString().equals("ACTIVE");
        return control;
    }

    // Closes the run if it is still active
    public void close() throws Exception {
        if (runActive) {
            client.closeRun(runId);
            runActive = false;
            System.out.println("Run " + runId + " closed");
        }
    }
}
