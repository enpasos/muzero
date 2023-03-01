package ai.enpasos.muzero.platform.agent.rational.async;

import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Data
public class GlobalState {
    Network network;


    NDManager nDManager;

    @Autowired
    MuZeroConfig config;


}
