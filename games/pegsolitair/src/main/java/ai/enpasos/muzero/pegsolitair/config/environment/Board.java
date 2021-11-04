package ai.enpasos.muzero.pegsolitair.config.environment;

import java.util.*;

import static ai.enpasos.muzero.pegsolitair.config.environment.NeighborMap.createNeighborMap;
import static ai.enpasos.muzero.pegsolitair.config.environment.NeighborMap.inRange;


public class Board {


    static NeighborMap neighborMap;

    Set<Point> stonesOnTheBoard;
    Set<Point> holesOnTheBoard;

//    // -1 = not allowed position
//    // 1 = filled position
//    // 0 = empty position
//    int[][] state;

    static {
        neighborMap = createNeighborMap();
    }


    public Board() {
//        state = new int[][] {
//                {-1, -1, 1, 1, 1, -1, -1},
//                {-1, -1, 1, 1, 1, -1, -1},
//                {1, 1, 1, 1, 1, 1, 1},
//                {1, 1, 1, 0, 1, 1, 1},
//                {1, 1, 1, 1, 1, 1, 1},
//                {-1, -1, 1, 1, 1, -1, -1},
//                {-1, -1, 1, 1, 1, -1, -1}
//        };
        stonesOnTheBoard = new TreeSet<>();
        stonesOnTheBoard.addAll(neighborMap.getMap().keySet());
        holesOnTheBoard = new TreeSet<>();

        Point firstHole = new Point(4, 4);
        removeStone(new Point(4, 4));



    }

    private void removeStone(Point point) {
        stonesOnTheBoard.remove(point);
        holesOnTheBoard.add(point);
    }



    public List<Move> getLegalMoves() {

        List<Move> legalMoves = new ArrayList<>();



        holesOnTheBoard.stream().forEach(
                p1 ->
                Arrays.stream(Direction.values()).forEach(
                     direction ->  {
                         Point p2 = p1.pointIn(direction);
                         Point p3 = p2.pointIn(direction);
                         if (!inRange(p2) || !inRange(p3)) return;
                         if (!(stonesOnTheBoard.contains(p2) && holesOnTheBoard.contains(p3) )) return;

                         Jump jump = new Jump(p1, direction);
                         Move move = new Move(List.of(jump));
                         legalMoves.add(move);
                     }
                )
        );

        // up to here only the direct jumps are taken into account


        // find empty positions
        // identify stones that could jump
        // for each such stone investigate the tree of possible moves

        // ...
        // alternatively look at the movement of the empty positions: it can move in (N,E,S,W) if there are two occupied space
        // it leaves an occupied space behind and produces two empty positions.
        // recursively the new (second) holes can move the same way filling the List<Jump> in a Move
        return legalMoves;
    }


}
