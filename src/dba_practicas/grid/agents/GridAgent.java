package dba_practicas.grid.agents;

import dba_practicas.grid.environment.GridEnvironment;
import dba_practicas.grid.policy.GridPolicy;
import jade.core.Agent;

public class GridAgent extends Agent {
    /*
        This is the grid agent. It connects the environment (sensors and actuators)
        with its policy (strategy) and starts the behaviour that runs the cycle.
    */

    private GridEnvironment env;

    @Override
    protected void setup() {
        GridPolicy policy;
        try {
            env = new GridEnvironment();
            env.start();
            policy = new GridPolicy();
        } catch (Exception e) {
            System.out.println("Setup error : " + e.getMessage());
            doDelete();
            return;
        }
        addBehaviour(new GridBehaviour(this, env, policy));
    }

    // Closing the run if the agent ends while it is still active (e.g. after an error),
    // so it does not stay open and block new runs until it expires
    @Override
    protected void takeDown() {
        if (env != null) {
            try {
                env.close();
            } catch (Exception e) {
                System.out.println("Could not close the run : " + e.getMessage());
            }
        }
    }
}
