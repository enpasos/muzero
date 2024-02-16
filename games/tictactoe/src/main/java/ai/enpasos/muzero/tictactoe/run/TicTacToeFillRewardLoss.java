package ai.enpasos.muzero.tictactoe.run;


import ai.enpasos.muzero.platform.run.FillRewardLoss;
import ai.enpasos.muzero.platform.run.FillValueTable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TicTacToeFillRewardLoss {

    @Autowired
    FillRewardLoss fillRewardLoss;

    public void run() {
        fillRewardLoss.run();
    }
}
