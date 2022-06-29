package ai.enpasos.muzero.platform.run;

import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.rational.SelfPlay;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@SuppressWarnings("squid:S106")
@Component
public class AltStarts {
    @Autowired
    MuZeroConfig config;

    @Autowired
    ReplayBuffer replayBuffer;

    @Autowired
    SelfPlay selfPlay;

    public void playGames(Network network, int trainingStep) {
        List<Game> games = replayBuffer.getBuffer().getGames().stream().filter(g -> g.isPlayedMoreThanOnce()).collect(Collectors.toList());
        if (games.isEmpty()) return;

        List<Game> gameSeeds = games.stream().map(game -> alternativeGameStart(game)).filter(g -> g != null).collect(Collectors.toList());

        selfPlay.replayGamesFromSeeds(network, gameSeeds);

        gameSeeds.clear();
        games.clear();

        replayBuffer.saveState();
    }

    public void reset() {
        this.replayBuffer.getBuffer().getGames().stream().forEach(game -> {
            game.setPlayedMoreThanOnce(false);
        });
    }

    @Data
    @AllArgsConstructor
    class StartOption {
        int t;
        int actionIndex;
    }

    private Game alternativeGameStart(Game game) {
        double pThreshold = config.getBadActionProbabilityThreshold();

        Game tmpGame = config.newGame();
        List<float[]> policyTargets = game.getGameDTO().getPolicyTargets();

        List<StartOption> options = new ArrayList<>();

        for (int t = 0; t < game.getGameDTO().getActions().size() - 1; t++) {
            float[] policyTarget = policyTargets.get(t);
            int[] legalActionsArray = tmpGame.legalActions().stream().mapToInt(a -> a.getIndex()).toArray();
            for (int a = 0; a < policyTarget.length; a++) {
                // if the action is legal and value of policyTarget is smaller than pThreshold
                if (ArrayUtils.contains(legalActionsArray, a) && policyTarget[a] < pThreshold) {
                    options.add(new StartOption(t, a));
                }
            }
            tmpGame.apply(game.getGameDTO().getActions().get(t));
        }
        if (options.size() == 0) {
            return null;
        }

        // select random option with random from thread local
        StartOption startOption = options.get(ThreadLocalRandom.current().nextInt(options.size()));

        Game seed = null;
        //      try {
        seed = game.copy(startOption.t);
        seed.apply(startOption.actionIndex);
        seed.getGameDTO().setTStateA(seed.getGameDTO().getActions().size());
        seed.getGameDTO().setTStateB(seed.getGameDTO().getActions().size());
        // dummy copy for the applied action

        //  seed.getGameDTO().getSurprises().add(game.getGameDTO().getSurprises().get(startOption.t + 1));
        seed.getGameDTO().getPolicyTargets().add(game.getGameDTO().getPolicyTargets().get(startOption.t + 1));
        //   seed.getGameDTO().getValues().add(game.getGameDTO().getValues().get(startOption.t + 1));
        seed.getGameDTO().getRootValuesFromInitialInference().add(game.getGameDTO().getRootValuesFromInitialInference().get(startOption.t + 1));

        return seed;
    }
}
