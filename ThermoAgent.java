package dba_practicas.Thermostat.Agents;

import dba_practicas.Thermostat.Environnement.DBAClient;
import jade.core.Agent;
import com.google.gson.JsonObject;

public class ThermoAgent extends Agent {

    // Connexion's settings to the Dba_Lab
    private DBAClient client;
    private static final String DBA_LAB_URL = "http://orca.ugr.es:8080";
    private static final String DBA_TOKEN = "Hp23rTuDqozv25v17d7FrbMmW2L9GaSYR50b5FAzaek";
    private static final String CHALLENGE_TYPE = "THERMOSTAT";

    @Override
    protected void setup() {
        // Connecting to client
        client = new DBAClient(DBA_LAB_URL, DBA_TOKEN);

        try {
            //Getting the challenges
            String challenges = client.getChallenges();
            System.out.println("Challenges: " + challenges);
            
            //Getting the challenge id (depending on the type)
            long challengeId = client.findChallengeId(CHALLENGE_TYPE);
            System.out.println(CHALLENGE_TYPE + " id: " + challengeId);

            //Starting the run using the challengeId
            String runId = client.startRun(challengeId);
            System.out.println("Runing : runId = " + runId);
            
            // Reading the sensors
            JsonObject limits = client.readSensor(runId, "thermostat.limits");
            double lower = limits.get("lower").getAsDouble();
            double upper = limits.get("upper").getAsDouble();

            JsonObject target = client.readSensor(runId, "thermostat.target");
            double targetTemp = target.get("celsius").getAsDouble();

            JsonObject temp = client.readSensor(runId, "thermostat.temperature");
            double temperature = temp.get("celsius").getAsDouble();

            System.out.println("Limits : [" + lower + ", " + upper + "]");
            System.out.println("Target : " + targetTemp);
            System.out.println("Temperature : " + temperature);
            
            // Testing one action ON
            String status = client.sendAction(runId, "thermostat.refrigeration.on");
            temperature = client.readSensor(runId, "thermostat.temperature").get("celsius").getAsDouble();
            System.out.println("After ON : " + temperature + " (" + status + ")");

            // Testing one action OFF
            if (status.equals("ACTIVE")) {
                status = client.sendAction(runId, "thermostat.refrigeration.off");
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
