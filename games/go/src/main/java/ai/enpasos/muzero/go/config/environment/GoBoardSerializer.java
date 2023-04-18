package ai.enpasos.muzero.go.config.environment;


import ai.enpasos.muzero.go.config.environment.basics.Point;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BLACK_PLAYER;

/**
 * adapted from <a href="https://github.com/maxpumperla/ScalphaGoZero">...</a>
 */
public class GoBoardSerializer {

    private static final String X_COORD = " A  B  C  D  E  F  G  H  I  J  K  L  M  N  O  P  Q  R  S  T  U  V  W  X  Y  Z ";

    private GoBoardSerializer() {
    }

    public static String serialize(GoBoard board) {
        StringBuilder s = new StringBuilder("-".repeat(board.getSize() * 3 + 3) + "\n");

        for (int i = board.getSize(); i > 0; i--) {
            s.append((i < 10) ? " " : "").append(i).append(" ");
            for (int j = 1; j <= board.getSize(); j++) {
                var player = board.getPlayer(new Point(i, j));
                String symbol;
                symbol = player.map(value -> value == BLACK_PLAYER ? " X " : " O ").orElse(" . ");
                s.append(symbol);
            }
            s.append("\n");
        }
        s.append("   ").append(X_COORD, 0, board.getSize() * 3).append("\n");
        s.append("-".repeat(board.getSize() * 3 + 3));
        return s.toString();
    }

}

