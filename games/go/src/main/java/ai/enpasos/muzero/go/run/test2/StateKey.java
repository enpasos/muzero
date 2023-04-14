package ai.enpasos.muzero.go.run.test2;

import ai.enpasos.muzero.go.config.GoEnvironment;
import ai.enpasos.muzero.go.config.GoGame;
import ai.enpasos.muzero.go.config.environment.GameState;
import ai.enpasos.muzero.go.config.environment.basics.Player;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.tuple.Pair;

import static ai.enpasos.muzero.go.config.GoAdapter.translate;

@Data
public class StateKey {
    long zobristHash;
    int toPlay;




    public static StateKey getFrom(Game game) {
        GoEnvironment newEnvironment = (GoEnvironment) game.getEnvironment();
        return StateKey.instanceOf(newEnvironment.getState());
    }
    private static StateKey instanceOf(GameState state) {
        StateKey key = new StateKey();
        Pair<Player, Long> pair = null;
        if (state.getAllPreviousStates().size() > 0) {
            pair = state.getAllPreviousStates().get(state.getAllPreviousStates().size() - 1);
        } else {
            pair = Pair.of(state.getNextPlayer(), state.getBoard().zobristHash());
        }
        key.zobristHash = pair.getRight();
        key.toPlay = translate(pair.getLeft()).getValue();
        return key;
    }
}
