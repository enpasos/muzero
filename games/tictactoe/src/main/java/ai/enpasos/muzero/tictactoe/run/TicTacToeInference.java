package ai.enpasos.muzero.tictactoe.run;

import ai.enpasos.muzero.platform.agent.c_model.Inference;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
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

        int action = inference.aiDecision(List.of(), false, dir);

        log.info("action: " + action);
    }

}
