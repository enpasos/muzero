package ai.enpasos.muzero.tictactoe.debug;

import ai.enpasos.muzero.platform.agent.Inference;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.debug.ValueExtractor;
import ai.enpasos.muzero.tictactoe.TicTacToeInferenceHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class TicTacToeInference {
    @Autowired
    MuZeroConfig config;

    @Autowired
    Inference inference;

    public void run() {

        String dir = config.getOutputDir() + "/networks";

        int action  = inference.aiDecision(List.of(new Integer[0]), false ,dir);

        log.info("action: " + action);
    }

}
