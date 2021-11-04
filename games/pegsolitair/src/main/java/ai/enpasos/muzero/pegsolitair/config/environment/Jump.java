package ai.enpasos.muzero.pegsolitair.config.environment;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Jump {
    Point fromPoint;
    Direction direction;


}
