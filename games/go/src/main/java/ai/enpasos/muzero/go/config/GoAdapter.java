package ai.enpasos.muzero.go.config;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.go.config.environment.GameState;
import ai.enpasos.muzero.go.config.environment.GoString;
import ai.enpasos.muzero.go.config.environment.basics.Player;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import ai.enpasos.muzero.go.config.environment.basics.move.Move;
import ai.enpasos.muzero.go.config.environment.basics.move.Pass;
import ai.enpasos.muzero.go.config.environment.basics.move.Play;
import ai.enpasos.muzero.go.config.environment.basics.move.Resign;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.common.Constants;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GoAdapter {

    private GoAdapter() {}

    public static Move translate(MuZeroConfig config, Action action) {
        if (0 <= action.getIndex() && action.getIndex() < config.getActionSpaceSize() - 1) {
            return Play.apply(((GoAction) action).getRow() + 1, ((GoAction) action).getCol() + 1);
        } else if (action.getIndex() == config.getActionSpaceSize() - 1) {
            return new Pass();
        } else {
            throw new MuZeroException(Constants.THIS_SHOULD_NOT_HAPPEN);
        }
    }

    public static List<NDArray> translate(MuZeroConfig config, NDManager ndManager, GameState gameState) {
        List<NDArray> list = new ArrayList<>();

        Player player = gameState.getNextPlayer();

        // values in the range [0, 1]
        // 8 historic boards needed
        NDArray boardCurrentPlayer = ndManager.full(new Shape(config.getBoardHeight(), config.getBoardWidth()), 0f);
        list.add(boardCurrentPlayer);
        NDArray boardOpponentPlayer = ndManager.full(new Shape(config.getBoardHeight(), config.getBoardWidth()), 0f);
        list.add(boardOpponentPlayer);

        for (int row = 0; row < config.getBoardHeight(); row++) {
            for (int col = 0; col < config.getBoardWidth(); col++) {
                var p = new Point(row + 1, col + 1);
                Optional<GoString> goStringOptional = gameState.getBoard().getGoString(p);
                if (goStringOptional.isPresent()) {
                    GoString goString = goStringOptional.get();
                    if (goString.getPlayer() == player) {
                        boardCurrentPlayer.setScalar(new NDIndex(row, col), 1f);
                    } else {
                        boardOpponentPlayer.setScalar(new NDIndex(row, col), 1f);
                    }
                }
            }
        }
        return list;
    }


    public static Action translate(MuZeroConfig config, Move move) {
        if (move instanceof Play) {
            Play play = (Play) move;
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
            case BlackPlayer:
                return OneOfTwoPlayer.PLAYER_A;
            case WhitePlayer:
                return OneOfTwoPlayer.PLAYER_B;
            default:
                throw new MuZeroException("this should not happen");
        }
    }
}
