package dba_practicas.thermostat.agents;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jade.core.behaviours.Behaviour;

public class ThermostatBehaviour extends Behaviour {
    /*
    This behaviour runs in a loop: each cycle perceives, decides and acts
    */
    private final ThermoAgent agent;
    private boolean finished = false;

    public ThermostatBehaviour(ThermoAgent agent) {
        super(agent);
        this.agent = agent;
    }

    @Override
    public void action() {
        try {
            agent.see();                               // Sensors
            String action = agent.decide();            // Rules
            JsonObject control = agent.execute(action); // Actuators

            // Waiting for the response (ACTIVE, SUCCEEDED, FAILED)
            String status = control.get("runStatus").getAsString();
            if (!status.equals("ACTIVE")) {
                JsonElement reason = control.get("reasonCode");
                System.out.println("End of run : " + status + " (reason : "
                        + (reason == null || reason.isJsonNull() ? "-" : reason.getAsString()) + ")");
                finished = true;
                agent.doDelete();
            }
        } catch (Exception e) {
            System.out.println("Error : " + e.getMessage());
            finished = true;
            agent.doDelete();
        }
    }

    @Override
    public boolean done() {
        return finished;
    }
}
