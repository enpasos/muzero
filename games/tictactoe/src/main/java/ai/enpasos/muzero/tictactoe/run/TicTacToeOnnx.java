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
        long o = config.getNumObservationLayers();

        List<Shape> inputRepresentation = List.of(new Shape(1L, o, w, h));
        List<Shape> inputAction = List.of(new Shape(1L, config.getActionSpaceSize()));
        onnxExport.run(inputRepresentation, inputAction, -1);
    }

}
