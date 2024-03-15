package ai.enpasos.muzero.tictactoe.run;


import ai.enpasos.muzero.platform.run.FillRulesLoss;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TicTacToeFillRewardLoss {

    @Autowired
    FillRulesLoss fillRulesLoss;

    public void run() {
        fillRulesLoss.run();
    }
}
