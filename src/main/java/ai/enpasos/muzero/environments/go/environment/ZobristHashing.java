package ai.enpasos.muzero.environments.go.environment;

import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class ZobristHashing {

    private ZobristHashing() {
        // hide
    }

    final public static int MAX_BOARD_SIZE = 19;
    final public static Random RAND = new Random();
    final public static Map<Pair<Point, Optional<Player>>, Long> ZOBRIST = new HashMap<>();
    static {
        List<Optional<Player>> players = new ArrayList<>();
        players.add(Optional.of(Player.BlackPlayer));
        players.add(Optional.of(Player.WhitePlayer));
        players.add(Optional.empty());
        for (int row = 1; row <= MAX_BOARD_SIZE; row++) {
            for (int col = 1; col <= MAX_BOARD_SIZE; col++) {
                for (Optional<Player> optionalPlayer : players) {
                    Long code = RAND.nextLong();
                    ZOBRIST.put(Pair.of(new Point(row, col), optionalPlayer), code);
                }
            }
        }
    }


}
