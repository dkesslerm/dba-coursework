package dba_practicas.grid.policy;

import dba_practicas.grid.environment.Position;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

public class GridPolicy {
    /*
    Logic of how the agent thinks: A* from the current position to the target over
    the known map. Unknown cells are assumed free, BLOCKED cells are never entered.
    The agent replans at every step and only takes the first move of the path,
    so the path changes as the bumpers reveal new obstacles.
    */

    // Action codes
    public static final String ACTION_NORTH = "grid.move.north";
    public static final String ACTION_EAST = "grid.move.east";
    public static final String ACTION_SOUTH = "grid.move.south";
    public static final String ACTION_WEST = "grid.move.west";


    // Returns the action code of the first move of the shortest path known so far
    public String decide(Position current, Position target, Map<Position, Boolean> known) {
        if (current.equals(target)) {
            throw new IllegalStateException("Already at the target " + target);
        }

        // Open: nodes waiting to be expanded, lowest f first (ties: closest to the target)
        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.comparingInt(Node::f).thenComparingInt(Node::h));
        // Closed: positions already expanded in this search
        Set<Position> closed = new HashSet<>();

        open.add(new Node(current, 0, manhattan(current, target), null));

        while (!open.isEmpty()) {
            Node node = open.poll();
            if (node.position().equals(target)) {
                return firstAction(node);
            }
            // A position can be in open several times with different g: only the first one counts
            if (!closed.add(node.position())) {
                continue;
            }

            for (Position next : neighbours(node.position())) {
                if (known.getOrDefault(next, false) || closed.contains(next)) { 
                    continue;
                }  
                open.add(new Node(next, node.g() + 1, manhattan(next, target), node));
            }
        }
        throw new IllegalStateException("No path from " + current + " to " + target);
    }

    // Heuristic: never overestimates, since each move changes x or y by 1
    private static int manhattan(Position a, Position b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
    }

    // The 4 adjacent cells, without negative coordinates (outside the world)
    private static List<Position> neighbours(Position p) {
        List<Position> result = new ArrayList<>(4);
        for (Position next : new Position[]{p.move(0, -1), p.move(1, 0), p.move(0, 1), p.move(-1, 0)}) {
            if (next.x() >= 0 && next.y() >= 0) {
                result.add(next);
            }
        }
        return result;
    }

    // Follows the parents back to the start and converts the first move into an action code
    private static String firstAction(Node goal) {
        Node step = goal;
        while (step.parent().parent() != null) {
            step = step.parent();
        }
        int dx = step.position().x() - step.parent().position().x();
        int dy = step.position().y() - step.parent().position().y();
        if (dy == -1) return ACTION_NORTH;
        if (dx == 1) return ACTION_EAST;
        if (dy == 1) return ACTION_SOUTH;
        return ACTION_WEST;
    }
}
