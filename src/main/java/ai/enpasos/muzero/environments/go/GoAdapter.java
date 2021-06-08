package ai.enpasos.muzero.environments.go;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.environments.OneOfTwoPlayer;
import ai.enpasos.muzero.environments.go.environment.*;
import ai.enpasos.muzero.agent.slow.play.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GoAdapter {

    public static Move translate(MuZeroConfig config, Action action) {
        if (0 <= action.getIndex() &&  action.getIndex() < config.getActionSpaceSize() - 1) {
            return Play.apply(action.getRow() + 1, action.getCol() + 1);
        } else if (action.getIndex() == config.getActionSpaceSize() - 1) {
            return new Pass();
        } else {
            throw new RuntimeException("this should not happen");
        }
    }

    public static List<NDArray> translate(MuZeroConfig config, NDManager ndManager, GameState gameState) {
        List<NDArray> list = new ArrayList<>();

   //    int boardSize = config.getBoardWidth();
       // gameState.getBoard().placeStone()

        Player player = gameState.getNextPlayer();

        // values in the range [0, 1]
        // 8 historic boards needed
        NDArray boardCurrentPlayer = ndManager.full(new Shape(config.getBoardHeight(), config.getBoardWidth()), 0f);
        list.add(boardCurrentPlayer);
        NDArray boardOpponentPlayer = ndManager.full(new Shape(config.getBoardHeight(), config.getBoardWidth()), 0f);
        list.add(boardOpponentPlayer);
        Player nextPlayer  = gameState.getNextPlayer();

        for (int row = 0; row < config.getBoardHeight(); row++) {
            for (int col = 0; col < config.getBoardWidth(); col++) {
                var p = new Point(row + 1, col + 1);
                Optional<GoString> goStringOptional =  gameState.getBoard().getGoString(p);
                if (goStringOptional.isPresent()) {
                    GoString goString = goStringOptional.get();
                    if (goString.getPlayer() == player)  {
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
            return new Action(config, play.getPoint().getRow() - 1, play.getPoint().getCol() - 1);
        } else if (move instanceof Pass) {
            return new Action(config, config.getActionSpaceSize() - 1);
        } else if (move instanceof Resign) {
            // Muzero does not resign -> not advantage for winning, it is efficiency not effectivity
            throw new RuntimeException("muzero is not resigning");
        }
        throw new RuntimeException("this should not happen");
    }

    public static OneOfTwoPlayer translate(Player player) {
        switch(player) {
            case BlackPlayer: return OneOfTwoPlayer.PlayerA;
            case WhitePlayer: return OneOfTwoPlayer.PlayerB;
            default: throw new RuntimeException("this should not happen");
        }
    }
}
