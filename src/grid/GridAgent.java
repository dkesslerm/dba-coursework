package grid;

import jade.core.Agent;

public class GridAgent extends Agent {


    private Environment env;

    @Override
    protected void setup() {
        env = new Environment();
        addBehaviour(new GridBehaviour(env));
    }


    @Override
    protected void takeDown() {
        env.endRun();
    }
}
