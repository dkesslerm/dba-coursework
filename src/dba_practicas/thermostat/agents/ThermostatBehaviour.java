package dba_practicas.thermostat.agents;

import com.google.gson.JsonObject;
import jade.core.behaviours.Behaviour;

public class ThermostatBehaviour extends Behaviour {
    /*
    This class is executed in lap, the cycle does perciving, deciding and acting
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
                System.out.println("End of run : " + status + " (reason : " + control.get("reasonCode") + ")");
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