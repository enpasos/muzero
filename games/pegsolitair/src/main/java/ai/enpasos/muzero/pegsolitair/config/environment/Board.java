package ai.enpasos.muzero.pegsolitair.config.environment;

import lombok.Data;

import java.util.*;

import static ai.enpasos.muzero.pegsolitair.config.environment.NeighborMap.createNeighborMap;
import static ai.enpasos.muzero.pegsolitair.config.environment.NeighborMap.inRange;

@Data
public class Board {


    static NeighborMap neighborMap;

    TreeSet<Point> stonesOnTheBoard;
    TreeSet<Point> holesOnTheBoard;

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



    public Board clone() {
        Board clone = new Board();
        clone.holesOnTheBoard.addAll(this.holesOnTheBoard);
        clone.stonesOnTheBoard.addAll(this.stonesOnTheBoard);
        return clone;
    }



    public List<Jump> getLegalJumps() {

        List<Jump> legalJumps = new ArrayList<>();

        holesOnTheBoard.stream().forEach(
                p1 ->  addDirectLegalJumpsForAHole(p1, legalJumps)
        );


//        Board currentBoard = this;
//
//        int legalMovesSize = legalMoves.size();
//
//        List<Move> movesToIterate = new ArrayList<>();
//        movesToIterate.addAll(legalMoves);
//
//        do {
//            legalMovesSize = legalMoves.size();
//            List<Move> newMoves = new ArrayList<>();
//            // legalMoves_.addAll(legalMoves);
//            movesToIterate.stream().forEach(
//                    m -> {
//                        Board tempBoard = currentBoard.clone();
//                        tempBoard.applyMove(m);
//
//                        Point p1 = m.getFinalPosition();
//                        Arrays.stream(Direction.values()).forEach(
//                                direction -> {
//                                    Point p2 = p1.pointIn(direction);
//                                    Point p3 = p2.pointIn(direction);
//
//                                    if (inRange(p2) && inRange(p3) && tempBoard.stonesOnTheBoard.contains(p2) && tempBoard.holesOnTheBoard.contains(p3)) {
//                                        Jump jump = new Jump(p1, direction);
//                                        Move move = m.clone();
//                                        move.jumps.add(jump);
//                                        newMoves.add(move);
//                                        legalMoves.add(move);
//                                    }
//                                }
//                        );
//                    }
//            );
//            movesToIterate = newMoves;
//        } while (movesToIterate.size() > 0);

        return legalJumps;
    }

    private void addDirectLegalJumpsForAHole(Point hole, List<Jump> legalJumps) {
        Arrays.stream(Direction.values()).forEach(
             direction ->  {
                 Point p2 = hole.pointIn(direction);
                 Point p3 = p2.pointIn(direction);
                 if (!inRange(p2) || !inRange(p3)) return;

                 if (!stonesOnTheBoard.contains(p2) || holesOnTheBoard.contains(p3) ) return;

                 Jump jump = new Jump(p3, direction.reverse());
                 legalJumps.add(jump);
             }
        );
    }


    public String render() {
        // e.g.
        //   1  2  3  4  5  6  7
        //1        O  O  O
        //2        O  O  O
        //3  O  O  O  O  O  O  O
        //4  O  O  O  .  O  O  O
        //5  O  O  O  O  O  O  O
        //6        O  O  O
        //7        O  O  O

        StringBuffer buf = new StringBuffer();
        buf.append("   1  2  3  4  5  6  7\n");
        for(int row = 1; row <= 7; row++) {
            buf.append(row);
            for(int col = 1; col <= 7; col++) {
                buf.append("  ");
                Point p = new Point(row, col);
//                if (!inRange(p)) {
//
//                }
                if (this.stonesOnTheBoard.contains(p)) {
                    buf.append("O");
                } else if (this.holesOnTheBoard.contains(p)) {
                    buf.append(".");
                } else if (!inRange(p)) {
                    buf.append(" ");
                }
            }
            buf.append("\n");
        }
        return buf.toString();

    }


    public void applyJump(Jump jump) {
        Point p1 = jump.fromPoint;
        Point p2 = p1.pointIn(jump.direction);
        Point p3 = p2.pointIn(jump.direction);
        stonesOnTheBoard.remove(p1);
        stonesOnTheBoard.remove(p2);
        stonesOnTheBoard.add(p3);
        holesOnTheBoard.remove(p3);
        holesOnTheBoard.add(p1);
        holesOnTheBoard.add(p2);
    }

    public int getScore() {
        if (stonesOnTheBoard.size() == 1 && stonesOnTheBoard.first().equals(new Point(4,4))) {
            return 10;
        } else {
            return - stonesOnTheBoard.size();
        }
    }
}
