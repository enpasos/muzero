package ai.enpasos.muzero.go.config;


import ai.enpasos.muzero.go.config.environment.GameState;
import ai.enpasos.muzero.go.config.environment.GoString;
import ai.enpasos.muzero.go.config.environment.basics.Player;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import ai.enpasos.muzero.go.config.environment.basics.move.Move;
import ai.enpasos.muzero.go.config.environment.basics.move.Pass;
import ai.enpasos.muzero.go.config.environment.basics.move.Play;
import ai.enpasos.muzero.go.config.environment.basics.move.Resign;
import ai.enpasos.muzero.platform.agent.b_planning.Action;
import ai.enpasos.muzero.platform.common.Constants;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;

import java.util.Optional;

public class GoAdapter {

    private GoAdapter() {
    }

    public static Move translate(MuZeroConfig config, Action action) {
        try {
            if (0 <= action.getIndex() && action.getIndex() < config.getActionSpaceSize() - 1) {
                return Play.apply(((GoAction) action).getRow() + 1, ((GoAction) action).getCol() + 1);
            } else if (action.getIndex() == config.getActionSpaceSize() - 1) {
                return new Pass();
            } else {
                throw new MuZeroException(Constants.THIS_SHOULD_NOT_HAPPEN);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new MuZeroException("needs to be fixed", e);
        }
    }

    public static void translate(MuZeroConfig config, float[] result, int index, GameState gameState) {

        Player player = gameState.getNextPlayer();

        // values in the range [0, 1]
        // 8 historic boards needed

        int boardHeight = config.getBoardHeight();
        int boardWidth = config.getBoardWidth();
        int opponentOffset = boardHeight * boardWidth;

        for (int row = 0; row < config.getBoardHeight(); row++) {
            for (int col = 0; col < config.getBoardWidth(); col++) {
                var p = new Point(row + 1, col + 1);
                Optional<GoString> goStringOptional = gameState.getBoard().getGoString(p);
                if (goStringOptional.isPresent()) {
                    GoString goString = goStringOptional.get();
                    if (goString.getPlayer() == player) {
                        result[index + row * boardWidth + col] = 1f;
                    } else {
                        result[index + opponentOffset + row * boardWidth + col] = 1f;
                    }
                }
            }
        }

    }


    public static Action translate(MuZeroConfig config, Move move) {
        if (move instanceof Play play) {
            return new GoAction(config, play.getPoint().getRow() - 1, play.getPoint().getCol() - 1);
        } else if (move instanceof Pass) {
            return config.newAction(config.getActionSpaceSize() - 1);
        } else if (move instanceof Resign) {
            // Muzero does not resign -> not advantage for winning, it is efficiency not effectivity
            throw new MuZeroException("muzero is not resigning");
        }
        throw new MuZeroException("this should not happen");
    }

    public static OneOfTwoPlayer translate(Player player) {
        switch (player) {
            case BLACK_PLAYER:
                return OneOfTwoPlayer.PLAYER_A;
            case WHITE_PLAYER:
                return OneOfTwoPlayer.PLAYER_B;
            default:
                throw new MuZeroException("this should not happen");
        }
    }
}
