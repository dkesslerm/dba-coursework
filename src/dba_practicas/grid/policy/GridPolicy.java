package dba_practicas.grid.policy;

import dba_practicas.grid.environment.Position;

import java.util.*;

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
            throw new RuntimeException(current + " equals " + target + " already!");
        }
        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingInt(Node::f).thenComparingInt(Node::g));
        Node currentAstarNode = new Node(current, 0, manhattan(current, target), null);
        Set<Position> closed = new HashSet<>();

        while (!currentAstarNode.position().equals(target)) {
            //add the current position to closed/visited nodes
            closed.add(currentAstarNode.position());

            //get all neighbours of current node
            List<Position> neighbours = getNeighbours(currentAstarNode.position(), known);

            //add all of the neighbours into the Priorityqueue
            for (Position p : neighbours) {
                if (closed.contains(p)) continue;
                open.add(new Node(p, currentAstarNode.g() + 1, manhattan(p, target), currentAstarNode));

            }

            //pop first, make it current Node, do that until we pop node that hasn't been visited yet.
            try {
                do {
                    currentAstarNode = open.remove();
                } while (closed.contains(currentAstarNode.position()));
            } catch (NoSuchElementException e) {
                throw new NoSuchElementException("No path from: " + current + " to: " + target);
            }
        }
        // the next Node is target start backtracking and return first move
        return firstAction(currentAstarNode);
    }

    // Heuristic; is the h(n).
    private int manhattan(Position a, Position b) {
        return Math.abs(a.x() - b.x()) + Math.abs(a.y() - b.y());
    }

    // Gets the 4 adjacent cells, filters out blocked ones
    private List<Position> getNeighbours(Position p, Map<Position, Boolean> known) {
        List<Position> result = new ArrayList<>(4);
        Position[] positions = new Position[]{p.move(0, -1),
                p.move(1, 0),
                p.move(0, 1),
                p.move(-1, 0)};
        for (Position next : positions) {
            // Adds neighbour if not discovered state yet or if it's free
            if (!known.containsKey(next) || !known.get(next)) {
                result.add(next);
            }
        }
        return result;
    }

    // Follows the parents back to the start and converts the first move into an action code
    private String firstAction(Node goal) {
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
