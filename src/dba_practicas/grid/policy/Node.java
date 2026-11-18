package dba_practicas.grid.policy;
import dba_practicas.grid.environment.Position;

// One node of a search: g = steps from the start, h = estimate to the target
public record Node(Position position, int g, int h, Node parent) {
    int f() {
        return g + h;
    }
}