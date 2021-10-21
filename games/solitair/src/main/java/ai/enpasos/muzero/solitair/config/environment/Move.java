package ai.enpasos.muzero.solitair.config.environment;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Move {
    List<Jump> jumps = new ArrayList<>();
}
