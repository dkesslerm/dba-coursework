package dba_practicas.grid.environment;

import com.sun.net.httpserver.HttpServer;
import dba_practicas.common.DBAClient;
import java.net.InetSocketAddress;
import java.util.Map;

/*
Tests for GridEnvironment against a small fake DBA Lab running on localhost,
so no token, VPN or real run is needed. Run main() and check the exit code.
*/
public class GridEnvironmentTest {

    private static int failures = 0;

    public static void main(String[] args) throws Exception {
        HttpServer lab = startFakeLab();
        try {
            DBAClient client = new DBAClient("http://localhost:" + lab.getAddress().getPort(), "fake");
            bumpersAreWrittenIntoTheKnownMap(client);
        } finally {
            lab.stop(0);
        }

        System.out.println(failures == 0 ? "ALL TESTS PASSED" : failures + " TEST(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    static void bumpersAreWrittenIntoTheKnownMap(DBAClient client) throws Exception {
        GridEnvironment env = new GridEnvironment(client);
        env.start();
        env.readPosition();
        env.readBumpers();

        // The fake lab puts the agent at (3, 3) with only north blocked
        Map<Position, Boolean> known = env.getGrid();
        check("north neighbour (3, 2) is blocked (true)", true, known.get(new Position(3, 2)));
        check("east neighbour (4, 3) is open (false)", false, known.get(new Position(4, 3)));
        check("south neighbour (3, 4) is open (false)", false, known.get(new Position(3, 4)));
        check("west neighbour (2, 3) is open (false)", false, known.get(new Position(2, 3)));
        check("target is read at start", new Position(6, 1), env.getTarget());
        env.close();
    }

    // ===== Helpers =====

    static HttpServer startFakeLab() throws Exception {
        HttpServer lab = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        lab.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath().replace("/api/v1", "");
            String control = "\"control\":{\"runId\":\"t1\",\"runStatus\":\"ACTIVE\"}";
            String body;
            if (path.equals("/challenges")) {
                body = "{\"challenges\":[{\"id\":9,\"environmentType\":\"GRID_2D\"}]}";
            } else if (path.equals("/runs")) {
                body = "{" + control + "}";
            } else if (path.endsWith("grid.position")) {
                body = "{\"value\":{\"x\":3,\"y\":3}," + control + "}";
            } else if (path.endsWith("grid.target")) {
                body = "{\"value\":{\"x\":6,\"y\":1}," + control + "}";
            } else if (path.endsWith("grid.bumpers")) {
                body = "{\"value\":{\"north\":true,\"east\":false,\"south\":false,\"west\":false}," + control + "}";
            } else if (path.endsWith("grid.energy")) {
                body = "{\"value\":{\"mode\":\"FINITE\",\"remaining\":40.0}," + control + "}";
            } else {
                body = "{}";
            }
            byte[] out = body.getBytes();
            exchange.sendResponseHeaders(200, out.length);
            exchange.getResponseBody().write(out);
            exchange.close();
        });
        lab.start();
        return lab;
    }

    static void check(String name, Object expected, Object actual) {
        if (expected.equals(actual)) {
            System.out.println("PASS " + name);
        } else {
            failures++;
            System.out.println("FAIL " + name + " : expected " + expected + ", got " + actual);
        }
    }
}
