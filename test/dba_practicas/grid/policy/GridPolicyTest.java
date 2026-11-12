package dba_practicas.grid.policy;

import dba_practicas.grid.environment.Position;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

/*
Tests for GridPolicy. Plain Java, no test library needed: run main() and check the exit code.
Coordinates follow DBA Lab: x is the column from the left, y the row from the top (north is y - 1).
*/
public class GridPolicyTest {

    private static int failures = 0;

    // Every map in the practice has a solution, so there is no test for a walled-off target
    public static void main(String[] args) {
        movesTowardTargetInEachDirection();
        avoidsKnownObstacle();
        followsShortestPathAroundWall();
        neverUsesNegativeCoordinates();
        reachesTargetWhileDiscoveringObstacles();
        failsWhenStartIsEnclosed();

        System.out.println(failures == 0 ? "ALL TESTS PASSED" : failures + " TEST(S) FAILED");
        System.exit(failures == 0 ? 0 : 1);
    }

    // ===== Tests =====

    static void movesTowardTargetInEachDirection() {
        GridPolicy policy = new GridPolicy();
        Map<Position, Boolean> empty = new HashMap<>();
        Position start = new Position(5, 5);
        check("moves north toward a target above", GridPolicy.ACTION_NORTH,
                policy.decide(start, new Position(5, 2), empty));
        check("moves east toward a target to the right", GridPolicy.ACTION_EAST,
                policy.decide(start, new Position(8, 5), empty));
        check("moves south toward a target below", GridPolicy.ACTION_SOUTH,
                policy.decide(start, new Position(5, 8), empty));
        check("moves west toward a target to the left", GridPolicy.ACTION_WEST,
                policy.decide(start, new Position(2, 5), empty));
    }

    static void avoidsKnownObstacle() {
        // The cell between start and target is blocked: the shortest detour takes 4 steps
        Set<Position> walls = Set.of(new Position(3, 2));
        check("goes around a single known obstacle in 4 steps", 4,
                walk(new GridPolicy(), new Position(2, 2), new Position(4, 2), walls, true));
    }

    static void followsShortestPathAroundWall() {
        // Wall on row 3 from x = 0 to 3: the only way through is at x >= 4
        Set<Position> walls = row(3, 0, 3);
        check("follows the shortest path around a known wall", 10,
                walk(new GridPolicy(), new Position(1, 1), new Position(1, 5), walls, true));
    }

    static void neverUsesNegativeCoordinates() {
        // Wall on row 1 from x = 0 to 3. Going west through x = -1 would take 4 steps,
        // but those cells do not exist, so the only real path starts by going east (10 steps)
        Map<Position, Boolean> known = new HashMap<>();
        for (Position w : row(1, 0, 3)) {
            known.put(w, true);
        }
        check("does not plan through negative coordinates", GridPolicy.ACTION_EAST,
                new GridPolicy().decide(new Position(0, 0), new Position(0, 2), known));
    }

    static void reachesTargetWhileDiscoveringObstacles() {
        // Same idea as the real lab: the agent only learns the 4 cells around it at each step
        Set<Position> walls = new HashSet<>(row(3, 0, 6));
        walls.addAll(column(7, 3, 8));
        walls.addAll(row(8, 2, 7));
        int steps = walk(new GridPolicy(), new Position(3, 0), new Position(4, 6), walls, false);
        check("reaches the target with partial perception, without collisions", true, steps > 0);
    }

    static void failsWhenStartIsEnclosed() {
        Position start = new Position(5, 5);
        Map<Position, Boolean> known = new HashMap<>();
        for (Position p : neighbours(start)) {
            known.put(p, true);
        }
        checkThrows("fails when the start is enclosed",
                () -> new GridPolicy().decide(start, new Position(9, 9), known));
    }

    // ===== Helpers =====

    // Moves an agent with the policy until it reaches the target.
    // fullKnowledge = true: the agent knows every wall from the start.
    // fullKnowledge = false: it only learns its 4 neighbours at each step, like the bumpers.
    // Returns the number of steps, or -1 on a collision, an error or too many steps.
    static int walk(GridPolicy policy, Position start, Position target, Set<Position> walls, boolean fullKnowledge) {
        Map<Position, Boolean> known = new HashMap<>();
        if (fullKnowledge) {
            for (Position w : walls) {
                known.put(w, true);
            }
        }
        Position current = start;
        for (int steps = 0; steps < 500; steps++) {
            if (current.equals(target)) {
                return steps;
            }
            for (Position n : neighbours(current)) {
                known.put(n, isWall(n, walls));
            }
            String action;
            try {
                action = policy.decide(current, target, known);
            } catch (RuntimeException e) {
                System.out.println("    policy error at " + current + " : " + e.getMessage());
                return -1;
            }
            current = switch (action) {
                case GridPolicy.ACTION_NORTH -> current.move(0, -1);
                case GridPolicy.ACTION_EAST -> current.move(1, 0);
                case GridPolicy.ACTION_SOUTH -> current.move(0, 1);
                case GridPolicy.ACTION_WEST -> current.move(-1, 0);
                default -> null;
            };
            if (current == null || isWall(current, walls)) {
                System.out.println("    collision or invalid action : " + action);
                return -1;
            }
        }
        return -1;
    }

    static boolean isWall(Position p, Set<Position> walls) {
        return p.x() < 0 || p.y() < 0 || walls.contains(p);
    }

    static Position[] neighbours(Position p) {
        return new Position[]{p.move(0, -1), p.move(1, 0), p.move(0, 1), p.move(-1, 0)};
    }

    static Set<Position> row(int y, int fromX, int toX) {
        Set<Position> cells = new HashSet<>();
        for (int x = fromX; x <= toX; x++) {
            cells.add(new Position(x, y));
        }
        return cells;
    }

    static Set<Position> column(int x, int fromY, int toY) {
        Set<Position> cells = new HashSet<>();
        for (int y = fromY; y <= toY; y++) {
            cells.add(new Position(x, y));
        }
        return cells;
    }

    static void check(String name, Object expected, Object actual) {
        if (expected.equals(actual)) {
            System.out.println("PASS " + name);
        } else {
            failures++;
            System.out.println("FAIL " + name + " : expected " + expected + ", got " + actual);
        }
    }

    static void checkThrows(String name, Runnable code) {
        try {
            code.run();
            failures++;
            System.out.println("FAIL " + name + " : no exception");
        } catch (RuntimeException e) {
            System.out.println("PASS " + name + " (" + e.getMessage() + ")");
        }
    }
}
