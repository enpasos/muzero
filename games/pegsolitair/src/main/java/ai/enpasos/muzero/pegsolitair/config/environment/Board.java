package ai.enpasos.muzero.pegsolitair.config.environment;

import java.util.List;

import static ai.enpasos.muzero.pegsolitair.config.environment.NeighborMap.createNeighborMap;

public class Board {


    private static NeighborMap neighborMap;

    // -1 = not allowed position
    // 1 = filled position
    // 0 = empty position
    int[][] state;

    static {
        neighborMap = createNeighborMap();
    }


    public Board() {
        state = new int[][] {
                {-1, -1, 1, 1, 1, -1, -1},
                {-1, -1, 1, 1, 1, -1, -1},
                {1, 1, 1, 1, 1, 1, 1},
                {1, 1, 1, 0, 1, 1, 1},
                {1, 1, 1, 1, 1, 1, 1},
                {-1, -1, 1, 1, 1, -1, -1},
                {-1, -1, 1, 1, 1, -1, -1}
        };

    }

    public List<Move> getLegalMoves() {
        // find empty positions
        // identify stones that could jump
        // for each such stone investigate the tree of possible moves

        // ...
        // alternatively look at the movement of the empty positions: it can move in (N,E,S,W) if there are two occupied space
        // it leaves an occupied space behind and produces two empty positions.
        // recursively the new (second) holes can move the same way filling the List<Jump> in a Move
        return null;
    }


}
