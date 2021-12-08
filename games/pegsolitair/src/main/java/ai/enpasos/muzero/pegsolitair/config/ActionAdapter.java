package ai.enpasos.muzero.pegsolitair.config;

import ai.enpasos.muzero.pegsolitair.config.environment.Direction;
import ai.enpasos.muzero.pegsolitair.config.environment.Jump;
import ai.enpasos.muzero.pegsolitair.config.environment.Point;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;

import java.util.*;

public class ActionAdapter {

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

    private ActionAdapter() {
    }

    public static Action getAction(MuZeroConfig config, Jump jump) {
        return config.newAction(jumpIntegerMap.get(jump));
    }


    public static Jump getJump(Action action) {
        return integerJumpMap.get(action.getIndex());
    }
}
