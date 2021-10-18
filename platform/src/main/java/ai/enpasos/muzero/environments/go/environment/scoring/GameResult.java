package ai.enpasos.muzero.environments.go.environment.scoring;


import ai.enpasos.muzero.environments.go.environment.GoBoard;
import ai.enpasos.muzero.environments.go.environment.basics.Player;
import ai.enpasos.muzero.environments.go.environment.basics.Point;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.Optional;

import static ai.enpasos.muzero.environments.go.environment.basics.Player.BlackPlayer;
import static ai.enpasos.muzero.environments.go.environment.basics.Player.WhitePlayer;
import static java.text.MessageFormat.format;

/**
 * Compute the result of a game
 * numBlackCaptures refers to the number of white stones captured by black.
 * If a player won by resignation, they are the winner regardless of the score statistics.
 * For the scoring calculation to be most accurate, the game has to really be over in the sense that
 * all stones that can be captured are captured. There is no cost to filling in your own territory as
 * long as you do not fill in either of your last 2 eyes.
 * <p>
 * adapted from https://github.com/maxpumperla/ScalphaGoZero
 */
@Data
@Builder
public class GameResult {

    private int numBlackStones;
    private int numWhiteStones;
    private int numBlackTerritory;
    private int numWhiteTerritory;
    private int numBlackCaptures;
    private int numWhiteCaptures;
    private int numDame;
    private float komi;
    Optional<Player> wonByResignation;

    /**
     * Points black scored
     */
    public int blackPoints() {
        return numBlackTerritory + numBlackStones; // + numBlackCaptures;
    }



    /**
     * Points white scored
     */
    public int whitePoints() {
        return numWhiteTerritory + numWhiteStones; // + numWhiteCaptures;
    }


    public float blackWinningMargin() {
        return blackPoints() - (whitePoints() + komi);
    }

    public Player winner() {
        return (blackWinningMargin() > 0) ? BlackPlayer : WhitePlayer;
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
        if (wonByResignation.isPresent()) {
            return (wonByResignation.get() == BlackPlayer ? "Black" : "White") + " won by resignation";
        } else {
            switch (winner()) {
                case BlackPlayer:
                    return "Black +" + blackWinningMargin();
                case WhitePlayer:
                    return "White +" + (-blackWinningMargin());
            }
        }
        return "this should not happen";
    }


 // static final float DEFAULT_KOMI = 7.5f;

  //  static final float DEFAULT_KOMI = 0f;
  //  static final float DEFAULT_KOMI = 0.5f;

   // static float defaultKomi;

    public   GameResult apply(GoBoard goBoard, Optional<Player> wonByResignation) {

        return apply(goBoard, komi, wonByResignation);
    }
    public   GameResult apply(GoBoard goBoard) {
        return apply(goBoard, komi, Optional.empty());
    }

    public static GameResult apply(GoBoard goBoard, float komi) {
        return apply(goBoard, komi, Optional.empty());
    }

        /**
         * Compute the game result from the current state.
         *
         * @param goBoard GoBoard instance
         * @return GameResult object
         */
        public static GameResult apply(GoBoard goBoard, float komi, Optional<Player> wonByResignation) {
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
                    case BlackStone:
                        numBlackStones += 1;
                        break;
                    case WhiteStone:
                        numWhiteStones += 1;
                        break;
                    case BlackTerritory:
                        numBlackTerritory += 1;
                        break;
                    case WhiteTerritory:
                        numWhiteTerritory += 1;
                        break;
                    case CapturedBlackStone:
                        numWhiteTerritory += 1;
                        numWhiteCaptures += 1;
                        break;
                    case CapturedWhiteStone:
                        numBlackTerritory += 1;
                        numBlackCaptures += 1;
                        break;
                    case Dame:
                        numDame += 1;
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
}
