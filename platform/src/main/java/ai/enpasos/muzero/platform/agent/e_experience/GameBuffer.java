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
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
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
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;


@Data
@Slf4j
@Component
public class GameBuffer {


    public static final String EPOCH_STR = "Epoch";
  //  String modelName;

    @Autowired
    private ModelState modelState;

    private int batchSize;
    private GameBufferDTO buffer;
    private GameBufferDTO replayBuffer;
    @Autowired
    private MuZeroConfig config;

    @Autowired
    private GameBufferIO gameBufferIO;
    private Map<Integer, Double> meanValuesLosses = new HashMap<>();
    private Map<Integer, Double> entropyExplorationSum = new HashMap<>();
    private Map<Integer, Integer> entropyExplorationCount = new HashMap<>();
    private Map<Integer, Double> maxEntropyExplorationSum = new HashMap<>();
    private Map<Integer, Integer> maxEntropyExplorationCount = new HashMap<>();
    private Map<Integer, Long> timestamps = new HashMap<>();
    private Map<Integer, Double> entropyBestEffortSum = new HashMap<>();
    private Map<Integer, Integer> entropyBestEffortCount = new HashMap<>();
    private Map<Integer, Double> maxEntropyBestEffortSum = new HashMap<>();
    private Map<Integer, Integer> maxEntropyBestEffortCount = new HashMap<>();

    public   Sample sampleFromGame(int numUnrollSteps, @NotNull Game game, NDManager ndManager, GameBuffer gameBuffer) {
        int gamePos = samplePosition(game);
        Sample sample = null;
        long count = 0;
        do {
            try {
                sample = sampleFromGame(numUnrollSteps, game, gamePos, ndManager, gameBuffer);
            } catch (MuZeroNoSampleMatch e) {
                // ignore
                count++;
              //  System.out.println(count);
            }
        } while (sample == null);
        if (count > 10000) {
            log.debug("{} tries were necessary to get a sample. You could lower the config parameter offPolicyRatioLimit.", count);
        }
        return sample;
    }

    public  Sample sampleFromGame(int numUnrollSteps, @NotNull Game game, int gamePos, NDManager ndManager, GameBuffer gameBuffer) {
        Sample sample = new Sample();
        sample.setGame(game);

     //   game.replayToPosition(gamePos);

        ObservationModelInput lastObservation = game.getObservationModelInput(gamePos);
        sample.getObservations().add(lastObservation);


        List<Integer> actions = new ArrayList<>(game.getGameDTO().getActions());
        int originalActionSize = actions.size();
        if (actions.size() < gamePos + numUnrollSteps) {
            actions.addAll(game.getRandomActionsIndices(gamePos + numUnrollSteps - actions.size()));
        }
        sample.setActionsList(new ArrayList<>());
        for (int i = 0; i < numUnrollSteps; i++) {
            int actionIndex = actions.get(gamePos + i);
            sample.getActionsList().add(actionIndex);

            if (gamePos + i < originalActionSize) {
               // game.replayToPosition(gamePos + i);
                lastObservation = game.getObservationModelInput(gamePos + i);
            }
            sample.getObservations().add(lastObservation);


        }
        sample.setGamePos(gamePos);
        sample.setNumUnrollSteps(numUnrollSteps);

        sample.makeTarget();
        return sample;
    }

    public static int samplePosition(@NotNull Game game) {
        return ThreadLocalRandom.current().nextInt(0, game.getGameDTO().getActions().size() + 1);
    }

//    private static int getEpoch(Model model) {
//        String epochStr = model.getProperty(EPOCH_STR);
//        if (epochStr == null) {
//            epochStr = "0";
//        }
//        return Integer.parseInt(epochStr);
//    }

    private static int getEpochFromPath(Path path) {
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
        if (config.getPlayTypeKey() == PlayTypeKey.REANALYSE) {
            return this.replayBuffer;
        } else {
            return this.buffer;
        }
    }

    public int getAverageGameLength() {
        return (int) getBuffer().getGames().stream().mapToInt(g -> g.getGameDTO().getActions().size()).average().orElse(1000);
    }

    public int getMaxGameLength() {
        return getBuffer().getGames().stream().mapToInt(g -> g.getGameDTO().getActions().size()).max().orElse(1000);
    }

//    public void createNetworkNameFromModel(Model model, String modelName, String outputDir) {
//        int epochLocal = 0;
//        if (model != null) {
//            String epochValue = model.getProperty(GameBuffer.EPOCH_STR);
//            Path modelPath = Paths.get(outputDir);
//
//            try {
//                epochLocal = epochValue == null ? Utils.getCurrentEpoch(modelPath, modelName) + 1 : Integer.parseInt(epochValue);
//            } catch (IOException e) {
//                throw new MuZeroException(e);
//            }
//        } else {
//            modelName = config.getModelName();
//        }
//        this.modelName = modelName;
//        this.epoch = epochLocal;
 //   }





//    private int getEpoch() {
//        int epoch = 0;
//        try {
//            epoch = modelService.getEpoch().get().intValue();
//        } catch (InterruptedException e) {
//            log.error("ModelService has been closed.");
//            throw new MuZeroException(e);
//        } catch (ExecutionException e) {
//            throw new MuZeroException(e);
//        }
//        return epoch;
//    }

//    public String getCurrentNetworkNameWithoutEpoch() {
//        return String.format(Locale.ROOT, "%s", modelName);
//    }

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
        this.replayBuffer = new GameBufferDTO(config);
       // createNetworkNameFromModel(null, null, null);

    }

    /**
     * @param numUnrollSteps number of actions taken after the chosen position (if there are any)
     */
    public List<Sample> sampleBatch(int numUnrollSteps ) {
        try (NDManager ndManager = NDManager.newBaseManager(Device.cpu())) {
            List<Sample> samples = sampleGames().stream()
                .map(game -> sampleFromGame(numUnrollSteps, game, ndManager, this))
                .collect(Collectors.toList());

          //  samples.stream().forEach(s -> s.makeTarget());
            return samples;
        }


    }

    public List<Game> sampleGames() {

        List<Game> games = getGames();
        Collections.shuffle(games);

//        long nDraw = games.stream().filter(g -> {
//            if (g instanceof ZeroSumGame zeroSumGame) {
//                Optional<OneOfTwoPlayer> winner = zeroSumGame.whoWonTheGame();
//                return winner.isEmpty() ;
//            } else {
//                return false;
//            }
//        }).count();

//        long nWinB = games.stream().filter(g -> {
//            if (g instanceof ZeroSumGame zeroSumGame) {
//                Optional<OneOfTwoPlayer> winner = zeroSumGame.whoWonTheGame();
//                return   !winner.isEmpty() && winner.get() == OneOfTwoPlayer.PLAYER_B;
//            } else {
//                return false;
//            }
//        }).count();
//        long nWinA = games.stream().filter(g -> {
//            if (g instanceof ZeroSumGame zeroSumGame) {
//                Optional<OneOfTwoPlayer> winner = zeroSumGame.whoWonTheGame();
//                return   !winner.isEmpty() && winner.get() == OneOfTwoPlayer.PLAYER_A;
//            } else {
//                return false;
//            }
//        }).count();
//
//        if (nDraw + nWinA + nWinB == games.size()) {
//            log.trace("{} draws, {} win A, {} win B", nDraw, nWinA, nWinB);
//        }
        List<Game> gamesToTrain = games.stream()
                .limit(this.batchSize )
                .collect(Collectors.toList());
        gamesToTrain.stream().forEach(g -> g.setPlayTypeKey(config.getPlayTypeKey()));
        return gamesToTrain;
    }

    @NotNull
    public List<Game> getGames() {
        List<Game> gamesFromBuffer = new ArrayList<>(this.buffer.getGames());
        List<Game> gamesFromReplayBuffer = new ArrayList<>(this.replayBuffer.getGames());
        //double f = config.getReplayFraction();
       // int nReplayGames = (int)(gamesFromBuffer.size() * f / (1-f));


        Collections.shuffle(gamesFromReplayBuffer);
        int nGamesFromBuffer = gamesFromBuffer.size();
        List<Game> games =  gamesFromBuffer;
       // List<Game> gamesFromReplay = gamesFromReplayBuffer.subList(0, Math.min(nReplayGames, gamesFromReplayBuffer.size()));
     //   int nGamesFromReplay = gamesFromReplay.size();
       // games.addAll(gamesFromReplay);
        Collections.shuffle(games);
        // log how many games are from the replay buffer and how many from the buffer
        int nReplayGamesFromBuffer = (int) games.stream().filter(g -> g.getPlayTypeKey() == PlayTypeKey.REANALYSE).count();

      //  log.trace("Games from buffer: {}, games from replay buffer: {}", nGamesFromBuffer, nGamesFromReplay);
        log.trace("Games from buffer: {}",  nGamesFromBuffer );
        return games;
    }



    public void loadLatestState() {
        List<Path> paths = this.gameBufferIO.getBufferNames();
        for (int h = 0; h < paths.size() && !this.buffer.isBufferFilled(); h++) {
            Path path = paths.get(paths.size() - 1 - h);
            GameBufferDTO gameBufferDTO = this.gameBufferIO.loadState(path);
            this.modelState.setEpoch(getEpochFromPath(path));
            gameBufferDTO.getGames().forEach(game -> addGame(game, true));
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
        this.getBuffer().removeGame(game);
    }

    public double getPRandomActionRawAverage() {
        double sum = this.getBuffer().games.stream().mapToDouble(g -> g.getGameDTO().pRandomActionRawSum).sum();
        long count = this.getBuffer().games.stream().mapToLong(g -> g.getGameDTO().pRandomActionRawCount).sum();
        if (count == 0) return 1;
        return sum / count;
    }

//    public void addGames(int epoch, List<Game> games, boolean atBeginning) {
//
//        games.forEach(game -> addGameAndRemoveOldGameIfNecessary(epoch, game, atBeginning));
//        if (this.config.getPlayTypeKey() == PlayTypeKey.REANALYSE) {
//            // do nothing more
//        } else {
//            this.timestamps.put(epoch, System.currentTimeMillis());
//            logEntropyInfo();
//            this.gameBufferIO.saveGames(
//                this.getBuffer().games.stream()
//                    .filter(g -> g.getGameDTO().getNetworkName().equals(this.getCurrentNetworkName()))
//                //    .filter(g -> g.getGameDTO().getTrainingEpoch() == this.getEpoch())
//                    .filter(g -> g.getPlayTypeKey() != PlayTypeKey.REANALYSE)
//                    .collect(Collectors.toList()),
//                this.getCurrentNetworkName(), this.getConfig());
//        }
//
//    }




    public void addGames2(List<Game> games, boolean atBeginning) {

        games.forEach(game -> addGameAndRemoveOldGameIfNecessary2(game, atBeginning));
        if (this.config.getPlayTypeKey() == PlayTypeKey.REANALYSE) {
            // do nothing more
        } else {
            this.timestamps.put(games.get(0).getGameDTO().getTrainingEpoch(), System.currentTimeMillis());
            logEntropyInfo();
          //  String currentNetworkNameWithEpoch = this.getCurrentNetworkNameWithEpoch();
            int epoch = this.getModelState().getEpoch();
            if (epoch != 0) {
                int j = 42;
            }
            this.gameBufferIO.saveGames(
                this.getBuffer().games.stream()
                    .filter(g -> g.getGameDTO().getTrainingEpoch() == epoch)
                    .filter(g -> g.getPlayTypeKey() != PlayTypeKey.REANALYSE)
                    .collect(Collectors.toList()),
                this.getModelState().getCurrentNetworkNameWithEpoch(), this.getConfig());
        }

    }

    private void addGameAndRemoveOldGameIfNecessary2(Game game, boolean atBeginning) {
        addGameAndRemoveOldGameIfNecessary2(game, atBeginning, this.getModelState().getCurrentNetworkNameWithEpoch());
    }

    private void addGameAndRemoveOldGameIfNecessary2(Game game, boolean atBeginning, String networkName) {
        //game.getGameDTO().setTrainingEpoch(epoch);
        memorizeEntropyInfo(game, game.getGameDTO().getTrainingEpoch());
        game.getGameDTO().setNetworkName(networkName);
        game.getGameDTO().setTrainingEpoch(this.getModelState().getEpoch());
        buffer.addGameAndRemoveOldGameIfNecessary(game, atBeginning);
    }
    private void addGameAndRemoveOldGameIfNecessary(int epoch, Game game, boolean atBeginning) {
        addGameAndRemoveOldGameIfNecessary(epoch, game, atBeginning, this.getModelState().getCurrentNetworkNameWithEpoch());
    }

    private void addGameAndRemoveOldGameIfNecessary(int epoch, Game game, boolean atBeginning, String networkName) {
        game.getGameDTO().setTrainingEpoch(epoch);
        memorizeEntropyInfo(game, epoch);
        game.getGameDTO().setNetworkName(networkName);
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
            this.maxEntropyExplorationSum.put(epoch, this.maxEntropyExplorationSum.get(epoch) + game.getGameDTO().getAverageMaxEntropy());
            this.maxEntropyExplorationCount.put(epoch, this.maxEntropyExplorationCount.get(epoch) + 1);
        } else {
            this.entropyBestEffortSum.put(epoch, this.entropyBestEffortSum.get(epoch) + game.getGameDTO().getAverageEntropy());
            this.entropyBestEffortCount.put(epoch, this.entropyBestEffortCount.get(epoch) + 1);
            this.maxEntropyBestEffortSum.put(epoch, this.maxEntropyBestEffortSum.get(epoch) + game.getGameDTO().getAverageMaxEntropy());
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

    public Double getMaxMeanValueLoss() {
        // get max of meanValueLosses values
        return meanValuesLosses.values().stream().max(Double::compare).orElse(0.0);
    }

    public double getDynamicRootTemperature() {
        return config.getTemperatureRoot();

    }

    public List<Game> getGamesToReanalyse() {
        int n =   config.getNumParallelGamesPlayed();
        List<String> networkNames = this.buffer.games.stream().map(g -> g.getGameDTO().getNetworkName()).distinct().collect(Collectors.toList());
        List<Game> games =  gameBufferIO.loadGamesForReplay(n, networkNames, this.getConfig());
        //games.forEach(g -> g.setPlayTypeKey(PlayTypeKey.REANALYSE));
        games.forEach(g -> g.setReanalyse(true));
        return games;
    }



}
