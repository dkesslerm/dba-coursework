package dba_practicas.grid.agents;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dba_practicas.grid.environment.GridEnvironment;
import dba_practicas.grid.policy.GridPolicy;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;

public class GridBehaviour extends Behaviour {
    /*
    This behaviour runs in a loop: each cycle perceives, decides and acts
    */
    private final GridEnvironment env;
    private final GridPolicy policy;
    private boolean finished = false;

    public GridBehaviour(Agent agent, GridEnvironment env, GridPolicy policy) {
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

            // Waiting for the response (ACTIVE or a terminal status)
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

    //TODO
    //didnt write see bc would change completely if Bumpers used or not
    private void see() {
        
    }

    // Decision: delegated to the policy
    private String decide() {
        return policy.decide();
    }

    // Actuators: send the action, return the control part
    private JsonObject execute(String action) throws Exception {
        JsonObject control = env.sendAction(action);

        String direction = switch (action) {
            case GridPolicy.ACTION_NORTH -> "NORTH";
            case GridPolicy.ACTION_EAST -> "EAST";
            case GridPolicy.ACTION_SOUTH -> "SOUTH";
            case GridPolicy.ACTION_WEST -> "WEST";
            default -> "unknown: " + action;
        };
        System.out.println("Position: " + env.getCurrentPosition() + " -> " + direction
                + " | " + control.get("runStatus").getAsString());
        return control;
    }

    @Override
    public boolean done() {
        return finished;
    }
}
