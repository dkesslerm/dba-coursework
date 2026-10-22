package dba_practicas.thermostat.agents;

import jade.core.behaviours.Behaviour;

public class ThermostatBehaviour extends Behaviour {

    private ThermoAgent agent;          
    private boolean isFinished = false; 

    public ThermostatBehaviour(ThermoAgent agent) {
        this.agent = agent;
    }

    @Override
    public void action() {
        // Get information
        double lower = agent.lower;
        double higher = agent.upper;
        double temp = agent.temperature;
        double target = agent.target;
        isFinished = agent.isFinished;

        if (lower <= temp && temp <= higher) {
            // we are good
        } else if (temp < lower) {
            // too cold -> turn OFF
        } else {
            // too warm -> turn ON
        }
    }

    @Override
    public boolean done() {
        return isFinished;
    }
}