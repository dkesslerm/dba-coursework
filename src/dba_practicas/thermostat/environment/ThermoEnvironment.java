package dba_practicas.thermostat.environment;

import com.google.gson.JsonObject;
import dba_practicas.common.DBAClient;
import java.io.FileInputStream;
import java.util.Properties;

public class ThermoEnvironment {
    /*
    The agent's view of one thermostat run: it owns the connection and the run,
    and exposes the sensors and actuators without any decision logic
    */

    private static final String CHALLENGE_TYPE = "THERMOSTAT";

    // Link to the environment
    private final DBAClient client;
    private String runId;
    private boolean runActive = false;

    // Fixed information of the run
    private double lower;
    private double upper;
    private double target;

    // Loads the secrets and creates the client, no run is started yet
    public ThermoEnvironment() throws Exception {
        Properties secrets = new Properties();
        try (FileInputStream in = new FileInputStream(".env")) {
            secrets.load(in);
        }
        client = new DBAClient(secrets.getProperty("DBA_LAB_URL"),
                               secrets.getProperty("DBA_TOKEN"));
    }

    // Starts the run and reads the fixed information, runs only once.
    // Separated from the constructor so that the agent already holds this object
    // and can close the run if one of the readings fails
    public void start() throws Exception {
        long challengeId = client.findChallengeId(CHALLENGE_TYPE);
        runId = client.startRun(challengeId);
        runActive = true;

        JsonObject limits = client.readSensor(runId, "thermostat.limits");
        lower = limits.get("lower").getAsDouble();
        upper = limits.get("upper").getAsDouble();
        target = client.readSensor(runId, "thermostat.target").get("celsius").getAsDouble();
        System.out.println("Run " + runId + " | limits [" + lower + ", " + upper + "] | target " + target);
    }

    public double getLower() {
        return lower;
    }

    public double getUpper() {
        return upper;
    }

    public double getTarget() {
        return target;
    }

    public boolean isRunActive() {
        return runActive;
    }

    // Sensor: reads the current temperature
    public double readTemperature() throws Exception {
        return client.readSensor(runId, "thermostat.temperature").get("celsius").getAsDouble();
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
