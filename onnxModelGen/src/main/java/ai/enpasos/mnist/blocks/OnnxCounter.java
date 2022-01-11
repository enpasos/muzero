package ai.enpasos.mnist.blocks;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class OnnxCounter {
    int counter;


    public int count() {
        return counter++;
    }
}
