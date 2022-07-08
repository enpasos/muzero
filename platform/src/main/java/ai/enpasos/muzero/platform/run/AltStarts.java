package ai.enpasos.muzero.platform.run;

import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.rational.Action;
import ai.enpasos.muzero.platform.agent.rational.SelfPlay;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
        double pThreshold = config.getBadActionProbabilityThreshold();
        List<Game> normalGames = replayBuffer.getBuffer().getGames().stream().filter(g -> g.getGameDTO().getTStateA() == 0).collect(Collectors.toList());

        final List<Game> gameSeeds = new ArrayList<>();
        normalGames.stream().forEach(game -> {
            List<StartOption> options = new ArrayList<>();
            Game tmpGame = config.newGame();
             for (int t = 0; t < Math.min(2, game.getGameDTO().getActions().size() - 1); t++) {

                Integer action = game.getGameDTO().getActions().get(t);
                float[] policyTarget = game.getGameDTO().getPolicyTargets().get(t);
                int[] legalActionsArray = tmpGame.legalActions().stream().mapToInt(Action::getIndex).toArray();
                for (int a = 0; a < policyTarget.length; a++) {
                    if (ArrayUtils.contains(legalActionsArray, a) && policyTarget[a] < pThreshold) {
                        options.add(new StartOption(t, a));
                    }
                }
                tmpGame.apply(action);
            }
            options.stream().forEach(startOption -> {
                Game seed = game.copy(startOption.t);

                    seed.apply(startOption.actionIndex);


                seed.getGameDTO().setTStateA(seed.getGameDTO().getActions().size());
                seed.getGameDTO().setTStateB(seed.getGameDTO().getActions().size());

                seed.getGameDTO().getSurprises().add(game.getGameDTO().getSurprises().get(startOption.t + 1));
                seed.getGameDTO().getPolicyTargets().add(game.getGameDTO().getPolicyTargets().get(startOption.t + 1));


                seed.getGameDTO().getRootValuesFromInitialInference().add(game.getGameDTO().getRootValuesFromInitialInference().get(startOption.t + 1));
                gameSeeds.add(seed);
            });

        });

        int n = (int) (config.getNumEpisodes() * config.getNumParallelGamesPlayed() * config.getVariableStartFraction());
        if (n == 0) return;
        List<Game> gameSeeds2 = null;
        Collections.shuffle(gameSeeds);
        if (n <  gameSeeds.size()) {
            gameSeeds2 = gameSeeds.subList(0, n);
        } else {
            gameSeeds2 = gameSeeds;
        }
        if (gameSeeds2.isEmpty()) return;

        selfPlay.replayGamesFromSeeds(network, gameSeeds2);

        gameSeeds.clear();
//        gameSeeds2.clear();
        normalGames.clear();

        replayBuffer.saveState();
    }


    @Data
    @AllArgsConstructor
    class StartOption {
        int t;
        int actionIndex;
    }


}
