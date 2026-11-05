package dba_practicas.thermostat.agents;

import dba_practicas.thermostat.environment.ThermoEnvironment;
import dba_practicas.thermostat.policy.ThermoPolicy;
import jade.core.Agent;

public class ThermoAgent extends Agent {
    /*
        This is the agent of our project. It connects the environment (sensors and actuators)
        with its policy (strategy) and starts the behaviour that runs the cycle.
    */

    private ThermoEnvironment env;

    @Override
    protected void setup() {
        ThermoPolicy policy;
        try {
            env = new ThermoEnvironment();
            env.start();
            policy = new ThermoPolicy(env.getLower(), env.getUpper(), env.getTarget());
        } catch (Exception e) {
            System.out.println("Setup error : " + e.getMessage());
            doDelete();
            return;
        }
        addBehaviour(new ThermostatBehaviour(this, env, policy));
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
