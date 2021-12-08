package ai.enpasos.muzero.go.config.environment;


import ai.enpasos.muzero.go.config.environment.basics.Point;

import static ai.enpasos.muzero.go.config.environment.basics.Player.BlackPlayer;

/**
 * adapted from https://github.com/maxpumperla/ScalphaGoZero
 */
public class GoBoardSerializer {

    private GoBoardSerializer() {}

    private static final String X_COORD = " A  B  C  D  E  F  G  H  I  J  K  L  M  N  O  P  Q  R  S  T  U  V  W  X  Y  Z ";

    public static String serialize(GoBoard board) {
        StringBuilder s = new StringBuilder("-".repeat(board.getSize() * 3 + 3) + "\n");

        for (int i = 1; i <= board.getSize(); i++) {
            var rowNum = board.getSize() + 1 - i;
            s.append((rowNum < 10) ? " " : "").append(rowNum).append(" ");
            for (int j = 1; j <= board.getSize(); j++) {
                var player = board.getPlayer(new Point(i, j));
                String symbol;
                if(player.isPresent()) {
                    symbol = player.get() == BlackPlayer ? " X " : " O ";
                } else {
                    symbol = " . ";
                }
                s.append(symbol);
            }
            s.append("\n");
        }
        s.append("   ").append(X_COORD, 0, board.getSize() * 3).append("\n");
        s.append("-".repeat(board.getSize() * 3 + 3));
        return s.toString();
    }

}

