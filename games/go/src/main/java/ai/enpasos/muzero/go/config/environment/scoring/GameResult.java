package ai.enpasos.muzero.go.config.environment.scoring;


import ai.enpasos.muzero.go.config.environment.GoBoard;
import ai.enpasos.muzero.go.config.environment.basics.Player;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BLACK_PLAYER;
import static ai.enpasos.muzero.go.config.environment.basics.Player.WHITE_PLAYER;
import static java.text.MessageFormat.format;

/**
 * Compute the result of a game
 * numBlackCaptures refers to the number of white stones captured by black.
 * If a player won by resignation, they are the winner regardless of the score statistics.
 * For the scoring calculation to be most accurate, the game has to really be over in the sense that
 * all stones that can be captured are captured. There is no cost to filling in your own territory as
 * long as you do not fill in either of your last 2 eyes.
 * <p>
 * adapted from <a href="https://github.com/maxpumperla/ScalphaGoZero">...</a>
 */
@Data
@Builder
public class GameResult {

    private Player wonByResignation;
    private int numBlackStones;
    private int numWhiteStones;
    private int numBlackTerritory;
    private int numWhiteTerritory;
    private int numBlackCaptures;
    private int numWhiteCaptures;
    private int numDame;
    private float komi;

    public static GameResult apply(GoBoard goBoard, float komi) {
        return apply(goBoard, komi, null);
    }

    /**
     * Compute the game result from the current state.
     *
     * @param goBoard GoBoard instance
     * @return GameResult object
     */
    public static GameResult apply(GoBoard goBoard, float komi, Player wonByResignation) {
        var territoryCalculator = new TerritoryCalculator(goBoard);
        var territoryMap = territoryCalculator.evaluateTerritory();

        int numBlackStones = 0;
        int numWhiteStones = 0;
        int numBlackTerritory = 0;
        int numWhiteTerritory = 0;
        int numBlackCaptures = 0;
        int numWhiteCaptures = 0;
        int numDame = 0;

        for (Map.Entry<Point, VertexType> entry : territoryMap.entrySet()) {
            VertexType status = entry.getValue();
            switch (status) {
                case BLACK_STONE -> numBlackStones += 1;
                case WHITE_STONE -> numWhiteStones += 1;
                case BLACK_TERRITORY -> numBlackTerritory += 1;
                case WHITE_TERRITORY -> numWhiteTerritory += 1;
                case CAPTURED_BLACK_STONE -> {
                    numWhiteTerritory += 1;
                    numWhiteCaptures += 1;
                }
                case CAPTURED_WHITE_STONE -> {
                    numBlackTerritory += 1;
                    numBlackCaptures += 1;
                }
                case DAME -> numDame += 1;
            }
        }
        return GameResult.builder()
            .numBlackStones(numBlackStones)
            .numWhiteStones(numWhiteStones)
            .numBlackCaptures(goBoard.getBlackCaptures() + numBlackCaptures)
            .numWhiteCaptures(goBoard.getWhiteCaptures() + numWhiteCaptures)
            .numBlackTerritory(numBlackTerritory)
            .numWhiteTerritory(numWhiteTerritory)
            .numDame(numDame)
            .komi(komi)
            .wonByResignation(wonByResignation)
            .build();
    }

    /**
     * Points black scored
     */
    public int blackPoints() {
        return numBlackTerritory + numBlackStones;
    }

    /**
     * Points white scored
     */
    public int whitePoints() {
        return numWhiteTerritory + numWhiteStones;
    }

    public float blackWinningMargin() {
        return blackPoints() - (whitePoints() + komi);
    }

    public Player winner() {
        return (blackWinningMargin() > 0) ? BLACK_PLAYER : WHITE_PLAYER;
    }


    public String toDebugString() {
        return format("Black: territory({0,number,#}) + stones({1,number,#}) = {2,number,#}\n",
            numBlackTerritory, numBlackStones, blackPoints())
            + format("White: territory({0,number,#}) + stones({1,number,#}) = {2,number,#}\n",
            numWhiteTerritory, numWhiteStones, whitePoints())
            + format("num dame = {0,number,#},  komi = {1,number,#.#}\n", numDame, komi);
    }

    @Override
    public String toString() {
        if (wonByResignation != null) {
            return (wonByResignation == BLACK_PLAYER ? "Black" : "White") + " won by resignation";
        } else {
            return switch (winner()) {
                case BLACK_PLAYER -> "Black +" + blackWinningMargin();
                case WHITE_PLAYER -> "White +" + (-blackWinningMargin());
                default -> "this should not happen";
            };
        }

    }

    public GameResult apply(GoBoard goBoard, Player wonByResignation) {

        return apply(goBoard, komi, wonByResignation);
    }

    public GameResult apply(GoBoard goBoard) {
        return apply(goBoard, komi, null);
    }
}
