package dba_practicas.grid.environment;


/**
 * A position in the Grid defined by an x and y coordinate.
 * Used for the agent position, the target and the keys of the known grid.
 * x = column counted from the left, y = row counted from the top.
 */
public record Position(int x, int y) {

    /**
     * Returns the position after moving; this object is not changed.
     */
    public Position move(int xDirection, int yDirection) {
        return new Position(x + xDirection, y + yDirection);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ")";
    }
}
