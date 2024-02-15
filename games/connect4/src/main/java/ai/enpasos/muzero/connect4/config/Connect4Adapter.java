package ai.enpasos.muzero.connect4.config;

import ai.enpasos.muzero.platform.agent.e_experience.Observation;
import ai.enpasos.muzero.platform.agent.e_experience.ObservationTwoPlayers;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;

import java.util.BitSet;

public class Connect4Adapter {

    private Connect4Adapter() {
    }

    public static Observation translateToObservation(MuZeroConfig config, int[][] board) {
        int n = config.getBoardHeight() * config.getBoardWidth();
        return ObservationTwoPlayers.builder()
                .partSize(n)
                .partA(getBoardPositions(n, board, OneOfTwoPlayer.PLAYER_A.getValue()))
                .partB(getBoardPositions(n, board, OneOfTwoPlayer.PLAYER_B.getValue()))
                .build();

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
