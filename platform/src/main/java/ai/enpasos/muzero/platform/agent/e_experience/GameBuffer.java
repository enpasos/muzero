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

package ai.enpasos.muzero.platform.agent.e_experience;

import ai.djl.Device;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.d_model.ModelState;
import ai.enpasos.muzero.platform.agent.d_model.ObservationModelInput;
import ai.enpasos.muzero.platform.agent.d_model.Sample;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Data
@Slf4j
@Component
public class GameBuffer {

    public static final String EPOCH_STR = "Epoch";

    @Autowired
    private ModelState modelState;

    private int batchSize;
    private GameBufferDTO buffer;
    @Autowired
    private MuZeroConfig config;

    @Autowired
    private GameBufferIO gameBufferIO;
    private Map<Integer, Double> meanValuesLosses = new HashMap<>();
    private Map<Integer, Double> meanEntropyValuesLosses = new HashMap<>();
    private Map<Integer, Double> entropyExplorationSum = new HashMap<>();
    private Map<Integer, Integer> entropyExplorationCount = new HashMap<>();
    private Map<Integer, Double> maxEntropyExplorationSum = new HashMap<>();
    private Map<Integer, Integer> maxEntropyExplorationCount = new HashMap<>();
    private Map<Integer, Long> timestamps = new HashMap<>();
    private Map<Integer, Double> entropyBestEffortSum = new HashMap<>();
    private Map<Integer, Integer> entropyBestEffortCount = new HashMap<>();
    private Map<Integer, Double> maxEntropyBestEffortSum = new HashMap<>();
    private Map<Integer, Integer> maxEntropyBestEffortCount = new HashMap<>();

    public   Sample sampleFromGame(int numUnrollSteps, @NotNull Game game) {
        int gamePos = samplePosition(game);
        Sample sample = null;
        long count = 0;
        do {
            try {
                sample = sampleFromGame(numUnrollSteps, game, gamePos );
            } catch (MuZeroNoSampleMatch e) {
                count++;
            }
        } while (sample == null);
        if (count > 10000) {
            log.debug("{} tries were necessary to get a sample. You could lower the config parameter offPolicyRatioLimit.", count);
        }
        return sample;
    }

    public  Sample sampleFromGame(int numUnrollSteps, @NotNull Game game, int gamePos) {
        Sample sample = new Sample();
        sample.setGame(game);

        ObservationModelInput observation = game.getObservationModelInput(gamePos);
        sample.getObservations().add(observation);


        List<Integer> actions = new ArrayList<>(game.getGameDTO().getActions());
        int originalActionSize = actions.size();
        if (actions.size() < gamePos + numUnrollSteps) {
            actions.addAll(game.getRandomActionsIndices(gamePos + numUnrollSteps - actions.size()));
        }
        sample.setActionsList(new ArrayList<>());


        // also add the action that was taken before the first observation
        // if there was none add an action index of -1
        if (gamePos > 0) {
            int actionIndex = actions.get(gamePos - 1);
            sample.getActionsList().add(actionIndex);
        } else {
            sample.getActionsList().add(-1);
        }
        for (int i = 0; i < numUnrollSteps; i++) {

            int actionIndex = actions.get(gamePos + i);
            sample.getActionsList().add(actionIndex);

          //  if (gamePos + i <= originalActionSize) {
                observation = game.getObservationModelInput(gamePos + i);
           // }
            sample.getObservations().add(observation);


        }
        sample.setGamePos(gamePos);
        sample.setNumUnrollSteps(numUnrollSteps);

        sample.makeTarget( );
        return sample;
    }

    public static int samplePosition(@NotNull Game game) {
        return ThreadLocalRandom.current().nextInt(0, game.getGameDTO().getActions().size() + 1);
    }



    public static int getEpochFromPath(Path path) {
        int epoch;
        String fileName = path.getFileName().toString();
        if (fileName.contains("-")) {
            String[] split = fileName.split("-");
            epoch = Integer.parseInt(split[split.length - 2]);
        } else {
            throw new MuZeroException("Could not find epoch in path " + path);
        }
        return epoch;
    }

    public GameBufferDTO getBuffer() {

            return this.buffer;
    }

    public int getAverageGameLength() {
        return (int) getBuffer().getGames().stream().mapToInt(g -> g.getGameDTO().getActions().size()).average().orElse(1000);
    }

    public int getMaxGameLength() {
        return getBuffer().getGames().stream().mapToInt(g -> g.getGameDTO().getActions().size()).max().orElse(1000);
    }


    @PostConstruct
    public void postConstruct() {
        init();
    }

    public void init() {
        this.batchSize = config.getBatchSize();
        if (this.getBuffer() != null) {
            this.getBuffer().games.forEach(g -> {
                g.setGameDTO(null);
                g.setOriginalGameDTO(null);
            });
            this.getBuffer().games.clear();
        }
        this.buffer = new GameBufferDTO(config);

    }

    /**
     * @param numUnrollSteps number of actions taken after the chosen position (if there are any)
     */
    public List<Sample> sampleBatch(int numUnrollSteps ) {
        try (NDManager ndManager = NDManager.newBaseManager(Device.cpu())) {
            return sampleGames().stream()
                .map(game -> sampleFromGame(numUnrollSteps, game))
                .collect(Collectors.toList());

        }


    }

    public List<Game> sampleGames() {

        List<Game> games = getGames();
        Collections.shuffle(games);

        return games.stream()
                .limit(this.batchSize)
                .collect(Collectors.toList());
    }

    @NotNull
    public List<Game> getGames() {
        List<Game> games = new ArrayList<>(this.buffer.getGames());
        log.trace("Games from buffer: {}",  games.size() );
        return games;
    }



    public void loadLatestStateIfExists() {
        init();
        List<Path> paths = this.gameBufferIO.getBufferNames();
        int epochMax = 0;
        for (int h = 0; h < paths.size() && !this.buffer.isBufferFilled(); h++) {
            Path path = paths.get(paths.size() - 1 - h);
            GameBufferDTO gameBufferDTO = this.gameBufferIO.loadState(path);

            epochMax = Math.max(getEpochFromPath(path), epochMax);
            gameBufferDTO.getGames().forEach(game -> addGame(game, true));
        }

        this.modelState.setEpoch(epochMax);
    }

    public void sortGamesByLastValueError() {
        this.getBuffer().getGames().sort(
            (Game g1, Game g2) -> Float.compare(g2.getError(), g1.getError()));
    }

    public void removeGames(List<Game> games) {
        games.forEach(this::removeGame);
    }

    public void removeGame(Game game) {
        this.getBuffer().removeGame(game);
    }

    public double getPRandomActionRawAverage() {
        double sum = this.getBuffer().games.stream().mapToDouble(g -> g.getGameDTO().pRandomActionRawSum).sum();
        long count = this.getBuffer().games.stream().mapToLong(g -> g.getGameDTO().pRandomActionRawCount).sum();
        if (count == 0) return 1;
        return sum / count;
    }





    public void addGames(List<Game> games, boolean atBeginning) {

        games.forEach(game -> addGameAndRemoveOldGameIfNecessary(game, atBeginning));
        if (this.config.getPlayTypeKey() == PlayTypeKey.REANALYSE) {
            // do nothing more
        } else {
            this.timestamps.put(games.get(0).getGameDTO().getTrainingEpoch(), System.currentTimeMillis());
            logEntropyInfo();
            int epoch = this.getModelState().getEpoch();

            this.gameBufferIO.saveGames(
                this.getBuffer().games.stream()
                    .filter(g -> g.getGameDTO().getTrainingEpoch() == epoch)
                    .filter(g -> !g.isReanalyse())
                    .collect(Collectors.toList()),
                this.getModelState().getCurrentNetworkNameWithEpoch(), this.getConfig());
        }

    }



    private void addGameAndRemoveOldGameIfNecessary(Game game, boolean atBeginning ) {
         memorizeEntropyInfo(game, game.getGameDTO().getTrainingEpoch());
        if (!game.isReanalyse()) {
            game.getGameDTO().setNetworkName(this.getModelState().getCurrentNetworkNameWithEpoch());
        }
        game.getGameDTO().setTrainingEpoch(this.getModelState().getEpoch());
        buffer.addGameAndRemoveOldGameIfNecessary(game, atBeginning);
    }


    private void addGame(Game game, boolean atBeginning) {
        memorizeEntropyInfo(game, game.getGameDTO().getTrainingEpoch());
        getBuffer().addGame(game, atBeginning);
    }

    private void memorizeEntropyInfo(Game game, int epoch) {
        this.entropyBestEffortSum.putIfAbsent(epoch, 0.0);
        this.maxEntropyBestEffortSum.putIfAbsent(epoch, 0.0);
        this.entropyExplorationSum.putIfAbsent(epoch, 0.0);
        this.maxEntropyExplorationSum.putIfAbsent(epoch, 0.0);
        this.entropyBestEffortCount.putIfAbsent(epoch, 0);
        this.maxEntropyBestEffortCount.putIfAbsent(epoch, 0);
        this.entropyExplorationCount.putIfAbsent(epoch, 0);
        this.maxEntropyExplorationCount.putIfAbsent(epoch, 0);
        if (game.getGameDTO().hasExploration()) {
            this.entropyExplorationSum.put(epoch, this.entropyExplorationSum.get(epoch) + game.getGameDTO().getAverageEntropy());
            this.entropyExplorationCount.put(epoch, this.entropyExplorationCount.get(epoch) + 1);
            this.maxEntropyExplorationSum.put(epoch, this.maxEntropyExplorationSum.get(epoch) + game.getGameDTO().getAverageActionMaxEntropy());
            this.maxEntropyExplorationCount.put(epoch, this.maxEntropyExplorationCount.get(epoch) + 1);
        } else {
            this.entropyBestEffortSum.put(epoch, this.entropyBestEffortSum.get(epoch) + game.getGameDTO().getAverageEntropy());
            this.entropyBestEffortCount.put(epoch, this.entropyBestEffortCount.get(epoch) + 1);
            this.maxEntropyBestEffortSum.put(epoch, this.maxEntropyBestEffortSum.get(epoch) + game.getGameDTO().getAverageActionMaxEntropy());
            this.maxEntropyBestEffortCount.put(epoch, this.maxEntropyBestEffortCount.get(epoch) + 1);
        }
    }


    @SuppressWarnings({"java:S106"})
    public void logEntropyInfo() {
        System.out.println("epoch;timestamp;entropyBestEffort;entropyExploration;maxEntropyBestEffort;maxEntropyExploration");
        this.entropyBestEffortSum.keySet().stream().sorted().forEach(epochLocal -> {

            double entropyBestEffort = this.entropyBestEffortSum.get(epochLocal) / Math.max(1, this.entropyBestEffortCount.get(epochLocal));
            double entropyExploration = this.entropyExplorationSum.get(epochLocal) / Math.max(1, this.entropyExplorationCount.get(epochLocal));
            double maxEntropyBestEffort = this.maxEntropyBestEffortSum.get(epochLocal) / Math.max(1, this.maxEntropyBestEffortCount.get(epochLocal));
            double maxEntropyExploration = this.maxEntropyExplorationSum.get(epochLocal) / Math.max(1, this.maxEntropyExplorationCount.get(epochLocal));
            String message = String.format("%d; %d; %.4f; %.4f; %.4f; %.4f", epochLocal, this.timestamps.get(epochLocal), entropyBestEffort, entropyExploration, maxEntropyBestEffort, maxEntropyExploration);

            System.out.println(message);

        });

    }

    public void putMeanValueLoss(int epoch, double meanValueLoss) {
        meanValuesLosses.put(epoch, meanValueLoss);
    }
//    public void putMeanEntropyValueLoss(int epoch, double meanValueLoss) {
//        meanEntropyValuesLosses.put(epoch, meanValueLoss);
//    }

    public Double getMaxMeanValueLoss() {
        // get max of meanValueLosses values
        return meanValuesLosses.values().stream().max(Double::compare).orElse(0.0);
    }

    public double getDynamicRootTemperature() {
        return config.getTemperatureRoot();

    }

    public List<Game> getGamesToReanalyse() {
        int n =   config.getNumParallelGamesPlayed();
  //      List<String> networkNames = this.buffer.games.stream().map(g -> g.getGameDTO().getNetworkName()).distinct().collect(Collectors.toList());
        List<String> networkNames = new ArrayList<>();
        List<Game> games =  gameBufferIO.loadGamesForReplay(n, networkNames );
        games.forEach(g -> g.setReanalyse(true));
        return games;
    }


}
