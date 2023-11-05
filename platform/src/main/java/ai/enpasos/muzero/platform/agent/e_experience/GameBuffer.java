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
import ai.enpasos.muzero.platform.agent.e_experience.db.DBService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import jakarta.persistence.Tuple;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Data
@Slf4j
@Component
public class GameBuffer {



    public static final String EPOCH_STR = "Epoch";

    @Autowired
    private ModelState modelState;


    @Autowired
    private DBService dbService;

    private int batchSize;
    private GameBufferDTO buffer;
    private GameBufferDTO bufferForReanalysedEpisodes;
    @Autowired
    private MuZeroConfig config;

    @Autowired
    EpisodeRepo episodeRepo;

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
    private Map<Long, Integer> mapTReanalyseMin2GameCount = new HashMap<>();

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
        ObservationModelInput     observation = game.getObservationModelInput(gamePos);

        sample.getObservations().add(observation);
        List<Integer> actions =  game.getEpisodeDO().getTimeSteps().stream()
                .filter(timeStepDO -> timeStepDO.getAction() != null)
                .map(timeStepDO -> (Integer)timeStepDO.getAction())
                .collect(Collectors.toList());

        int originalActionSize = actions.size();
        if (actions.size() < gamePos + numUnrollSteps) {
            actions.addAll(game.getRandomActionsIndices(gamePos + numUnrollSteps - actions.size()));
        }
        sample.setActionsList(new ArrayList<>());
        for (int i = 0; i < numUnrollSteps; i++) {
            int actionIndex = actions.get(gamePos + i);
            sample.getActionsList().add(actionIndex);

            if (gamePos + i < originalActionSize) {
                observation = game.getObservationModelInput(gamePos + i);
            }
            sample.getObservations().add(observation);


        }
        sample.setGamePos(gamePos);
        sample.setNumUnrollSteps(numUnrollSteps);

        sample.makeTarget(config.withLegalActionsHead());
        return sample;
    }

    public static int samplePosition(@NotNull Game game) {
//        int t0 = game.getFirstSamplePosition();
        int t0 = 0;
        int tmax = game.getEpisodeDO().getLastTimeWithAction() + 1 ;
//        // TODO tHybrid should be not larger than lastActionTime ... next line is a workaround
//         t0 = Math.min(t0, tmax);
        return ThreadLocalRandom.current().nextInt(t0, tmax + 1);
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
        return   getBuffer().getEpisodeMemory().getAverageGameLength() ;
    }

    public int getMaxGameLength() {
        return getBuffer().getEpisodeMemory().getMaxGameLength();
    }


    @PostConstruct
    public void postConstruct() {
        init();
    }

    public void init() {
        this.batchSize = config.getBatchSize();
        this.buffer = new GameBufferDTO(config);
        this.bufferForReanalysedEpisodes = new GameBufferDTO(config);
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
        List<Game> games = new ArrayList<>(this.buffer.getEpisodeMemory().getGameList());
        log.trace("Games from buffer: {}",  games.size() );

        List<Game> games2 = new ArrayList<>(this.bufferForReanalysedEpisodes.getEpisodeMemory().getGameList());
        log.trace("Games from bufferForReanalysedEpisodes: {}",  games2.size() );


//        int n = 2000; // TODO make configurable
//        this.buffer.get


        games.addAll(games2);
        return games;
    }



    public void loadLatestStateIfExists() {
        init();
        DurAndMem duration = new DurAndMem();
        duration.on();
        List<EpisodeDO> episodeDOList = this.dbService.findTopNByOrderByIdDescAndConvertToGameDTOList(config.getWindowSize());

        duration.off();
        log.debug("duration loading buffer from db: " + duration.getDur());
        this.getBuffer().setInitialEpisodeDOList(episodeDOList);
        episodeDOList.stream().mapToInt(EpisodeDO::getTrainingEpoch).max().ifPresent(this.modelState::setEpoch);
        episodeDOList.stream().mapToLong(EpisodeDO::getCount).max().ifPresent(this.getBuffer()::setCounter);
        this.getBuffer().rebuildGames(config);

    }



    public double getPRandomActionRawAverage() {
        List<Game> gameList = this.getBuffer().getEpisodeMemory().getGameList();
        double sum = gameList.stream().mapToDouble(g -> g.getEpisodeDO().getPRandomActionRawSum()).sum();
        long count = gameList.stream().mapToLong(g -> g.getEpisodeDO().getPRandomActionRawCount()).sum();
        if (count == 0) return 1;
        return sum / count;
    }




    public void addGames(List<Game> games ) {
        if (games.isEmpty()) return;

        int countGamesNotVisitingUnvisitedActions = 0;
        for(Game game : games) {
            if (!this.buffer.getEpisodeMemory().visitsUnvisitedAction(game)) {
                countGamesNotVisitingUnvisitedActions++;
            }
            addGameAndRemoveOldGameIfNecessary(game );
        }
        log.info("### countGamesNotVisitingUnvisitedActions: {} of {}", countGamesNotVisitingUnvisitedActions, games.size());
        if (this.config.getPlayTypeKey() == PlayTypeKey.REANALYSE) {
            // do nothing more
        } else {
            this.timestamps.put(games.get(0).getEpisodeDO().getTrainingEpoch(), System.currentTimeMillis());
            logEntropyInfo();
            int epoch = this.getModelState().getEpoch();


           List<Game> gamesToSave = this.getBuffer().getEpisodeMemory().getGameList().stream()
                    .filter(g -> g.getEpisodeDO().getTrainingEpoch() == epoch)
                    .filter(g -> !g.isReanalyse())

                    .collect(Collectors.toList());


           gamesToSave.forEach(g -> g.getEpisodeDO().setNetworkName(this.getModelState().getCurrentNetworkNameWithEpoch()));

            List<EpisodeDO> episodes  = games.stream().map(Game::getEpisodeDO).collect(Collectors.toList());

            dbService.saveEpisodesAndCommit(episodes);
        }

    }



    private void addGameAndRemoveOldGameIfNecessary(Game game ) {

         memorizeEntropyInfo(game, game.getEpisodeDO().getTrainingEpoch());
        if (!game.isReanalyse()) {
            game.getEpisodeDO().setNetworkName(this.getModelState().getCurrentNetworkNameWithEpoch());
            game.getEpisodeDO().setTrainingEpoch(this.getModelState().getEpoch());
            buffer.addGame(game );
        } else {
            this.bufferForReanalysedEpisodes.addGame(game );
        }

    }


//    private void addGame(Game game, boolean atBeginning) {
//        memorizeEntropyInfo(game, game.getEpisodeDO().getTrainingEpoch());
//        getBuffer().addGame(game, atBeginning);
//    }

    private void memorizeEntropyInfo(Game game, int epoch) {
        this.entropyBestEffortSum.putIfAbsent(epoch, 0.0);
        this.maxEntropyBestEffortSum.putIfAbsent(epoch, 0.0);
        this.entropyExplorationSum.putIfAbsent(epoch, 0.0);
        this.maxEntropyExplorationSum.putIfAbsent(epoch, 0.0);
        this.entropyBestEffortCount.putIfAbsent(epoch, 0);
        this.maxEntropyBestEffortCount.putIfAbsent(epoch, 0);
        this.entropyExplorationCount.putIfAbsent(epoch, 0);
        this.maxEntropyExplorationCount.putIfAbsent(epoch, 0);
        if (game.getEpisodeDO().hasExploration()) {
            this.entropyExplorationSum.put(epoch, this.entropyExplorationSum.get(epoch) + game.getEpisodeDO().getAverageEntropy());
            this.entropyExplorationCount.put(epoch, this.entropyExplorationCount.get(epoch) + 1);
            this.maxEntropyExplorationSum.put(epoch, this.maxEntropyExplorationSum.get(epoch) + game.getEpisodeDO().getAverageActionMaxEntropy());
            this.maxEntropyExplorationCount.put(epoch, this.maxEntropyExplorationCount.get(epoch) + 1);
        } else {
            this.entropyBestEffortSum.put(epoch, this.entropyBestEffortSum.get(epoch) + game.getEpisodeDO().getAverageEntropy());
            this.entropyBestEffortCount.put(epoch, this.entropyBestEffortCount.get(epoch) + 1);
            this.maxEntropyBestEffortSum.put(epoch, this.maxEntropyBestEffortSum.get(epoch) + game.getEpisodeDO().getAverageActionMaxEntropy());
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
    public void putMeanEntropyValueLoss(int epoch, double meanValueLoss) {
        meanEntropyValuesLosses.put(epoch, meanValueLoss);
    }

    public Double getMaxMeanValueLoss() {
        // get max of meanValueLosses values
        return meanValuesLosses.values().stream().max(Double::compare).orElse(0.0);
    }



    public List<Game> getGamesToReanalyse() {
        int n =   config.getNumParallelGamesPlayed();

        List<EpisodeDO> episodeDOList = this.dbService.findRandomNByOrderByIdDescAndConvertToGameDTOList(n); // gameBufferIO.loadGamesForReplay(n );   // TODO
        List<Game> games = convertEpisodeDOsToGames(episodeDOList, config);


        return games;
    }

    public Pair<List<Game>, Integer> getGamesByPage( int pageNumber, int pageSize) {
        Pair<List<EpisodeDO>, Integer> pair = this.dbService.findAll(pageNumber, pageSize);
        List<EpisodeDO> episodeDOList = pair.getKey();
           List<Game> games = convertEpisodeDOsToGames(episodeDOList, config);
        return new ImmutablePair<>(games, pair.getRight());
    }


    // TODO more efficient!
    public static List<Game> convertEpisodeDOsToGames(List<EpisodeDO> episodeDOList, MuZeroConfig config) {
        GameBufferDTO buffer = new GameBufferDTO();
        buffer.setInitialEpisodeDOList(episodeDOList);
        episodeDOList.stream().mapToLong(EpisodeDO::getCount).max().ifPresent(buffer::setCounter);
        buffer.rebuildGames(config);
        List<Game> games = buffer.getEpisodeMemory().getGameList();
        return games;
    }


    public List<Game> getGamesWithHighestTemperatureTimesteps() {
        int nGamesNeeded = config.getNumParallelGamesPlayed();
       // int nWindow = config.getWindowSize();
        List<Tuple> result = episodeRepo.findEpisodeIdsWithHighValueVariance(nGamesNeeded); // TODO nWindow


        List<Long> ids = result.stream().map(tuple -> tuple.get(0, Long.class)).collect(Collectors.toList());
        List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(ids );

        episodeDOList.stream().forEach(episodeDO -> {if (episodeDO.getId() == 678) {
            log.info("found episodeDO.getId() == 678");
        }});


        List<Game> games = convertEpisodeDOsToGames(episodeDOList, config);
        Map<Long, Game> idGameMap =
                games.stream().collect(Collectors.toMap(game -> game.getEpisodeDO().getId(), game -> game));
            for (int i = 0; i < games.size(); i++) {
                int t = result.get(i).get(1, Integer.class);
                long id = ids.get(i);
                Game game = idGameMap.get(id);
                game.getEpisodeDO().setTStartNormal(t);
            }

        return games;
    }
}
