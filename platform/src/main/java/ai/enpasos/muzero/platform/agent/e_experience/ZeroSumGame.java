package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.EnvironmentZeroSumBase;
import org.jetbrains.annotations.NotNull;

public abstract class ZeroSumGame extends Game {
    protected ZeroSumGame(@NotNull MuZeroConfig config) {
        super(config);
    }

    protected ZeroSumGame(@NotNull MuZeroConfig config, EpisodeDO episodeDO) {
        super(config, episodeDO);
    }







    @Override
    public EnvironmentZeroSumBase getEnvironment() {
        if (!(environment instanceof EnvironmentZeroSumBase)) {
            throw new MuZeroException("Environment is expected to be of type EnvironmentZeroSumBase");
        }
        return (EnvironmentZeroSumBase) environment;
    }
}
