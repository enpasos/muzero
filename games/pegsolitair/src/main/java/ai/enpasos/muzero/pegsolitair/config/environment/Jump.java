package ai.enpasos.muzero.pegsolitair.config.environment;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Jump {
    Point fromPoint;
    Direction direction;

    public Point getToPoint() {
        return fromPoint.pointIn(direction).pointIn(direction);
    }

}
