package ai.enpasos.muzero.platform.agent.e_experience;


import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RuleBufferService {


    @Autowired
    MuZeroConfig config;

    @Autowired
    GameBuffer gameBuffer;


    public void run() {
        log.info("RuleBufferService.run");



    }
}
