package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.agent.d_model.ObservationModelInput;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.EnvironmentZeroSumBase;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public abstract class ZeroSumGame extends Game {
    protected ZeroSumGame(@NotNull MuZeroConfig config) {
        super(config);
    }

    protected ZeroSumGame(@NotNull MuZeroConfig config, GameDTO gameDTO) {
        super(config, gameDTO);
    }







    @Override
    public EnvironmentZeroSumBase getEnvironment() {
        if (!(environment instanceof EnvironmentZeroSumBase)) {
            throw new MuZeroException("Environment is expected to be of type EnvironmentZeroSumBase");
        }
        return (EnvironmentZeroSumBase) environment;
    }
}
