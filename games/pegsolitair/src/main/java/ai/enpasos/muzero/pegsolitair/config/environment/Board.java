package ai.enpasos.muzero.pegsolitair.config.environment;

import lombok.Data;

import java.util.*;

import static ai.enpasos.muzero.pegsolitair.config.environment.NeighborMap.createNeighborMap;
import static ai.enpasos.muzero.pegsolitair.config.environment.NeighborMap.inRange;

@Data
public class Board {


    static NeighborMap neighborMap;

    static {
        neighborMap = createNeighborMap();
    }

    TreeSet<Point> pegsOnTheBoard;
    TreeSet<Point> holesOnTheBoard;


    public Board() {

        pegsOnTheBoard = new TreeSet<>();
        pegsOnTheBoard.addAll(neighborMap.getMap().keySet());
        holesOnTheBoard = new TreeSet<>();

        Point firstHole = new Point(4, 4);
        removePeg(firstHole);

    }

    private void removePeg(Point point) {
        pegsOnTheBoard.remove(point);
        holesOnTheBoard.add(point);
    }


    public Board copy() {
        Board clone = new Board();
        clone.holesOnTheBoard.addAll(this.holesOnTheBoard);
        clone.pegsOnTheBoard.addAll(this.pegsOnTheBoard);
        return clone;
    }


    public List<Jump> getLegalJumps() {

        List<Jump> legalJumps = new ArrayList<>();

        holesOnTheBoard.forEach(
                p1 -> addDirectLegalJumpsForAHole(p1, legalJumps)
        );

        return legalJumps;
    }

    private void addDirectLegalJumpsForAHole(Point hole, List<Jump> legalJumps) {
        Arrays.stream(Direction.values()).forEach(
                direction -> {
                    Point p2 = hole.pointIn(direction);
                    Point p3 = p2.pointIn(direction);
                    if (!inRange(p2) || !inRange(p3)) return;

                    if (!pegsOnTheBoard.contains(p2) || holesOnTheBoard.contains(p3)) return;

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

        StringBuilder buf = new StringBuilder();
        buf.append("   1  2  3  4  5  6  7\n");
        for (int row = 1; row <= 7; row++) {
            buf.append(row);
            for (int col = 1; col <= 7; col++) {
                buf.append("  ");
                Point p = new Point(row, col);
                if (this.pegsOnTheBoard.contains(p)) {
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
        pegsOnTheBoard.remove(p1);
        pegsOnTheBoard.remove(p2);
        pegsOnTheBoard.add(p3);
        holesOnTheBoard.remove(p3);
        holesOnTheBoard.add(p1);
        holesOnTheBoard.add(p2);
    }

    public float getScore() {
        float score = 0f;
        for (Point peg : pegsOnTheBoard) {
            score--; // depending on the number of pegs on the board
            score += locationScore(peg) / 5f; // closer to the target position is better
        }
        if (pegsOnTheBoard.size() == 1 && isOnePegInTheMiddle()) {
            score++; // the final goal is to have one peg in the middle -> reward it
        }
        if (impossibleToReachSituation()) {
            score -= 10;
        }
        return score;
    }

    private boolean impossibleToReachSituation() {
        return !isThereAtLeastOnePegInCenterGroup()
                || !conditionA()
                || !conditionB()
                || !conditionC()
                || !conditionD()
                || !conditionE()
                || !conditionF();
    }

    private Set<Point> centerGroup() {
        return Set.of(
                new Point(2, 4),
                new Point(4, 2),
                new Point(4, 4),
                new Point(4, 6),
                new Point(6, 4));
    }

    public boolean isThereAtLeastOnePegInCenterGroup() {
        return centerGroup().stream().anyMatch(pegsOnTheBoard::contains);
    }


    private int locationScore(Point peg) {
        int score = 0;
        score += scoreForPeg(peg, 1, 3, -1);
        score += scoreForPeg(peg, 1, 4, 1);
        score += scoreForPeg(peg, 1, 5, -1);

        score += scoreForPeg(peg, 2, 3, 1);
        score += scoreForPeg(peg, 2, 4, 2);
        score += scoreForPeg(peg, 2, 5, 1);

        score += scoreForPeg(peg, 3, 1, -1);
        score += scoreForPeg(peg, 3, 2, 1);
        score += scoreForPeg(peg, 3, 3, 2);
        score += scoreForPeg(peg, 3, 4, 3);
        score += scoreForPeg(peg, 3, 5, 2);
        score += scoreForPeg(peg, 3, 6, 1);
        score += scoreForPeg(peg, 3, 7, -1);

        score += scoreForPeg(peg, 4, 1, 1);
        score += scoreForPeg(peg, 4, 2, 2);
        score += scoreForPeg(peg, 4, 3, 3);
        score += scoreForPeg(peg, 4, 4, 5);
        score += scoreForPeg(peg, 4, 5, 3);
        score += scoreForPeg(peg, 4, 6, 2);
        score += scoreForPeg(peg, 4, 7, 1);

        score += scoreForPeg(peg, 5, 1, -1);
        score += scoreForPeg(peg, 5, 2, 1);
        score += scoreForPeg(peg, 5, 3, 2);
        score += scoreForPeg(peg, 5, 4, 3);
        score += scoreForPeg(peg, 5, 5, 2);
        score += scoreForPeg(peg, 5, 6, 1);
        score += scoreForPeg(peg, 5, 7, -1);

        score += scoreForPeg(peg, 6, 3, 1);
        score += scoreForPeg(peg, 6, 4, 2);
        score += scoreForPeg(peg, 6, 5, 1);

        score += scoreForPeg(peg, 7, 3, -1);
        score += scoreForPeg(peg, 7, 4, 1);
        score += scoreForPeg(peg, 7, 5, -1);

        return score;
    }

    private int scoreForPeg(Point peg, int row, int col, int scoreChange) {
        if (peg.equals(new Point(row, col))) {
            return scoreChange;
        } else {
            return 0;
        }
    }


    private Set<Point> groupA() {
        return Set.of(
                new Point(1, 3),
                new Point(1, 5),
                new Point(3, 1),
                new Point(3, 3),
                new Point(3, 5),
                new Point(3, 7),
                new Point(5, 1),
                new Point(5, 3),
                new Point(5, 5),
                new Point(5, 7),
                new Point(7, 3),
                new Point(7, 5));
    }

    private Set<Point> circleA() {
        return Set.of(
                new Point(3, 1),
                new Point(3, 7),
                new Point(5, 1),
                new Point(5, 7));
    }

    private Set<Point> circleB() {
        return Set.of(
                new Point(1, 3),
                new Point(1, 5),
                new Point(7, 3),
                new Point(7, 5));
    }

    private Set<Point> circleC() {
        return Set.of(
                new Point(1, 4),
                new Point(4, 1),
                new Point(4, 7),
                new Point(7, 4));
    }

    private Set<Point> circleD() {
        return Set.of(
                new Point(2, 3),
                new Point(2, 5),
                new Point(3, 2),
                new Point(3, 4),
                new Point(5, 2),
                new Point(5, 4),
                new Point(6, 3),
                new Point(6, 5));
    }

    private Set<Point> circleE() {
        return Set.of(
                new Point(2, 4),
                new Point(6, 4));
    }

    private Set<Point> circleF() {
        return Set.of(
                new Point(4, 2),
                new Point(4, 6));
    }

    private Set<Point> groupB() {
        return Set.of(
                new Point(1, 4),
                new Point(3, 2),
                new Point(3, 4),
                new Point(3, 6),
                new Point(5, 2),
                new Point(5, 4),
                new Point(5, 6),
                new Point(7, 4));
    }

    private Set<Point> groupC() {
        return Set.of(
                new Point(2, 3),
                new Point(2, 5),
                new Point(4, 1),
                new Point(4, 3),
                new Point(4, 5),
                new Point(4, 7),
                new Point(6, 3),
                new Point(6, 5));
    }

    private Set<Point> groupD() {
        return Set.of(
                new Point(2, 4),
                new Point(4, 2),
                new Point(4, 4),
                new Point(4, 6),
                new Point(6, 4));
    }


    private boolean conditionA() {
        return condition(circleA(), groupB());
    }

    private boolean conditionB() {
        return condition(circleB(), groupC());
    }

    private boolean conditionC() {

        return condition(circleC(), groupD());
    }

    private boolean conditionD() {
        return condition(circleD(), groupA());
    }

    private boolean conditionE() {
        return condition(circleE(), groupB());
    }

    private boolean conditionF() {
        return condition(circleF(), groupC());
    }

    private boolean condition(Set<Point> circle, Set<Point> group) {
        return numberOfPegsOnPointSet(circle) <= numberOfPegsOnPointSet(group);
    }

    private int numberOfPegsOnPointSet(Set<Point> pointSet) {
        return (int) pointSet.stream().filter(p -> pegsOnTheBoard.contains(p)).count();
    }


    public boolean isOnePegInTheMiddle() {
        Point center = new Point(4, 4);
        return pegsOnTheBoard.contains(center);
    }
}
