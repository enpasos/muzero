package ai.enpasos.muzero.pegsolitair;

import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBufferDTO;
import ai.enpasos.muzero.platform.config.FileType;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@Disabled
@Slf4j
class IOTest {

    @Autowired
    MuZeroConfig config;


    @Test
    void loadTest() throws IOException {

        config.setOutputDir("./src/test/resources/");
        ReplayBuffer replayBuffer1 = new ReplayBuffer();
        replayBuffer1.setConfig(config);
        replayBuffer1.init();
        replayBuffer1.loadLatestState();
        config.setOutputDir("./target/");
        FileUtils.deleteDirectory(new File("./target/games/"));
        FileUtils.forceMkdir(new File("./target/games/"));
        ReplayBufferDTO dto1 = replayBuffer1.getBuffer();

        config.setGameBufferWritingFormat(FileType.ZIPPED_PROTOCOL_BUFFERS);
        replayBuffer1.getBuffer().setCounter(replayBuffer1.getBuffer().getCounter() + 1);
        replayBuffer1.saveState();


        ReplayBuffer replayBuffer = new ReplayBuffer();
        replayBuffer.setConfig(config);
        replayBuffer.loadLatestState();
        ReplayBufferDTO dto2 = replayBuffer.getBuffer();

        assertEquals(dto1, dto2);

        replayBuffer.getBuffer().setCounter(replayBuffer1.getBuffer().getCounter() + 1);
        replayBuffer.saveState();

        replayBuffer.loadLatestState();
        dto2 = replayBuffer.getBuffer();

        log.info(Arrays.toString(dto1.getData().get(0).getPolicyTargets().get(0)));
        log.info(Arrays.toString(dto2.getData().get(0).getPolicyTargets().get(0)));
        assertTrue(Arrays.equals(dto1.getData().get(0).getPolicyTargets().get(0), dto2.getData().get(0).getPolicyTargets().get(0)));


    }


}
