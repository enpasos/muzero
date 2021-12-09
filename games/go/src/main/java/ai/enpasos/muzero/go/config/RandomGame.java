package ai.enpasos.muzero.go.config;

import ai.enpasos.muzero.go.config.environment.GameState;
import ai.enpasos.muzero.go.config.environment.basics.move.Move;
import ai.enpasos.muzero.go.config.environment.basics.move.Pass;
import ai.enpasos.muzero.go.config.environment.basics.move.Play;
import ai.enpasos.muzero.go.config.environment.basics.move.Resign;
import ai.enpasos.muzero.go.config.environment.scoring.GameResult;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class RandomGame {

    @Autowired
    MuZeroConfig config;


    public void run() {
        var state = GameState.newGame(5);
        log.info(state.getBoard().toString());

        List<Move> validMoves = state.getValidMoves();

        while (!validMoves.isEmpty()) {
            Collections.shuffle(validMoves);
            Move selectedMove = validMoves.get(0);


            state = state.applyMove(selectedMove);


            if (selectedMove instanceof Play) {
                log.info("\n" + state.getBoard().toString());
            } else if (selectedMove instanceof Pass) {
                log.info("Pass");
            } else if (selectedMove instanceof Resign) {
                log.info("Resign");
            }

            validMoves = state.getValidMoves();
        }

        log.info("*** G A M E   O V E R ***");

        var result = GameResult.apply(state.getBoard(), (float)config.getKomi());
        log.info("result = " + result + "\n" + result.toDebugString());


    }


}
