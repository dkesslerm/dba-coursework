package dba_practicas.thermostat.agents;

import com.google.gson.JsonObject;
import dba_practicas.thermostat.environment.DBAClient;
import jade.core.Agent;

public class ThermoAgent extends Agent {

    // Connection settings to DBA Lab
    private DBAClient client;
    private static final String DBA_LAB_URL = "http://orca.ugr.es:8080";
    private static final String DBA_TOKEN = "Hp23rTuDqozv25v17d7FrbMmW2L9GaSYR50b5FAzaek";
    private static final String CHALLENGE_TYPE = "THERMOSTAT";

    // Internal state (used by the behaviour)
    double lower, upper, target, temperature;
    boolean isFinished = false;

    @Override
    protected void setup() {
        client = new DBAClient(DBA_LAB_URL, DBA_TOKEN);

        try {
            // Getting the challenge id (depending on the type)
            long challengeId = client.findChallengeId(CHALLENGE_TYPE);
            System.out.println(CHALLENGE_TYPE + " id: " + challengeId);

            // Starting the run
            String runId = client.startRun(challengeId);
            System.out.println("Running : runId = " + runId);

            // Reading the sensors (no "double": we fill the fields)
            JsonObject limits = client.readSensor(runId, "thermostat.limits");
            lower = limits.get("lower").getAsDouble();
            upper = limits.get("upper").getAsDouble();
            target = client.readSensor(runId, "thermostat.target").get("celsius").getAsDouble();
            temperature = client.readSensor(runId, "thermostat.temperature").get("celsius").getAsDouble();

            System.out.println("Limits : [" + lower + ", " + upper + "]");
            System.out.println("Target : " + target);
            System.out.println("Temperature : " + temperature);

            // Testing one action ON
            String status = client.sendAction(runId, "thermostat.refrigeration.on")
                    .get("runStatus").getAsString();
            temperature = client.readSensor(runId, "thermostat.temperature").get("celsius").getAsDouble();
            System.out.println("After ON : " + temperature + " (" + status + ")");

            // Testing one action OFF
            if (status.equals("ACTIVE")) {
                status = client.sendAction(runId, "thermostat.refrigeration.off")
                        .get("runStatus").getAsString();
                temperature = client.readSensor(runId, "thermostat.temperature").get("celsius").getAsDouble();
                System.out.println("After OFF : " + temperature + " (" + status + ")");
            }

            // Closing only if the run is still active
            if (status.equals("ACTIVE")) {
                client.closeRun(runId);
                System.out.println("Run closed !");
            }
        } catch (Exception e) {
            System.out.println("Error : " + e.getMessage());
            doDelete();
        }
    }
}