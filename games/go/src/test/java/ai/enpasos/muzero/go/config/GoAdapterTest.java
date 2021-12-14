package ai.enpasos.muzero.go.config;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import ai.enpasos.muzero.go.config.environment.basics.move.Pass;
import ai.enpasos.muzero.go.config.environment.basics.move.Play;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

@SpringBootTest()
public class GoAdapterTest {


    @Autowired
    MuZeroConfig config;

    @Test
    @Ignore
    void translatePass() {
        Action action = GoAdapter.translate(config, new Pass());

        NDArray ndArray = action.encode(NDManager.newBaseManager());
    }

    @Test
    @Ignore
    void translateSomeAction() {
        Action action = GoAdapter.translate(config, new Play(new Point(1, 2)));
        NDArray ndArray = action.encode(NDManager.newBaseManager());
    }
}
