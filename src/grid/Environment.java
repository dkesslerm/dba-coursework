package grid;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dba_practicas.common.DBAClient;

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class Environment {


    // Challenge type
    private static final String CHALLENGE_TYPE = "GRID_2D";

    //Link to the environment
    private DBAClient client;
    private String runId;
    private boolean runActive = false;
    private GridPolicy policy;

    //state
    private Position currentPosition;
    private Position targetPos;
    private Bumpers bumpers;
    private final Map<Position, CellState> grid = new HashMap<>();

    // print in the beginning and then remainingEnergy after each move?
    private String energyMode;
    private double remainingEnergy;

    public void setup() throws Exception {
        // Sets up the connection and reads all the needed information, runs only once

        // Loading secrets
        Properties env = new Properties();
        try (FileInputStream in = new FileInputStream(".env")) {
            env.load(in);
        }

        // Connecting to the environment
        client = new DBAClient(env.getProperty("DBA_LAB_URL"),
                env.getProperty("DBA_TOKEN"));

        // Finding challenge and starting the run
        long challengeId = client.findChallengeId(CHALLENGE_TYPE);
        runId = client.startRun(challengeId);
        runActive = true;


        //getting current Position
        JsonObject position = client.readSensor(runId, "grid.position");
        currentPosition = new Position(position.get("x").getAsInt(), position.get("y").getAsInt());
        System.out.println("Run " + runId + " | Current Position " + currentPosition.x() + ", " + currentPosition.y());


        //getting target Position
        JsonObject tPosition = client.readSensor(runId, "grid.target");
        targetPos = new Position(tPosition.get("x").getAsInt(), tPosition.get("y").getAsInt());
        System.out.println(" | Target Position [" + targetPos.x() + ", " + targetPos.y() + "] "); //print runId again?


        // TODO do i need the bumper class? could also just save current vision in grid and work with that? maybe bumper makes it easier for policy later?
        //getting bumpers and setting them in the Grid. this method could be replaced using the see method.
        JsonObject bumper = client.readSensor(runId, "grid.bumpers");
        bumpers = new Bumpers(bumper.get("north").getAsBoolean(),
                bumper.get("east").getAsBoolean(),
                bumper.get("south").getAsBoolean(),
                bumper.get("west").getAsBoolean());


        // add new INFO to grid depends on if we use bumpers or not
        //grid.put()


        //setting grid
        grid.put(currentPosition, CellState.VISITED);
        grid.put(targetPos, CellState.TARGET);

        //getting Energy Info
        JsonObject energy = client.readSensor(runId, "grid.energy");
        energyMode = energy.get("mode").getAsString();
        remainingEnergy = energy.get("remaining").getAsDouble();
        System.out.println(" | Energy Mode: " + energyMode + " | Energy Remaining: " + remainingEnergy);


        policy = new GridPolicy();
    }

    // Decision: delegated to the policy
    String decide() {
        return policy.decide();
    }


    // Actuators: send the action, return the control part
    boolean execute(String action) throws Exception {
        JsonObject control = client.sendAction(runId, action);
        String status = control.get("runStatus").getAsString();
        runActive = status.equals("ACTIVE");

        String direction = switch (action) {
            case GridPolicy.ACTION_NORTH -> "NORTH";
            case GridPolicy.ACTION_EAST -> "EAST";
            case GridPolicy.ACTION_SOUTH -> "SOUTH";
            case GridPolicy.ACTION_WEST -> "WEST";
            default -> "unknown: " + action;
        };

        System.out.println("Position: " + currentPosition + " -> " + direction + " | " + status);

        // moved here from behaviour
        if (!runActive) {
            JsonElement reason = control.get("reasonCode");
            System.out.println("End of run : " + status + " (reason : "
                    + (reason == null || reason.isJsonNull() ? "-" : reason.getAsString()) + ")");
        }
        return runActive;
    }

    //TODO
    //didnt write see bc would change completely if Bumpers used or not
    void see() {
    }

    void endRun(){
        if (runActive) {
            try {
                client.closeRun(runId);
                System.out.println("Run " + runId + " closed");
            } catch (Exception e) {
                System.out.println("Could not close run " + runId + " : " + e.getMessage());
            }
        }
    }

}

