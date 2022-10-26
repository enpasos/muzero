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

package ai.enpasos.muzero.platform.agent.memorize;


import ai.djl.Device;
import ai.djl.Model;
import ai.djl.ndarray.NDManager;
import ai.djl.util.Utils;
import ai.enpasos.muzero.platform.agent.intuitive.Observation;
import ai.enpasos.muzero.platform.agent.intuitive.Sample;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


@Data
@Slf4j
@Component
public class ReplayBuffer {


    private int batchSize;
    private ReplayBufferDTO buffer;


    public int getMaxGameLength() {
        return (int) getBuffer().getGames().stream().mapToInt(g -> g.getGameDTO().getActions().size()).max().orElse(1000);
    }

    private String currentNetworkName = "NONE";
    @Autowired
    private MuZeroConfig config;

    @Autowired
    private ReplayBufferIO replayBufferIO;

    public static @NotNull Sample sampleFromGame(int numUnrollSteps,  @NotNull Game game, NDManager ndManager, ReplayBuffer replayBuffer) {
        int gamePos = samplePosition(game);
        return sampleFromGame(numUnrollSteps,   game, gamePos, ndManager, replayBuffer);
    }

    public static @NotNull Sample sampleFromGame(int numUnrollSteps,   @NotNull Game game, int gamePos, NDManager ndManager, ReplayBuffer replayBuffer) {
        Sample sample = new Sample();
        game.replayToPosition(gamePos);

        sample.getObservations().add(game.getObservation(ndManager));

        List<Integer> actions = new ArrayList<>(game.getGameDTO().getActions());
        int originalActionSize = actions.size();
        if (actions.size() < gamePos + numUnrollSteps) {
            actions.addAll(game.getRandomActionsIndices(gamePos + numUnrollSteps - actions.size()));
        }
        sample.setActionsList(new ArrayList<>());
        Observation lastObservation = null;
        for (int i = 0; i < numUnrollSteps; i++) {
            int actionIndex = actions.get(gamePos + i);
            sample.getActionsList().add(actionIndex);

            if (gamePos + i < originalActionSize) {
                game.replayToPosition(gamePos + i);
                lastObservation = game.getObservation(ndManager);
            }
            sample.getObservations().add(lastObservation);


        }

        sample.setTargetList(game.makeTarget(gamePos, numUnrollSteps ));

        return sample;
    }

    public static int samplePosition(@NotNull Game game) {
        GameDTO dto = game.getGameDTO();
        int numActions = dto.getActions().size();
        long delta = dto.getTStateB() - dto.getTStateA();
        if (delta < 0) delta = 0L;
        int enhanceFactor = 1;
        long numNormalActions = numActions - (dto.getTStateA() + delta);
        int n = (int) (enhanceFactor * (delta) + numNormalActions);

        int rawpos = ThreadLocalRandom.current().nextInt(0, n);
        int gamePos = 0;
        if (rawpos < numNormalActions) {
            gamePos = (int) (rawpos + dto.getTStateA() + delta);
        } else {
            rawpos -= numNormalActions;
            gamePos = rawpos;
        }
        if (dto.getTStateA() > 0 && gamePos < dto.getTStateA()) {
            throw new MuZeroException("gamePos < dto.getTStateA()");
        }
        return gamePos;
    }


    public void createNetworkNameFromModel(Model model, String modelName, String outputDir) {
        String epochValue = model.getProperty("Epoch");
        Path modelPath = Paths.get(outputDir);
        int epoch = 0;
        try {
            epoch = epochValue == null ? Utils.getCurrentEpoch(modelPath, modelName) + 1 : Integer.parseInt(epochValue);
        } catch (IOException e) {
            throw new MuZeroException(e);
        }
        this.currentNetworkName = String.format(Locale.ROOT, "%s-%04d", modelName, epoch);
    }

    @PostConstruct
    public void postConstruct() {
        init();
    }

    public void init() {
        this.batchSize = config.getBatchSize();
        if (this.buffer != null) {
            this.buffer.games.forEach(g -> {
                g.setGameDTO(null);
                g.setOriginalGameDTO(null);
            });
            this.buffer.games.clear();
        }
        this.buffer = new ReplayBufferDTO(config);
    }


    /**
     * @param numUnrollSteps number of actions taken after the chosen position (if there are any)
     */
    public List<Sample> sampleBatch(int numUnrollSteps) {
        try (NDManager ndManager = NDManager.newBaseManager(Device.cpu())) {
            return sampleGames().stream()
                .filter(game -> game.getGameDTO().getTStateA() < game.getGameDTO().getActions().size())
                .filter(game -> game.getGameDTO().getTHybrid() < game.getGameDTO().getActions().size())
                .map(game -> sampleFromGame(numUnrollSteps, game, ndManager, this))
                .collect(Collectors.toList());
        }
    }


    // for "fair" training do train the strengths of PlayerA and PlayerB equally
    public List<Game> sampleGames() {

        List<Game> games = new ArrayList<>(this.buffer.getGames());
        Collections.shuffle(games);


        List<Game> gamesToTrain = games.stream()
            .filter(g -> {
                if (g instanceof ZeroSumGame) {
                    Optional<OneOfTwoPlayer> winner = ((ZeroSumGame) g).whoWonTheGame();
                    return winner.isEmpty() || winner.get() == OneOfTwoPlayer.PLAYER_A;
                } else {
                    return true;
                }
            })
            .limit(this.batchSize / 2).collect(Collectors.toList());
        games.removeAll(gamesToTrain);  // otherwise, draw games could be selected again
        gamesToTrain.addAll(games.stream()
            .filter(g -> {
                if (g instanceof ZeroSumGame) {
                    Optional<OneOfTwoPlayer> winner = ((ZeroSumGame) g).whoWonTheGame();
                    return winner.isEmpty() || winner.get() == OneOfTwoPlayer.PLAYER_B;
                } else {
                    return true;
                }
            })
            .limit(this.batchSize / 2)
            .collect(Collectors.toList()));

        return gamesToTrain;
    }


    public void saveState() {
        replayBufferIO.saveState(this.buffer, this.currentNetworkName);
    }


    public void loadLatestState() {
        ReplayBufferDTO replayBufferDTO = this.replayBufferIO.loadLatestState();
        if (replayBufferDTO != null) {
            this.buffer = replayBufferDTO;
        }
    }


    public void sortGamesByLastValueError() {
        this.getBuffer().getGames().sort(
            (Game g1, Game g2) -> Float.compare(g2.getError(), g1.getError()));
    }


    public void removeGames(List<Game> games) {
        games.forEach(this::removeGame);
    }

    public void removeGame(Game game) {
        this.buffer.removeGame(game);
    }

    public double getPRandomActionRawAverage() {
        double sum = this.buffer.games.stream().mapToDouble(g -> g.getGameDTO().pRandomActionRawSum).sum();
        long count = this.buffer.games.stream().mapToLong(g -> g.getGameDTO().pRandomActionRawCount).sum();
        return sum / count;
    }

    public void addGames(Model model, List<Game> games) {
        games.forEach(game ->  addGame(model, game));
    }

    private void addGame(Model model,  Game game) {
        String epochStr = model.getProperty("Epoch");
        if (epochStr == null) {
            epochStr = "0";
        }
        int epoch = Integer.parseInt(epochStr);
        game.getGameDTO().setNetworkName(this.currentNetworkName);
        if (config.isRecordVisitsOn()) {
            this.buffer.getNodeDTO().memorize(game, epoch);
        }
        buffer.addGame(game);
    }


    private Map<Integer, Double> meanValuesLosses = new HashMap<>();
    public void putMeanValueLoss(int epoch, double meanValueLoss) {
        meanValuesLosses.put(epoch, meanValueLoss);
    }

    public Double getMaxMeanValueLoss() {
        // get max of meanValueLosses values
        return meanValuesLosses.values().stream().max(Double::compare).orElse(0.0);
    }

    public double getDynamicRootTemperature() {
        return config.getTemperatureRoot();

    }
}
