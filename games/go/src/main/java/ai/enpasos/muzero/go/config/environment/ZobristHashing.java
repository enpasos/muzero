package ai.enpasos.muzero.go.config.environment;

import ai.enpasos.muzero.go.config.environment.basics.Player;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;

public class ZobristHashing {

    public static final int MAX_BOARD_SIZE = 19;
    public static final Random RAND = new Random();
    protected static final Map<Pair<Point, Optional<Player>>, Long> ZOBRIST = new HashMap<>();

    static {
        List<Optional<Player>> players = new ArrayList<>();
        players.add(Optional.of(Player.BLACK_PLAYER));
        players.add(Optional.of(Player.WHITE_PLAYER));
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

    private ZobristHashing() {
        // hide
    }


}
