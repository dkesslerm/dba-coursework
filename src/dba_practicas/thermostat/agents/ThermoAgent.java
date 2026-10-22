package dba_practicas.thermostat.agents;

import com.google.gson.JsonObject;
import dba_practicas.thermostat.environment.DBAClient;
import dba_practicas.thermostat.policy.ThermoPolicy;
import jade.core.Agent;

public class ThermoAgent extends Agent {

    // Connection settings
    private static final String DBA_LAB_URL = "http://orca.ugr.es:8080";
    private static final String DBA_TOKEN = "xxx";
    private static final String CHALLENGE_TYPE = "THERMOSTAT";

    // Link with the environment
    private DBAClient client;
    private String runId;

    // Decision strategy
    private ThermoPolicy policy;

    // Internal state
    private double temperature;

    @Override
    protected void setup() {
        client = new DBAClient(DBA_LAB_URL, DBA_TOKEN);
        try {
            // Finding the challenge and starting the run
            long challengeId = client.findChallengeId(CHALLENGE_TYPE);
            runId = client.startRun(challengeId);

            // Reading the fixed information once
            JsonObject limits = client.readSensor(runId, "thermostat.limits");
            double lower = limits.get("lower").getAsDouble();
            double upper = limits.get("upper").getAsDouble();
            double target = client.readSensor(runId, "thermostat.target").get("celsius").getAsDouble();
            System.out.println("Run " + runId + " | limits [" + lower + ", " + upper + "] | target " + target);

            // Creating the policy with this information
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
        temperature = client.readSensor(runId, "thermostat.temperature")
                .get("celsius").getAsDouble();
    }

    // Decision: delegated to the policy
    String decide() {
        return policy.decide(temperature);
    }

    // Actuators: send the action, return the control part
    JsonObject execute(String action) throws Exception {
        JsonObject control = client.sendAction(runId, action);
        System.out.println("temp " + temperature + " -> "
                + (action.equals(ThermoPolicy.ACTION_ON) ? "ON " : "OFF")
                + " | " + control.get("runStatus").getAsString());
        return control;
    }
}