package grid;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import jade.core.behaviours.Behaviour;

public class GridBehaviour extends Behaviour {

    private final Environment env;
    private boolean finished = false;

    public GridBehaviour(Environment env) {
        this.env = env;
    }

    //just copied from thermostat, env instead of agent, and moved agent.doDelete in onEnd() on bottom
    @Override
    public void action() {
        try {
            env.see();                              // Sensors
            String action = env.decide();           // Rules
            finished = !env.execute(action);        // Actuators: false = run is over
        } catch (Exception e) {
            System.out.println("Error : " + e.getMessage());
            finished = true;
        }
    }

    @Override
    public boolean done() {
        return finished;
    }

    @Override
    public int onEnd() {
        myAgent.doDelete();
        return 0;
    }
}

