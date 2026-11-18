package dba_practicas.grid;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/*
A fake DBA Lab that simulates one grid run on localhost, so the real GridAgent can be
run end to end without the server, a token or the VPN.

Usage: java ... dba_practicas.grid.FakeGridLab [port] [energy] [mapFile]
  port    default 8099
  energy  default 100, each move costs 1 (assumption: check against the real lab)
  mapFile text file with one row per line: '#' wall, '.' free, 'A' agent start, 'T' target.
          Without it, a built-in map is used.

The lab answers like DBA Lab: a collision or the edge of the world ends the run with FAILED,
reaching the target ends it with SUCCEEDED, running out of energy ends it with FAILED.
It serves one run, then keeps running until it is stopped with Ctrl+C.
*/
public class FakeGridLab {

    private static final String[] DEFAULT_MAP = {
        "...A......",
        "..........",
        "..........",
        "#######...",
        ".......#..",
        ".......#..",
        "....T..#..",
        ".......#..",
        "..######..",
        "..........",
    };

    private final List<String> map;
    private int x, y, targetX, targetY;
    private double energy;
    private String status = "ACTIVE";
    private String reason = null;
    private int moves = 0;

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 8099;
        double energy = args.length > 1 ? Double.parseDouble(args[1]) : 100;
        List<String> map = args.length > 2 ? Files.readAllLines(Path.of(args[2])) : List.of(DEFAULT_MAP);
        new FakeGridLab(map, energy).serve(port);
    }

    FakeGridLab(List<String> map, double energy) {
        this.map = new ArrayList<>(map);
        this.energy = energy;
        for (int row = 0; row < map.size(); row++) {
            for (int col = 0; col < map.get(row).length(); col++) {
                char c = map.get(row).charAt(col);
                if (c == 'A') { x = col; y = row; }
                if (c == 'T') { targetX = col; targetY = row; }
            }
        }
    }

    void serve(int port) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", port), 0);
        server.createContext("/", this::handle);
        server.start();
        System.out.println("Fake grid lab on http://localhost:" + port + " | start (" + x + ", " + y
                + ") | target (" + targetX + ", " + targetY + ") | energy " + energy);
        print();
    }

    private boolean blocked(int cx, int cy) {
        return cy < 0 || cy >= map.size() || cx < 0 || cx >= map.get(cy).length() || map.get(cy).charAt(cx) == '#';
    }

    private String control() {
        return "\"control\":{\"runId\":\"fake-run\",\"runStatus\":\"" + status + "\",\"reasonCode\":"
                + (reason == null ? "null" : "\"" + reason + "\"") + "}";
    }

    private synchronized void handle(HttpExchange exchange) throws java.io.IOException {
        String path = exchange.getRequestURI().getPath().replace("/api/v1", "");
        String body;
        if (path.equals("/challenges")) {
            body = "{\"challenges\":[{\"id\":1,\"environmentType\":\"GRID_2D\"}]}";
        } else if (path.equals("/runs")) {
            body = "{" + control() + "}";
        } else if (path.endsWith("grid.position")) {
            body = "{\"value\":{\"x\":" + x + ",\"y\":" + y + "}," + control() + "}";
        } else if (path.endsWith("grid.target")) {
            body = "{\"value\":{\"x\":" + targetX + ",\"y\":" + targetY + "}," + control() + "}";
        } else if (path.endsWith("grid.bumpers")) {
            body = "{\"value\":{\"north\":" + blocked(x, y - 1) + ",\"east\":" + blocked(x + 1, y)
                    + ",\"south\":" + blocked(x, y + 1) + ",\"west\":" + blocked(x - 1, y) + "}," + control() + "}";
        } else if (path.endsWith("grid.energy")) {
            body = "{\"value\":{\"mode\":\"FINITE\",\"remaining\":" + energy + "}," + control() + "}";
        } else if (path.endsWith("/actions")) {
            body = move(new String(exchange.getRequestBody().readAllBytes()));
        } else if (path.endsWith("/close")) {
            System.out.println("Run closed by the agent");
            status = "CLOSED";
            body = "{" + control() + "}";
        } else {
            body = "{}";
        }
        byte[] out = body.getBytes();
        exchange.sendResponseHeaders(200, out.length);
        exchange.getResponseBody().write(out);
        exchange.close();
    }

    private String move(String request) {
        if (!status.equals("ACTIVE")) {
            return "{\"actionStatus\":\"REJECTED\",\"reasonCode\":\"RUN_NOT_ACTIVE\"," + control() + "}";
        }
        int nx = x, ny = y;
        if (request.contains("grid.move.north")) ny--;
        else if (request.contains("grid.move.east")) nx++;
        else if (request.contains("grid.move.south")) ny++;
        else if (request.contains("grid.move.west")) nx--;
        else return "{\"actionStatus\":\"REJECTED\",\"reasonCode\":\"UNKNOWN_ACTION\"," + control() + "}";

        moves++;
        energy--;
        if (blocked(nx, ny)) {
            status = "FAILED";
            reason = "COLLISION";
        } else {
            x = nx;
            y = ny;
            if (x == targetX && y == targetY) {
                status = "SUCCEEDED";
                reason = "TARGET_REACHED";
            } else if (energy <= 0) {
                status = "FAILED";
                reason = "ENERGY_EXHAUSTED";
            }
        }
        if (!status.equals("ACTIVE")) {
            System.out.println("End of run after " + moves + " moves : " + status + " (" + reason + ")");
            print();
        }
        return "{\"actionStatus\":\"APPLIED\"," + control() + "}";
    }

    private void print() {
        for (int row = 0; row < map.size(); row++) {
            StringBuilder line = new StringBuilder(map.get(row).replace('A', '.'));
            if (row == y) line.setCharAt(x, '@');
            System.out.println("  " + line);
        }
    }
}
