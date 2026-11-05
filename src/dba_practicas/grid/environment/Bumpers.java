package dba_practicas.grid.environment;

public class Bumpers {
    /**
     * This class defines the Bumpers, as in the current view of the agent
     * defining the 4 cardinal directions
     */

    private boolean north;
    private boolean east;
    private boolean south;
    private boolean west;

    public Bumpers(boolean north, boolean east, boolean south, boolean west) {
        this.north = north;
        this.east = east;
        this.south = south;
        this.west = west;
    }

    public boolean eastIsBlocked() {
        return east;
    }

    public void setEast(boolean east) {
        this.east = east;
    }

    public boolean southIsBlocked() {
        return south;
    }

    public void setSouth(boolean south) {
        this.south = south;
    }

    public boolean northIsBlocked() {
        return north;
    }

    public void setNorth(boolean north) {
        this.north = north;
    }

    public boolean westIsBlocked() {
        return west;
    }

    public void setWest(boolean west) {
        this.west = west;
    }
}
