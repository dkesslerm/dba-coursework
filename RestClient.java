package dba_practicas;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/*
this class is supposed to be a Facade, that does all the Rest calls,
so that the other classes get easy methods to do them.

Not sure whether other response codes != 200 are also  correct ! have to see at testrun
 */
public class RestClient {
    private String runID;
    private final String api;
    private final String token;
    private final HttpClient http;
    //for JSON
    private final ObjectMapper mapper = new ObjectMapper();

    public RestClient() {
        this.http = HttpClient.newHttpClient();
        this.api = System.getenv("DBA_LAB_URL") + "/api/v1";
        this.token = System.getenv("DBA_TOKEN");
    }


    /*
    is called once in the beginning, fetches all challenges. Code was given in guia.dba.*.md
    only added statusCode validation
     */
    public String getChallenges() throws Exception {
        HttpRequest discovery = HttpRequest.newBuilder()
                .uri(URI.create(api + "/challenges"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> found = http.send(discovery, HttpResponse.BodyHandlers.ofString());
        if (found.statusCode() != 200) {
            throw new RuntimeException("Failed to fetch challenges. Error: " + found.statusCode() + " Response: " + found.body());
        }

        return found.body();
    }

    //starts the run and stores control.runId
    public String startRun() throws Exception {
        long challengeId = Long.parseLong(System.getenv("DBA_CHALLENGE_ID"));
        String body = "{\"challengeId\":" + challengeId + "}";
        HttpRequest start = HttpRequest.newBuilder()
                .uri(URI.create(api + "/runs"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> started = http.send(start, HttpResponse.BodyHandlers.ofString());
        if (started.statusCode() != 200) {
            throw new RuntimeException("Failed to start Run. Error: " + started.statusCode() + " Response: " + started.body());
        }

        JsonNode root = mapper.readTree(started.body());
        this.runID = root.path("control").path("runId").asText();
        return started.body();
    }


    public String perceive(String sensorId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(api + "/runs/" + runID + "/perceptions/" + sensorId))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to perceive. Error: " + response.statusCode() + " Response: " + response.body());
        }
        return response.body();
    }

    public String setRefrigerator(String actionCode) throws Exception {
        String body = "{\"actionCode\":\"" + actionCode + "\"}";
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(api + "/runs/" + runID + "/actions"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to act. Error: " + response.statusCode() + " Response: " + response.body());
        }
        return response.body();
    }

    public String closeRun() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(api + "/runs/" + runID + "/close"))
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to close run. Error: " + response.statusCode() + " Response: " + response.body());
        }
        return response.body();
    }

    /*
    this one was described briefly, its to get the state in case we lose the connection
    or if we get an error response code and dont know what has been done
    and want to check the state of run.
    returns the control info (maybe we should already unpack it here from the JSON and return it?)
     */
    // TODO: return a Control object instead of String
    public String getRun() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(api + "/runs/" + runID))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get run. Error: " + response.statusCode() + " Response: " + response.body());
        }
        return response.body();
    }

    /*
    was also described briefly at the very end of the guia-api-dba-lab-en.md
    "returns recorded state"-> doesnt say in what form, do only need actioncode or more?
     */
    //TODO return recorded state directly
    public String getLastAction() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(api + "/runs/" + runID + "/actions/last"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) {
            throw new RuntimeException("Failed to get last action. Error: " + response.statusCode() + " Response: " + response.body());
        }
        return response.body();
    }

}


