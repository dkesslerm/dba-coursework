package dba_practicas.thermostat.agents;

import com.google.gson.JsonObject;
import dba_practicas.thermostat.environment.DBAClient;
import dba_practicas.thermostat.policy.ThermoPolicy;
import jade.core.Agent;
import java.io.FileInputStream;
import java.util.Properties;

public class ThermoAgent extends Agent {
    /* 
        This is the Agent of our projet. He can see the environment via some methods (using the api), and
        take actions via the Policies (strategy).
        This agent can see, decide or execute.
    */

    // Challenge type
    private static final String CHALLENGE_TYPE = "THERMOSTAT";

    // Link to the environment
    private DBAClient client;
    private String runId;

    // Policies (decisions)
    private ThermoPolicy policy;

    // State
    private double temperature;

    @Override
    protected void setup() {
        //This method setup the connection and get all the needed informations, it runs only one time
        try {
            // Loading secrets
            Properties env = new Properties();
            env.load(new FileInputStream(".env"));
            
            // Connecting to the environment
            client = new DBAClient(env.getProperty("DBA_LAB_URL"),env.getProperty("DBA_TOKEN"));

            // Finding challenge and starting the run
            long challengeId = client.findChallengeId(CHALLENGE_TYPE);
            runId = client.startRun(challengeId);

            // Reading the informations
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
        System.out.println("temp " + temperature + " -> " 
                + (action.equals(ThermoPolicy.ACTION_ON) ? "ON " : "OFF") 
                + " | " + control.get("runStatus").getAsString());
        return control;
    }
}