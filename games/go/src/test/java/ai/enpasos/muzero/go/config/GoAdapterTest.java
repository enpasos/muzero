package ai.enpasos.muzero.go.config;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.go.config.environment.basics.Point;
import ai.enpasos.muzero.go.config.environment.basics.move.Pass;
import ai.enpasos.muzero.go.config.environment.basics.move.Play;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@Disabled
class GoAdapterTest {


    @Autowired
    MuZeroConfig config;

    @Test
    void translatePass() {
        Action action = GoAdapter.translate(config, new Pass());
        NDArray ndArray = action.encode(NDManager.newBaseManager());
        assertNotNull(ndArray);
    }

    @Test
    void translateSomeAction() {
        Action action = GoAdapter.translate(config, new Play(new Point(1, 2)));
        NDArray ndArray = action.encode(NDManager.newBaseManager());
        assertNotNull(ndArray);
    }
}
