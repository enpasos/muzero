package ai.enpasos.muzero.pegsolitair.config.environment;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
public class Move {
    List<Jump> jumps = new ArrayList<>();

    public Point getFinalPosition() {
        return jumps.get(jumps.size() - 1).getToPoint();
    }

    public Move clone() {
        Move move = new Move(new ArrayList<Jump>());
        move.jumps.addAll(jumps);
        return move;
    }
}
