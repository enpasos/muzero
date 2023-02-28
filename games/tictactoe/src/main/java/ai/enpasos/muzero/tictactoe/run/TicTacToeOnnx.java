package ai.enpasos.muzero.tictactoe.run;

import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.OnnxExport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class TicTacToeOnnx {
    @Autowired
    MuZeroConfig config;

    @Autowired
    private OnnxExport onnxExport;

    public void run() {
        long w = config.getBoardWidth();
        long h = config.getBoardHeight();
        long hs = config.getNumChannels();
        long a = config.getNumActionLayers();
        long o = config.getNumObservationLayers();
        long s = config.getNumChannelsOutputLayerSimilarity();

        List<Shape> inputRepresentation = List.of(new Shape(1L, o, w, h));
        List<Shape> inputPrediction = List.of(new Shape(1L, hs, w, h));
        List<Shape> inputSimilarityProjection = List.of(new Shape(1L, hs, w, h));
        List<Shape> inputSimilarityPrediction = List.of(new Shape(1L, s));
        List<Shape> inputGeneration = List.of(new Shape(1L, hs, w, h), new Shape(1L, a, w, h));
        onnxExport.run(inputRepresentation, inputPrediction, inputGeneration, inputSimilarityPrediction, inputSimilarityProjection, -1);
    }

}
