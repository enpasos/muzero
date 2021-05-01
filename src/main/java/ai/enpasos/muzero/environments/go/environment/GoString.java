package ai.enpasos.muzero.environments.go.environment;


import lombok.Builder;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

/**
 * A Go string is a collection of stones of given color and corresponding liberties
 * <p>
 * adapted from https://github.com/maxpumperla/ScalphaGoZero
 */
@Data
@Builder
public class GoString {

    private Player player;
    private Set<Point> stones;
    private Set<Point> liberties;


    GoString(Player player, Set<Point> stones, Set<Point> liberties) {
        this.player = player;
        this.stones = stones;
        this.liberties = liberties;
    }

    GoString(Player player) {
        this(player, new TreeSet<>(), new TreeSet<>());
    }

    int size() {
        return stones.size();
    }

    int numLiberties() {
        return liberties.size();
    }

    GoString withoutLiberty(Point point) {
        Set<Point> newLiberties = new TreeSet<Point>(this.liberties);
        newLiberties.remove(point);
        return new GoString(player, stones, newLiberties);
    }

    GoString withLiberty(Point point) {
        Set<Point> newLiberties = new TreeSet<>(this.liberties);
        newLiberties.add(point);
        return new GoString(player, stones, newLiberties);
    }

    GoString mergedWith(GoString goString) {
        if (!player.equals(goString.player))
            throw new IllegalArgumentException("Color of Go strings has to match");
        Set<Point> combinedStones = new TreeSet<>(stones);
        combinedStones.addAll(goString.stones);
        Set<Point> commonLiberties = new TreeSet<>(this.liberties);
        commonLiberties.addAll(goString.liberties);
        commonLiberties.removeAll(combinedStones);
        return new GoString(player, combinedStones, commonLiberties);
    }


}
