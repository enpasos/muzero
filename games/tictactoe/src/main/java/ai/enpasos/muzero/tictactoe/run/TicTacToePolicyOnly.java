package ai.enpasos.muzero.tictactoe.run;


import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.MuZero;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.TrainParams;
import ai.enpasos.muzero.tictactoe.run.test.TicTacToeTest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class TicTacToePolicyOnly {

    @Autowired
    private MuZeroConfig config;

    @Autowired
    private TicTacToeTest ticTacToeTest;

    @Autowired
    private MuZero muZero;

    public void run() {
try{
        muZero.train(TrainParams.builder()
            .render(true)
            .freshBuffer(true)
            .build());
} catch (Exception e) {
    e.printStackTrace();
}

    }


}
