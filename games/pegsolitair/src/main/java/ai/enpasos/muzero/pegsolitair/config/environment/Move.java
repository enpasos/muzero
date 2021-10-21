package ai.enpasos.muzero.pegsolitair.config.environment;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Move {
    List<Jump> jumps = new ArrayList<>();
}
