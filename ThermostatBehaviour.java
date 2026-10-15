package dba_practicas;
import jade.core.behaviours.Behaviour;

public class ThermostatBehaviour extends Behaviour {

    private Agent agent;

    public ThermostatBehaviour(Agent agent) {
        this.agent = agent;
    }

    @Override
    public void action() {
        //Get informations
        double lower = agent.limits[0];
        double higher = agent.limits[1];
        double temp = agent.temperature;
        double target = agent.target;
        boolean isFinished = agent.isFinished


        if (lower <= temp && temp <= higher) {
            //we are good => done
        } else if (temp < lower) {
            //we should turn on
        } else {
            //we should turn off
        }
    }
    @Override
    public boolean done() {
        return 
    }
}