package ai.enpasos.muzero.tictactoe.config;

import ai.enpasos.muzero.platform.agent.e_experience.Observation;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;

import java.util.Arrays;
import java.util.BitSet;

public class TicTacToeAdapter {

    public static Observation translateToObservation(MuZeroConfig config, int[][] board) {
        int n = config.getBoardHeight() * config.getBoardWidth();
        return Observation.builder()
                .partSize(n)
                .partA(getBoardPositions(n, board, OneOfTwoPlayer.PLAYER_A.getValue()))
                .partB(getBoardPositions(n, board, OneOfTwoPlayer.PLAYER_B.getValue()))
                .build();

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



    private static BitSet getBoardPositions(int n, int[][] board, int p) {
        BitSet result = new BitSet(n);
        for (int i = 0; i < board.length; i++) {
            for (int j = 0; j < board[i].length; j++) {
                result.set(i * board.length + j, board[i][j] == p);
            }
        }
        return result;
    }
}
