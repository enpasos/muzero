package ai.enpasos.muzero.environments.go.environment;


import static ai.enpasos.muzero.environments.go.environment.Player.BlackPlayer;

/**
 * adapted from https://github.com/maxpumperla/ScalphaGoZero
 */
public class GoBoardSerializer {

    public static String serialize(GoBoard board) {
        var s = "-".repeat(board.getSize()*3 + 3) + "\n";

        for (int i = 1; i <= board.getSize(); i++){
            var rowNum = board.getSize() + 1 - i;
            s +=  ((rowNum < 10) ? " " : "") + rowNum + " ";
            for (int j = 1; j <= board.getSize(); j++){
                var player = board.getPlayer(new Point(i, j));
                s +=   (player.isEmpty()) ? " . " : (player.get() == BlackPlayer ? " X " : " O ");
            }
            s += "\n";
        }
        s += "   " + X_COORD.substring(0, board.getSize()*3) + "\n";
        s += "-".repeat(board.getSize()*3 + 3);
        return s;
    }

    private static final String X_COORD = " A  B  C  D  E  F  G  H  I  J  K  L  M  N  O  P  Q  R  S  T  U  V  W  X  Y  Z ";

}

