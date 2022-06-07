/*
 *  Copyright (c) 2021 enpasos GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ai.enpasos.muzero.platform.run.train;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.training.DefaultTrainingConfig;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.rational.SelfPlay;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.Surprise;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MuZeroFast2 {

    @Autowired
    MuZeroConfig config;

    @Autowired
    SelfPlay selfPlay;


    @Autowired
    MuZero muzero;

    @Autowired
    ReplayBuffer replayBuffer;

    @Autowired
    Surprise surprise;

    @Autowired
    NetworkHelper networkHelper;


    public void train(TrainParams params) {
        try (Model model = Model.newInstance(config.getModelName(), Device.gpu())) {
            Network network = new Network(config, model);

            muzero.init(params.freshBuffer, params.randomFill, network, params.withoutFill);

            int epoch = networkHelper.getEpoch();
            int trainingStep = config.getNumberOfTrainingStepsPerEpoch() * epoch;
            DefaultTrainingConfig djlConfig = networkHelper.setupTrainingConfig(epoch);

            List<Game> games = this.replayBuffer.getBuffer().getGames();


            int loop = 0;

            double surpriseThreshold = Double.MAX_VALUE;

            do {
                log.info("*>*>*>* looping " + loop++);

                log.info("start surprise.measureValueAndSurprise");
                surprise.measureValueAndSurprise(network, games);
                replayBuffer.saveState();
                log.info("end surprise.measureValueAndSurprise");

                log.info(loop + " >>> 1. find surprise threshold as 3*sigma surprise");
                surpriseThreshold = surprise.getSurpriseThreshold(games);


                log.info(loop + " >>> 2. for each game with a surprise beyond threshold, mark the surprise beyond threshold that is the latest time");

                List<Game> gamesWithSurprise = identifyGamesWithSurprise(games, surpriseThreshold);
                log.info(loop + " >>> 3. Train all games (but not on timesteps before t-2)");


                trainingStep = muzero.trainNetwork(params.numberOfEpochs, model, djlConfig);


                log.info(loop + " >>> 4. Reevaluate surprises on all games");
                log.info("start surprise.measureValueAndSurprise");
                surprise.measureValueAndSurprise(network, games);
                replayBuffer.saveState();

                gamesWithSurprise = identifyGamesWithSurprise(games, surpriseThreshold);
                log.info("end surprise.measureValueAndSurprise");

                log.info(loop + " >>> 5. Replay the 1000 games with the highest marked surprise according to convolution for gameplay. Higher temperature and higher simulationNum at hotspot");
                gamesWithSurprise.sort(Comparator.comparing(
                    game -> game.getGameDTO().getSurprises().get((int) game.getGameDTO().getTSurprise())
                ));
                Collections.reverse(gamesWithSurprise);
                List<Game> gamesToReplay = gamesWithSurprise.stream().limit(1000).collect(Collectors.toList());
                games.stream().forEach(game ->
                    game.getGameDTO().setSurprised(false)

                );
                gamesToReplay.stream().forEach(game ->
                    game.getGameDTO().setSurprised(true)
                );
                surprise.handleOldSurprises(network);


                log.info(loop + " >>> 6. Play 1000 new games from start with normal temperature and simulationNum. And train all games.");
                muzero.playGames(params.render, network, trainingStep);
                trainingStep = muzero.trainNetwork(params.numberOfEpochs, model, djlConfig);

            } while (surpriseThreshold > 10);


        }
    }

    @NotNull
    private List<Game> identifyGamesWithSurprise(List<Game> games, double surpriseThreshold) {
        List<Game> gamesWithSurprise = surprise.getGamesWithSurprisesAboveThreshold(games, surpriseThreshold);


        games.stream().forEach(game -> {
            game.getGameDTO().setSurprised(false);
            game.getGameDTO().setTStateA(0);
            game.getGameDTO().setTStateB(0);
        });
        gamesWithSurprise.stream().forEach(game -> {
            game.getGameDTO().setSurprised(true);
            long t = game.getGameDTO().getTSurprise();
            long t0 = Math.max(t - 3, 0);
            game.getGameDTO().setTStateA(t0);
            game.getGameDTO().setTStateB(t0);
        });
        return gamesWithSurprise;
    }


}
