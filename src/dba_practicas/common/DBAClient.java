package dba_practicas.common;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class DBAClient {

    private final HttpClient http;
    private final String api;
    private final String token;

    public DBAClient(String baseUrl, String token) {
        this.http = HttpClient.newHttpClient();
        this.api = baseUrl + "/api/v1";
        this.token = token;
    }

    // Sends a GET request
    private String get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(api + path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return send(request);
    }

    // Sends a POST request
    private String post(String path, String jsonBody) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(api + path))
                .header("Authorization", "Bearer " + token);
        if (jsonBody == null) {
            builder.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                   .POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        }
        return send(builder.build());
    }

    // Sends the request and checks the HTTP status
    private String send(HttpRequest request) throws Exception {
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            throw new Exception("HTTP error " + status + " : " + response.body());
        }
        return response.body();
    }

    // ===== Challenges =====

    // Getting all the challenges available
    public String getChallenges() throws Exception {
        return get("/challenges");
    }

    // Finding the id of the challenge of the given type
    public long findChallengeId(String environmentType) throws Exception {
        String json = getChallenges();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();

        for (JsonElement element : root.getAsJsonArray("challenges")) {
            JsonObject challenge = element.getAsJsonObject();
            if (challenge.get("environmentType").getAsString().equals(environmentType)) {
                return challenge.get("id").getAsLong();
            }
        }
        throw new Exception("No challenge of type " + environmentType);
    }

    // ===== Runs =====

    // Starting a run and returning its runId
    public String startRun(long challengeId) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("challengeId", challengeId);
        String json = post("/runs", body.toString());
        System.out.println("Start response : " + json);

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return root.getAsJsonObject("control").get("runId").getAsString();
    }

    // Closing an active run
    public void closeRun(String runId) throws Exception {
        post("/runs/" + runId + "/close", null);
    }

    // ===== Perceptions =====

    // Reading one sensor and returning its value
    public JsonObject readSensor(String runId, String sensorId) throws Exception {
        String json = post("/runs/" + runId + "/perceptions/" + sensorId, null);
        System.out.println("Sensor " + sensorId + " : " + json);

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return root.getAsJsonObject("value");
    }

    // ===== Actions =====

    // Sending an action and returning the control part of the response
    public JsonObject sendAction(String runId, String actionCode) throws Exception {
        JsonObject body = new JsonObject();
        body.addProperty("actionCode", actionCode);
        String json = post("/runs/" + runId + "/actions", body.toString());

        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        if (!root.get("actionStatus").getAsString().equals("APPLIED")) {
            System.out.println("Action rejected : " + root.get("reasonCode"));
        }
        return root.getAsJsonObject("control");
    }
}
