package dba_practicas.thermostat.agents;

import com.google.gson.JsonObject;
import dba_practicas.common.DBAClient;
import dba_practicas.thermostat.policy.ThermoPolicy;
import jade.core.Agent;
import java.io.FileInputStream;
import java.util.Properties;

public class ThermoAgent extends Agent {
    /*
        This is the agent of our project. It sees the environment through its sensors (using the API),
        decides with its policy (strategy) and acts through its actuators.
    */

    // Challenge type
    private static final String CHALLENGE_TYPE = "THERMOSTAT";

    // Link to the environment
    private DBAClient client;
    private String runId;
    private boolean runActive = false;

    // Policies (decisions)
    private ThermoPolicy policy;

    // State
    private double temperature;

    @Override
    protected void setup() {
        // Sets up the connection and reads all the needed information, runs only once
        try {
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

            // Reading the fixed information
            JsonObject limits = client.readSensor(runId, "thermostat.limits");
            double lower = limits.get("lower").getAsDouble();
            double upper = limits.get("upper").getAsDouble();
            double target = client.readSensor(runId, "thermostat.target").get("celsius").getAsDouble();
            System.out.println("Run " + runId + " | limits [" + lower + ", " + upper + "] | target " + target);

            // Creating the policy
            policy = new ThermoPolicy(lower, upper, target);

        } catch (Exception e) {
            System.out.println("Setup error : " + e.getMessage());
            doDelete();
            return;
        }
        addBehaviour(new ThermostatBehaviour(this));
    }

    // Sensors: read the current temperature
    void see() throws Exception {
        temperature = client.readSensor(runId, "thermostat.temperature").get("celsius").getAsDouble();
    }

    // Decision: delegated to the policy
    String decide() {
        return policy.decide(temperature);
    }

    // Actuators: send the action, return the control part
    JsonObject execute(String action) throws Exception {
        JsonObject control = client.sendAction(runId, action);
        String status = control.get("runStatus").getAsString();
        runActive = status.equals("ACTIVE");
        System.out.println("temp " + temperature + " -> "
                + (action.equals(ThermoPolicy.ACTION_ON) ? "ON " : "OFF")
                + " | " + status);
        return control;
    }

    // Closing the run if the agent ends while it is still active (e.g. after an error),
    // so it does not stay open and block new runs until it expires
    @Override
    protected void takeDown() {
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
