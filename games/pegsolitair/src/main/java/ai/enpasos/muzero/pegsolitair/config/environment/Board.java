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
        int score = 0;
        for (Point peg : stonesOnTheBoard) {
            score += pagoda(peg);
        }
        if (stonesOnTheBoard.size() == 1 &&  isOneStoneInTheMiddle()) {
            score++; // the final goal is to have one stone in the middle -> reward it
        }
        return score;
    }

    private int pagoda(Point point) {
        int score = 0;
        if (point.equals(new Point(1,3)))  score+=1;
        if (point.equals(new Point(1,4)))  score+=1;
        if (point.equals(new Point(1,5)))  score+=1;

        if (point.equals(new Point(2,3)))  score+=1;
        if (point.equals(new Point(2,4)))  score+=2;
        if (point.equals(new Point(2,5)))  score+=1;

        if (point.equals(new Point(3,1)))  score+=1;
        if (point.equals(new Point(3,2)))  score+=1;
        if (point.equals(new Point(3,3)))  score+=2;
        if (point.equals(new Point(3,4)))  score+=3;
        if (point.equals(new Point(3,5)))  score+=2;
        if (point.equals(new Point(3,6)))  score+=1;
        if (point.equals(new Point(3,7)))  score+=1;

        if (point.equals(new Point(4,1)))  score+=1;
        if (point.equals(new Point(4,2)))  score+=2;
        if (point.equals(new Point(4,3)))  score+=3;
        if (point.equals(new Point(4,4)))  score+=5;
        if (point.equals(new Point(4,5)))  score+=3;
        if (point.equals(new Point(4,6)))  score+=2;
        if (point.equals(new Point(4,7)))  score+=1;

        if (point.equals(new Point(5,1)))  score+=1;
        if (point.equals(new Point(5,2)))  score+=1;
        if (point.equals(new Point(5,3)))  score+=2;
        if (point.equals(new Point(5,4)))  score+=3;
        if (point.equals(new Point(5,5)))  score+=2;
        if (point.equals(new Point(5,6)))  score+=1;
        if (point.equals(new Point(5,7)))  score+=1;

        if (point.equals(new Point(6,3)))  score+=1;
        if (point.equals(new Point(6,4)))  score+=2;
        if (point.equals(new Point(6,5)))  score+=1;

        if (point.equals(new Point(7,3)))  score+=1;
        if (point.equals(new Point(7,4)))  score+=1;
        if (point.equals(new Point(7,5)))  score+=1;
        return score-5;
    }

    public int getScoreB() {
        int score = 0;
        score -= stonesOnTheBoard.size(); // less stones on the board is better -> penalty on remaining stones
        score -= getNumberOfConvexCornerStones();
        if (stonesOnTheBoard.size() == 1 &&  isOneStoneInTheMiddle()) {
            score++; // the final goal is to have one stone in the middle -> reward it
        }
        if (isThereAtLeastOneStoneInCenterGroup()) {
            score++;  // all stones belong to 4 groups that never mix. Therefore it is necessary to have at least
            // one stone in the group of stones that can reach the targeted final position
        }
        if (stonesOnTheBoard.size() >= 1 && getNumberOfStonesInCenterGroup() > 1) {
            score++;
        }
        if (areThereAtLeastTwoStonesAndOneInCenterTangentGroups()) {
            score++;
        }
        return score;
    }



    private int getNumberOfConvexCornerStones() {
        int c = 0;
        if (stonesOnTheBoard.contains(new Point(1, 3) )) c++;
        if (stonesOnTheBoard.contains(new Point(1, 5) )) c++;
        if (stonesOnTheBoard.contains(new Point(3, 1) )) c++;
        if (stonesOnTheBoard.contains(new Point(3, 7) )) c++;
        if (stonesOnTheBoard.contains(new Point(5, 1) )) c++;
        if (stonesOnTheBoard.contains(new Point(5, 7) )) c++;
        if (stonesOnTheBoard.contains(new Point(7, 3) )) c++;
        if (stonesOnTheBoard.contains(new Point(7, 5) )) c++;
        return c;
    }

    private boolean areThereAtLeastTwoStonesAndOneInCenterTangentGroups() {
        return stonesOnTheBoard.size() >=2 &&
                Set.of(
                        new Point(1, 4),
                        new Point(2, 3),
                        new Point(2, 5),
                        new Point(3, 2),
                        new Point(3, 4),
                        new Point(3, 6),
                        new Point(4, 1),
                        new Point(4, 3),
                        new Point(4, 5),
                        new Point(4, 7),
                        new Point(5, 2),
                        new Point(5, 4),
                        new Point(5, 6),
                        new Point(6, 3),
                        new Point(6, 5),
                        new Point(7,4))
                .stream().anyMatch(stonesOnTheBoard::contains);
    }

    private Set<Point> centerGroup() {
        return Set.of(
                new Point(2, 4),
                new Point(4, 2),
                new Point(4, 4),
                new Point(4, 6),
                new Point(6, 4));
    }

    public boolean isThereAtLeastOneStoneInCenterGroup() {
        return centerGroup().stream().anyMatch(stonesOnTheBoard::contains);
    }

    private int getNumberOfStonesInCenterGroup() {
        return  (int)centerGroup().stream().filter(p -> stonesOnTheBoard.contains(p)).count();
    }

    public boolean isOneStoneInTheMiddle() {
        Point center = new Point(4,4);
        return stonesOnTheBoard.contains(center);
    }
}
