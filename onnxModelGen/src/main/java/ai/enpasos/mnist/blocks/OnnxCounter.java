package ai.enpasos.mnist.blocks;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OnnxCounter {
    int counter;
    String prefix = "";

    public String count() {
        return prefix + counter++;
    }
}
