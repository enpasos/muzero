package ai.enpasos.muzero.go.config;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import ai.enpasos.muzero.go.config.environment.basics.move.Pass;
import ai.enpasos.muzero.go.config.environment.basics.move.Play;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.testng.annotations.Test;

public class GoAdapterTest {

    @Test
    void translatePass() {
        MuZeroConfig config = GoConfigFactory.getGoInstance(5);
        Action action = GoAdapter.translate(config, new Pass());

        NDArray ndArray = action.encode(NDManager.newBaseManager());
        int i = 42;
    }

    @Test
    void translateSomeAction() {
        MuZeroConfig config = GoConfigFactory.getGoInstance(5);
        Action action = GoAdapter.translate(config, new Play(new Point(1, 2)));
        NDArray ndArray = action.encode(NDManager.newBaseManager());
        int i = 42;
    }
}
