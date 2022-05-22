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
import ai.djl.metric.Metric;
import ai.djl.metric.Metrics;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.EasyTrain;
import ai.djl.training.Trainer;
import ai.djl.training.dataset.Batch;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.djl.MySaveModelTrainingListener;
import ai.enpasos.muzero.platform.agent.intuitive.djl.NetworkHelper;
import ai.enpasos.muzero.platform.agent.intuitive.djl.blocks.atraining.MuZeroBlock;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.rational.SelfPlay;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.run.Surprise;
import ai.enpasos.muzero.platform.run.ValueSelfconsistency;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

import static ai.enpasos.muzero.platform.agent.intuitive.djl.NetworkHelper.getEpochFromModel;
import static ai.enpasos.muzero.platform.common.Constants.TRAIN_ALL;

@Slf4j
@Component
public class MuZeroFast {

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

            log.info("start surprise.measureValueAndSurprise");
            surprise.measureValueAndSurprise(network, games);
            replayBuffer.saveState();
            log.info("end surprise.measureValueAndSurprise");

            double surpriseThreshold = surprise.getSurpriseThreshold(games);

          //  long surpriseNum = games.stream().mapToLong(g -> g.getGameDTO().getSurprises().size()).sum();

            List<Game> gamesWithSurprisesAboveQuantilHere = null;
            List<Game> gamesWithSurprisesAboveQuantil = null;
            int loop = 0;
            do {
                log.info("*>*>*>* looping " + loop++);
                int backInTime = 1;

                Pair<List<Game>, List<Game>> gameListPair = surprise.getGamesWithSurprisesAboveThreshold(games, surpriseThreshold, backInTime);
                gamesWithSurprisesAboveQuantilHere = gameListPair.getLeft();
                gamesWithSurprisesAboveQuantil = gameListPair.getRight();

                for (; gamesWithSurprisesAboveQuantil.size() > games.size() * 0.001; backInTime++) {
                    //  for (; backInTime < 50 && !gamesWithSurprisesAboveQuantil.isEmpty(); backInTime++) {
                    log.info("<<< backInTime: " + backInTime);
                    markTStateA(backInTime);

                    int n = 1;
                   // surpriseNum / 1000d * 3d
                    for (int i = 0; i < n && gamesWithSurprisesAboveQuantilHere.size() > 0; i++) {
                        log.info("iteration: " + i + " of " + n);
                        if (!(i == 0 && backInTime == 1)) {
                            gameListPair =
                            surprise.getGamesWithSurprisesAboveThreshold(games, surpriseThreshold, backInTime);
                            gamesWithSurprisesAboveQuantilHere = gameListPair.getLeft();
                            gamesWithSurprisesAboveQuantil = gameListPair.getRight();
                        }
                        trainingStep =  muzero.trainNetwork(params.numberOfEpochs, model, djlConfig);
                        surprise.measureValueAndSurprise(network, gamesWithSurprisesAboveQuantilHere, backInTime);

                        gameListPair = surprise.getGamesWithSurprisesAboveThreshold(games, surpriseThreshold, backInTime);
                        gamesWithSurprisesAboveQuantilHere = gameListPair.getLeft();
                        gamesWithSurprisesAboveQuantil = gameListPair.getRight();


                        replayBuffer.saveState();
                        // log.info("replayBuffer size (before handleOldSurprises): " + replayBuffer.getBuffer().getData().size());
                        checkAssumptionsForGames();
                        surprise.handleOldSurprises(network);
                        checkAssumptionsForGames();
                        //  log.info("replayBuffer size (after handleOldSurprises): " + replayBuffer.getBuffer().getData().size());
                        replayBuffer.saveState();
                    }
                }

                log.info("start surprise.measureValueAndSurprise");
                surprise.measureValueAndSurprise(network, games);
                replayBuffer.saveState();
                log.info("end surprise.measureValueAndSurprise");

            } while (gamesWithSurprisesAboveQuantil.size() > games.size() * 0.001);


        }
    }

    private void checkAssumptionsForGames() {
        this.replayBuffer.getBuffer().getGames().stream().forEach(game -> game.checkAssumptions());
    }

    private void markTStateA(int backInTime) {
        this.replayBuffer.getBuffer().getData().forEach(gameDTO -> {
            int t = Math.max(0, gameDTO.getActions().size() - 1 - backInTime);
            gameDTO.setTStateA(t);
        });
    }



}
