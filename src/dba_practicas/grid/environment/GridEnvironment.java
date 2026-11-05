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
    private Bumpers bumpers;
    private final Map<Position, CellState> grid = new HashMap<>();

    // print in the beginning and then remainingEnergy after each move
    private String energyMode;
    private double remainingEnergy;

    // Loads the secrets and creates the client, no run is started yet
    public GridEnvironment() throws Exception {
        Properties secrets = new Properties();
        try (FileInputStream in = new FileInputStream(".env")) {
            secrets.load(in);
        }
        client = new DBAClient(secrets.getProperty("DBA_LAB_URL"),
                               secrets.getProperty("DBA_TOKEN"));
    }

    // Starts the run and reads the initial information, runs only once.
    // Separated from the constructor so that the agent already holds this object
    // and can close the run if one of the readings fails
    public void start() throws Exception {
        long challengeId = client.findChallengeId(CHALLENGE_TYPE);
        runId = client.startRun(challengeId);
        runActive = true;

        readPosition();
        System.out.println("Run " + runId + " | Current Position " + currentPosition.x() + ", " + currentPosition.y());

        JsonObject tPosition = client.readSensor(runId, "grid.target");
        targetPos = new Position(tPosition.get("x").getAsInt(), tPosition.get("y").getAsInt());
        System.out.println(" | Target Position [" + targetPos.x() + ", " + targetPos.y() + "] "); 

        // TODO do i need the bumper class? could also just save current vision in grid and work with that? maybe bumper makes it easier for policy later?
        readBumpers();

        // add new INFO to grid depends on if we use bumpers or not
        //grid.put()

        grid.put(currentPosition, CellState.VISITED);
        grid.put(targetPos, CellState.TARGET);

        readEnergy();
        System.out.println(" | Energy Mode: " + energyMode + " | Energy Remaining: " + remainingEnergy);
    }

    public Position getCurrentPosition() {
        return currentPosition;
    }

    public Position getTarget() {
        return targetPos;
    }

    public Bumpers getBumpers() {
        return bumpers;
    }

    public Map<Position, CellState> getGrid() {
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

    // Sensor: reads which adjacent cells are blocked
    public void readBumpers() throws Exception {
        JsonObject bumper = client.readSensor(runId, "grid.bumpers");
        bumpers = new Bumpers(bumper.get("north").getAsBoolean(),
                bumper.get("east").getAsBoolean(),
                bumper.get("south").getAsBoolean(),
                bumper.get("west").getAsBoolean());
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
