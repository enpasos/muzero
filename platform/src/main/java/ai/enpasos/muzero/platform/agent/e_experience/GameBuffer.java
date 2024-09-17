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
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.*;
import ai.enpasos.muzero.platform.agent.e_experience.memory2.ShortTimestep;
import ai.enpasos.muzero.platform.common.AliasMethod;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
@Component
public class GameBuffer {



    public static final String EPOCH_STR = "Epoch";

    @Autowired
    private ModelState modelState;


    @Autowired
    private DBService dbService;

    private int batchSize;
    private GameBufferDTO planningBuffer;
  //  private GameBufferDTO rulesBuffer;
    private GameBufferDTO reanalyseBuffer;
    @Autowired
    private MuZeroConfig config;

    @Autowired
    EpisodeRepo episodeRepo;

    @Autowired
    TimestepRepo timestepRepo;

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
        int gamePos  = samplePosition(0, game);
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
        List<Integer> actions =   game.getEpisodeDO().getActions();

        int originalActionSize = actions.size();
        if (actions.size() < gamePos + numUnrollSteps) {
            actions.addAll(game.getRandomActionsIndices(gamePos + numUnrollSteps - actions.size()));
        }
        sample.setActionsList(new ArrayList<>());
        for (int i = 0; i <  numUnrollSteps ; i++) {
            int actionIndex = actions.get(gamePos + i);
            sample.getActionsList().add(actionIndex);

            if (config.isWithConsistencyLoss()) {
                observation = game.getObservationModelInput(gamePos + i + 1);    // TODO: check
                sample.getObservations().add(observation);
            }
        }
        sample.setGamePos(gamePos);
        sample.setNumUnrollSteps(numUnrollSteps);

        sample.makeTarget( );
        return sample;
    }

    public static int samplePosition(int t0, @NotNull Game game) {
        int tmax = game.getEpisodeDO().getLastTimeWithAction() + 1 ;
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

    public GameBufferDTO getPlanningBuffer() {
            return this.planningBuffer;
    }

    public int getAverageGameLength() {
        return   getPlanningBuffer().getEpisodeMemory().getAverageGameLength() ;
    }

    public int getMaxGameLength() {
        return getPlanningBuffer().getEpisodeMemory().getMaxGameLength();
    }


    @PostConstruct
    public void postConstruct() {
        init();
    }

    public void init() {
        this.batchSize = config.getBatchSize();
        this.planningBuffer = new GameBufferDTO(config);
     //   this.rulesBuffer = new GameBufferDTO(config);
        this.reanalyseBuffer = new GameBufferDTO(config);
    }

    /**
     * @param numUnrollSteps number of actions taken after the chosen position (if there are any)
     */
    public List<Sample> sampleBatchFromPlanningBuffer(int numUnrollSteps ) {
        try (NDManager ndManager = NDManager.newBaseManager(Device.cpu())) {
            return sampleGamesFrom( getGamesFromPlanningBuffer()).stream()
                .map(game -> sampleFromGame(numUnrollSteps, game))
                .collect(Collectors.toList());
        }
    }
//    public List<Sample> sampleBatchFromRulesBuffer(int s) {
//        try (NDManager ndManager = NDManager.newBaseManager(Device.cpu())) {
//            return  getGamesToLearnRules().stream()
//                    .map(game -> sampleFromGame(numUnrollSteps, game))
//                    .collect(Collectors.toList());
//        }
//    }


 //Set<Long> episodeIdsRewardLearning;
   // Set<Long> episodeIdsLegalActionLossLearning;
    private List<Long> episodeIds;

//
    public void clearEpisodeIds() {
        episodeIds = null;
    }



    public List<Long> getEpisodeIds( ) {
        int limit = 50000;
        if (episodeIds == null) {
            int offset = 0;
            episodeIds = new ArrayList<>();
            List newIds;
            do {
                newIds = episodeRepo.findAllEpisodeIds( limit, offset);
                episodeIds.addAll(newIds);
                offset += limit;
            } while (newIds.size() > 0);
        }
        return episodeIds;
    }



 //   private List<IdProjection> relevantIdsA;

//    private List<IdProjection2> relevantIds2;
    private List<IdProjection3> idProjection3List;

    public void resetRelevantIds() {
    //    relevantIds2 = null;
  //      relevantIdsA = null;
        idProjection3List = null;
    }





    public List<Long> getShuffledEpisodeIds() {
        List<Long> episodeIds = getEpisodeIds( )  ;
        Collections.shuffle(episodeIds);
        return episodeIds;

    }



    public List<Sample> sampleBatchFromReanalyseBuffer(int numUnrollSteps ) {
        try (NDManager ndManager = NDManager.newBaseManager(Device.cpu())) {
            return sampleGamesFrom( getGamesFromReanalyseBuffer()).stream()
                    .map(game -> sampleFromGame(numUnrollSteps, game))
                    .collect(Collectors.toList());
        }
    }

    public List<Game> sampleGamesFrom(List<Game> games) {


        Collections.shuffle(games);


        int n = this.batchSize  ;
        List<Game> gameList = new ArrayList<>();
        if (n > 0) {
            gameList.addAll(games.stream()
                    .limit(n)
                    .collect(Collectors.toList()));
        }
        return gameList;
    }


    public List<Game> getGamesFromPlanningBuffer() {
        List<Game> games = new ArrayList<>(this.planningBuffer.getEpisodeMemory().getGameList());
        log.trace("Games from planning buffer: {}",  games.size() );
        return games;
    }


    public List<Game> getGamesFromReanalyseBuffer() {
        List<Game> games = new ArrayList<>(this.reanalyseBuffer.getEpisodeMemory().getGameList());
        log.trace("Games from reanalyse buffer: {}",  games.size() );
        return games;
    }



    public void loadLatestStateIfExists() {
        init();
        DurAndMem duration = new DurAndMem();
        duration.on();
        List<EpisodeDO> episodeDOList = this.dbService.findTopNByOrderByIdDescAndConvertToGameDTOList(config.getWindowSize());

        duration.off();
        log.debug("duration loading buffer from db: " + duration.getDur());
        this.getPlanningBuffer().setInitialEpisodeDOList(episodeDOList);
        episodeDOList.stream().mapToInt(EpisodeDO::getTrainingEpoch).max().ifPresent(this.modelState::setEpoch);
        episodeDOList.stream().mapToLong(EpisodeDO::getCount).max().ifPresent(this.getPlanningBuffer()::setCounter);
        this.getPlanningBuffer().rebuildGames(config );

    }



    public double getPRandomActionRawAverage() {
        List<Game> gameList = this.getPlanningBuffer().getEpisodeMemory().getGameList();
        double sum = gameList.stream().mapToDouble(g -> g.getEpisodeDO().getPRandomActionRawSum()).sum();
        long count = gameList.stream().mapToLong(g -> g.getEpisodeDO().getPRandomActionRawCount()).sum();
        if (count == 0) return 1;
        return sum / count;
    }




    public void addGames(List<Game> games ) {
        if (games.isEmpty()) return;

      //  int countGamesNotVisitingUnvisitedActions = 0;
        for(Game game : games) {
//            if (!this.buffer.getEpisodeMemory().visitsUnvisitedAction(game)) {
//                countGamesNotVisitingUnvisitedActions++;
//            }
            addGameAndRemoveOldGameIfNecessary(game );
        }
     //   log.info("### countGamesNotVisitingUnvisitedActions: {} of {}", countGamesNotVisitingUnvisitedActions, games.size());
        if (this.config.getPlayTypeKey() == PlayTypeKey.REANALYSE) {
            // do nothing more
        } else {
            this.timestamps.put(games.get(0).getEpisodeDO().getTrainingEpoch(), System.currentTimeMillis());
            logEntropyInfo();
            int epoch = modelState.getEpoch();


           List<Game> gamesToSave = this.getPlanningBuffer().getEpisodeMemory().getGameList().stream()
                    .filter(g -> g.getEpisodeDO().getTrainingEpoch() == epoch)
                    .filter(g -> !g.isReanalyse())

                    .collect(Collectors.toList());


           gamesToSave.forEach(g -> g.getEpisodeDO().setNetworkName(modelState.getCurrentNetworkNameWithEpoch()));

            List<EpisodeDO> episodes  = games.stream().map(Game::getEpisodeDO).collect(Collectors.toList());

            dbService.saveEpisodesAndCommit(episodes);
        }

    }



    private void addGameAndRemoveOldGameIfNecessary(Game game ) {

       //  memorizeEntropyInfo(game, game.getEpisodeDO().getTrainingEpoch());
        if (!game.isReanalyse()) {
            game.getEpisodeDO().setNetworkName(modelState.getCurrentNetworkNameWithEpoch());
            game.getEpisodeDO().setTrainingEpoch(modelState.getEpoch());
            planningBuffer.addGame(game );
        } else {
            this.reanalyseBuffer.addGame(game );
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




    public List<Game> getGamesToReanalyse() {
        int n =   config.getNumParallelGamesPlayed();

        return getNRandomSelectedGames(n);
    }




    public List<Game> getNRandomSelectedGames(int n) {
        List<EpisodeDO> episodeDOList = this.dbService.findRandomNByOrderByIdDescAndConvertToGameDTOList(n); // gameBufferIO.loadGamesForReplay(n );   // TODO
        List<Game> games = convertEpisodeDOsToGames(episodeDOList, config);

        return games;
    }




    public static List<Game> convertEpisodeDOsToGames(List<EpisodeDO> episodeDOList, MuZeroConfig config) {

        List<Game> games = new ArrayList<>();
        for (EpisodeDO episodeDO : episodeDOList) {
            episodeDO.sortTimeSteps();
            Game game = config.newGame(false,false);
            game.setEpisodeDO(episodeDO);
            games.add(game);
        }

        return games;
    }





    private Set<ShortTimestep> shortTimesteps;
    private Map<Long, Integer> episodeIdToMaxTime;

    public void refreshCache(List<Long> idsTsChanged) {
        Set<ShortTimestep>  shortTimesteps = getShortTimestepSet();
        List<ShortTimestep> shortTimestepsNew =  timestepRepo.getShortTimestepList(idsTsChanged);
         shortTimesteps.removeAll(shortTimestepsNew );
         shortTimesteps.addAll(shortTimestepsNew );
    }


    public Set<ShortTimestep> getShortTimestepSet( )  {
        if (shortTimesteps == null || shortTimesteps.isEmpty()) {
            int limit = 50000;

            int offset = 0;
            shortTimesteps = new HashSet<>();
            episodeIdToMaxTime = new HashMap<>();
            List<Object[]> resultList;
            do {
                // comment: no list of proxies for performance reasons
                resultList = timestepRepo.getShortTimestepList(limit, offset);
                List<ShortTimestep> shortTimesteps = resultList.stream()
                        .map(result -> ShortTimestep.builder()
                                .id(result[0] != null ? ((Number) result[0]).longValue() : null)
                                .episodeId(result[1] != null ? ((Number) result[1]).longValue() : null)
                                .boxes(result[2] != null ? convert((Integer[]) result[2]) : null)
                                .uOk((result[3] != null) ? (Integer) result[3] : null)
                                .nextUOk(result[4] != null ? (Integer) result[4] : null)
                                .nextuokclosed(result[5] != null ? (Boolean) result[5] : null)
                                .t(result[6] != null ? (Integer) result[6] : null)
                                .uOkClosed(result[7] != null ? (Boolean) result[7] : false)
                                .build())
                        .collect(Collectors.toList());
                this.shortTimesteps.addAll(shortTimesteps);

                offset += limit;
            } while (resultList.size() > 0);
        }
        // fill episodeIdToMaxTime
        for (ShortTimestep shortTimestep : shortTimesteps) {
            Long episodeId = shortTimestep.getEpisodeId();
            Integer t = shortTimestep.getT();
            episodeIdToMaxTime.put(episodeId, Math.max(t, episodeIdToMaxTime.getOrDefault(episodeId, 0)));
        }
        return shortTimesteps;
    }




    private static int[] convert(Integer[] input) {
        int[] result = new int[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = input[i];
        }
        return result;
    }

    public ShortTimestep[] getIdsRelevantForTraining(int nOriginal, int unrollSteps) {

        Set<ShortTimestep> tsSet = getShortTimestepSet();

        // count number timesteps which are not known for given unrollSteps
         long numUnknownsForGiven =  numIsTrainableAndNeedsTraining( unrollSteps);




        // the number of training timesteps is limited by the input n
        // and strategy to train in maximum 2 times the number of timesteps which are not known
       int n = Math.min(nOriginal,   2 * (int) numUnknownsForGiven);
       if (unrollSteps == 1) {
            n = Math.min(nOriginal,    (int) numUnknownsForGiven);
       }

        // filter timesteps that are trainable
        ShortTimestep[] tsArray = (ShortTimestep[]) tsSet.stream()
                .filter(t -> {
                    int tmax = episodeIdToMaxTime.get(t.getEpisodeId());
                    return t.isTrainable(unrollSteps, tmax);
                })
                .toArray(ShortTimestep[]::new);
        n = Math.min(n, tsArray.length);

        log.info("max n = {}, effective n = {}, unrollSteps = {}, num of trainable timesteps = {}", nOriginal, n, unrollSteps, tsArray.length);


        // Generate Map<Integer, Integer> boxOccupations with the box as key, counting occurrences
        final Map<Integer, Integer> boxOccupations = Arrays.stream(tsArray)
                .map(p -> p.getBox( unrollSteps ))  // Get the box from the array
                .collect(Collectors.toMap(
                        box -> box,   // Use the box as the key
                        box -> 1,     // Initialize count as 1
                        Integer::sum  // If the box is already present, sum the counts
                ));

        // generate weight array double[] g from box(unrollSteps) as 1/(2^(box-1))
        double[] g = Arrays.stream(tsArray)
                .mapToDouble(p -> {
                    int box = p.getBox( unrollSteps );
                    if (unrollSteps == 1) {
                        return box == 0 ? 1.0 : 0.0;
                    }
                    return 1.0 / Math.pow(2, box) / boxOccupations.get(box);
                }).toArray();
        AliasMethod aliasMethod = new AliasMethod(g);
        int[] samples = aliasMethod.sampleWithoutReplacement(n);

        ShortTimestep[] tsTrainable = IntStream.range(0, samples.length).mapToObj(i -> tsArray[samples[i]]).toArray(ShortTimestep[]::new);

        return tsTrainable;


    }

    public int getSmallestUnrollSteps() {
        final int maxUnrollSteps = config.getMaxUnrollSteps();
        return (int)  getShortTimestepSet().stream().mapToInt(t ->  {
            int tmax = episodeIdToMaxTime.get(t.getEpisodeId());
            int unrollSteps = t.getUnrollSteps(tmax);
            if (t.isUOkClosed()) return unrollSteps;
            return maxUnrollSteps;
        }).min().orElse(maxUnrollSteps);
    }


    public long numIsTrainableAndNeedsTraining() {
        return  getShortTimestepSet().stream().filter(t ->  t.isTrainableAndNeedsTraining() ).count();

    }

    public long numIsTrainableAndNeedsTraining(int unrollSteps) {
        return  getShortTimestepSet().stream().filter(t ->  {
            int tmax = episodeIdToMaxTime.get(t.getEpisodeId());
            return t.isTrainableAndNeedsTraining( unrollSteps, tmax );
        }).count();

    }


}
