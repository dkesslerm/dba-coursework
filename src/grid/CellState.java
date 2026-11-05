package grid;

public enum CellState {
    // TODO
    //what states do we actually need? boolean not enough bc we also need to know where the target is?
    // or should we check with the heuristics? or does it happen automatically? when we reach the target?
    // do we need the Visited for backtrack? Prob. not bc would make algorithm more complex.
    UNKNOWN, FREE, BLOCKED, VISITED, TARGET
}
