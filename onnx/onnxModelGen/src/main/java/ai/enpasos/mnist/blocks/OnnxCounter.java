package ai.enpasos.mnist.blocks;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OnnxCounter {
    int counter;
    @Builder.Default
    String prefix = "";

    public String count() {
        return prefix + counter++;
    }
}
