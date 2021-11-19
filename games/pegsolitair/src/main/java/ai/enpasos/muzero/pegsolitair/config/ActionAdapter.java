package ai.enpasos.muzero.pegsolitair.config;

import ai.enpasos.muzero.pegsolitair.config.environment.Direction;
import ai.enpasos.muzero.pegsolitair.config.environment.Jump;
import ai.enpasos.muzero.pegsolitair.config.environment.Point;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.slow.play.Action;

import java.util.*;

import static ai.enpasos.muzero.pegsolitair.config.environment.NeighborMap.inRange;

public class ActionAdapter {

    private static Map<Integer, Jump> integerJumpMap;
    private static Map<Jump, Integer> jumpIntegerMap;

    static {
        List<Jump> possibleJumps = new ArrayList<>();
        integerJumpMap = new HashMap<>();
        jumpIntegerMap = new HashMap<>();
        for(int row = 1; row <= 7; row++) {
            for(int col = 1; col <= 7; col++) {
                Point p = new Point(row, col);
                if (!inRange(p)) continue;
                Arrays.stream(Direction.values()).forEach(
                        direction ->  {
                            Point p2 = p.pointIn(direction);
                            Point p3 = p2.pointIn(direction);
                            if (inRange(p2) && inRange(p3)) {
                                Jump j = new Jump(p, direction);
                                possibleJumps.add(j);
                            }
                        }
                );

            }
        }
        int c = 0;
        for(Jump j : possibleJumps) {
            integerJumpMap.put(c, j);
            jumpIntegerMap.put(j, c);
            c++;
        }
    }

    public static Action getAction(MuZeroConfig config, Jump jump) {
        return new Action(config, jumpIntegerMap.get(jump));
    }



    public static Jump getJump(Action action) {
        return integerJumpMap.get(action.getIndex());
    }
}
