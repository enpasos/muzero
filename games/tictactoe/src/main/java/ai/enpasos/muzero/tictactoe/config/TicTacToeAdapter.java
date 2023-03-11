package ai.enpasos.muzero.tictactoe.config;

import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;

import java.util.Arrays;

public class TicTacToeAdapter {

    public static float[] translateToObservation(MuZeroConfig config, int[][] board) {
        OneOfTwoPlayer playerPerspective = OneOfTwoPlayer.PLAYER_A;
        float[] observation = new float[ 2 * config.getBoardHeight() * config.getBoardWidth()];
        int index = 0;
        getBoardPositions(observation, index, board, playerPerspective.getValue());
        index += config.getBoardHeight() * config.getBoardWidth();

        getBoardPositions(observation, index, board, -1 * playerPerspective.getValue());

        return observation;
    }

    public static float[] changePlayerPerspective(MuZeroConfig config, float[] observation) {

        float[] observationChanged = new float[observation.length];
        int n = config.getBoardHeight() * config.getBoardWidth();

        for (int i = 0; i < n; i++) {
            observationChanged[i+n] = observation[i] == 0f ? 1f : 0f;
        }
        for (int i = n; i < observationChanged.length; i++) {
            observationChanged[i-n] = observation[i] == 0f ? 1f : 0f;
        }

        return observationChanged;
    }


    private static void getBoardPositions(float[] result, int index, int[][] board, int p) {
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                result[index + i * board.length + j] = board[i][j] == p ? 1f : 0f;
            }
        }
    }
}
