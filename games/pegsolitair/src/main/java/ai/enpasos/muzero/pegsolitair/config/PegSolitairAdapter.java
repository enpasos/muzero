package ai.enpasos.muzero.pegsolitair.config;

import ai.enpasos.muzero.pegsolitair.config.environment.Board;
import ai.enpasos.muzero.pegsolitair.config.environment.Direction;
import ai.enpasos.muzero.pegsolitair.config.environment.Jump;
import ai.enpasos.muzero.pegsolitair.config.environment.Point;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.agent.e_experience.Observation;
import ai.enpasos.muzero.platform.agent.e_experience.ObservationOnePlayer;
import ai.enpasos.muzero.platform.agent.e_experience.ObservationTwoPlayers;
import ai.enpasos.muzero.platform.config.MuZeroConfig;

import java.util.*;

public class PegSolitairAdapter {

    private static final Map<Integer, Jump> integerJumpMap;
    private static final Map<Jump, Integer> jumpIntegerMap;

    static {
        List<Jump> possibleJumps = new ArrayList<>();
        integerJumpMap = new HashMap<>();
        jumpIntegerMap = new HashMap<>();
        Arrays.stream(Direction.values()).forEach(
            direction -> {
                for (int row = 1; row <= 7; row++) {
                    for (int col = 1; col <= 7; col++) {
                        Point p = new Point(row, col);
                        Jump j = new Jump(p, direction);
                        possibleJumps.add(j);
                    }
                }
            });

        int c = 0;
        for (Jump j : possibleJumps) {
            integerJumpMap.put(c, j);
            jumpIntegerMap.put(j, c);
            c++;
        }
    }

    private PegSolitairAdapter() {
    }

    public static Action getAction(MuZeroConfig config, Jump jump) {
        return config.newAction(jumpIntegerMap.get(jump));
    }


    public static Jump getJump(Action action) {
        return integerJumpMap.get(action.getIndex());
    }



    public static Observation translateToObservation(MuZeroConfig config, Board board) {
        int n = config.getBoardHeight() * config.getBoardWidth();
        return ObservationOnePlayer.builder()
                .partSize(n)
                .part(getBoardPositions(n, board))
                .build();
    }

    private static BitSet getBoardPositions(int n, Board board) {
        BitSet bitSet = new BitSet(n);
        int size = 7;
       // float[] boardtransfer = new float[size * size];
        for (int row = 1; row <= size; row++) {
            for (int col = 1; col <= size; col++) {
                Point p = new Point(row, col);
                if (board.getPegsOnTheBoard().contains(p)) {
                    bitSet.set((row - 1) * size + col - 1);
                }
//                else if (board.getHolesOnTheBoard().contains(p)) {
//                    boardtransfer[(row - 1) * size + col - 1] = 0f;
//                } else if (!inRange(p)) {
//                    boardtransfer[(row - 1) * size + col - 1] = 0f;
//                }
            }
        }
        return bitSet;
    }
}
