package dba_practicas.thermostat.agents;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dba_practicas.thermostat.environment.ThermoEnvironment;
import dba_practicas.thermostat.policy.ThermoPolicy;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;

public class ThermostatBehaviour extends Behaviour {
    /*
    This behaviour runs in a loop: each cycle perceives, decides and acts
    */
    private final ThermoEnvironment env;
    private final ThermoPolicy policy;
    private double temperature;
    private boolean finished = false;

    public ThermostatBehaviour(Agent agent, ThermoEnvironment env, ThermoPolicy policy) {
        super(agent);
        this.env = env;
        this.policy = policy;
    }

    @Override
    public void action() {
        try {
            see();                                // Sensors
            String action = decide();             // Rules
            JsonObject control = execute(action); // Actuators

            // Waiting for the response (ACTIVE, SUCCEEDED, FAILED)
            String status = control.get("runStatus").getAsString();
            if (!status.equals("ACTIVE")) {
                JsonElement reason = control.get("reasonCode");
                System.out.println("End of run : " + status + " (reason : "
                        + (reason == null || reason.isJsonNull() ? "-" : reason.getAsString()) + ")");
                finished = true;
                myAgent.doDelete();
            }
        } catch (Exception e) {
            System.out.println("Error : " + e.getMessage());
            finished = true;
            myAgent.doDelete();
        }
    }

    // Sensors: read the current temperature
    private void see() throws Exception {
        temperature = env.readTemperature();
    }

    // Decision: delegated to the policy
    private String decide() {
        return policy.decide(temperature);
    }

    // Actuators: send the action, return the control part
    private JsonObject execute(String action) throws Exception {
        JsonObject control = env.sendAction(action);
        System.out.println("temp " + temperature + " -> "
                + (action.equals(ThermoPolicy.ACTION_ON) ? "ON " : "OFF")
                + " | " + control.get("runStatus").getAsString());
        return control;
    }

    @Override
    public boolean done() {
        return finished;
    }
}
