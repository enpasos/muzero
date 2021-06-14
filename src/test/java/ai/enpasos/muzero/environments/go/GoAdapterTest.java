package ai.enpasos.muzero.environments.go;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.agent.slow.play.Action;
import ai.enpasos.muzero.environments.go.environment.Pass;
import ai.enpasos.muzero.environments.go.environment.Play;
import ai.enpasos.muzero.environments.go.environment.Point;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GoAdapterTest {

    @Test
    void translatePass() {
        MuZeroConfig config = MuZeroConfig.getGoInstance(5);
        Action action = GoAdapter.translate(config, new Pass());

        NDArray ndArray = action.encode(NDManager.newBaseManager());
        int i = 42;
    }

    @Test
    void translateSomeAction() {
        MuZeroConfig config = MuZeroConfig.getGoInstance(5);
        Action action = GoAdapter.translate(config, new Play(new Point(1, 2)));
        NDArray ndArray = action.encode(NDManager.newBaseManager());
        int i = 42;
    }
}
