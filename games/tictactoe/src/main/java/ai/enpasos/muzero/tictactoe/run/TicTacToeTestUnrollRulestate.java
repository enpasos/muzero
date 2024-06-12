package ai.enpasos.muzero.tictactoe.run;


import ai.enpasos.muzero.platform.run.TestUnrollRulestate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TicTacToeTestUnrollRulestate {

    @Autowired
    TestUnrollRulestate testUnrollRulestate;

    public void run() {
      //  testUnrollRulestate.run();

        testUnrollRulestate.runOneGame(999);
    }
}
