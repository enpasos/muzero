package ai.enpasos.muzero.go;

import ai.enpasos.muzero.platform.agent.intuitive.Inference;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBufferDTO;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;


@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@Disabled
class CompareStoredGamesByHand {

    @Autowired
    MuZeroConfig config;

    @Autowired
    private ReplayBuffer replayBuffer;

    @Test
    @Disabled
    void go() {
//       replayBuffer.loadState(984872);
//       ReplayBufferDTO dtoSmall = replayBuffer.getBuffer();
//
//        replayBuffer.loadState(965764);
//        ReplayBufferDTO dtoLarge = replayBuffer.getBuffer();
//
//        int i = 42;

    }




}
