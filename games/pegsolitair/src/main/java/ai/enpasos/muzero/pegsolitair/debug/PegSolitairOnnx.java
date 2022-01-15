package ai.enpasos.muzero.pegsolitair.debug;

import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.debug.OnnxExport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class PegSolitairOnnx {
    @Autowired
    MuZeroConfig config;

    @Autowired
    private OnnxExport onnxExport;

    public void run() {
        long w = config.getBoardWidth();
        long h = config.getBoardHeight();
        long hs = config.getNumHiddenStateChannels();
        long a = config.getNumActionLayers();
        long o = config.getNumObservationLayers();

        List<Shape> inputRepresentation = List.of(new Shape(1L,o,w,h));
        List<Shape> inputPrediction = List.of(new Shape(1L,hs,w,h));
        List<Shape> inputGeneration = List.of(new Shape(1L,hs,w,h), new Shape(1L, a,w,h));
        onnxExport.run(inputRepresentation, inputPrediction, inputGeneration);
    }

}
