package ai.enpasos.muzero.environments.go.environment;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Play extends Move {
    private Point point;

    public static Play apply(int row, int col) {
        return new Play(new Point(row, col));
    }
}
