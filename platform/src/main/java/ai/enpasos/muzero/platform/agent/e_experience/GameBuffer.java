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
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.IdProjection3;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.agent.e_experience.memory2.ShortEpisode;
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
    private Map<Long, ShortEpisode> episodeIdToShortEpisodes;

    public void refreshCache(List<Long> idsTsChanged) {
        Set<ShortTimestep>  shortTimesteps = getShortTimestepSet();
        List<ShortTimestep> shortTimestepsNew =  timestepRepo.getShortTimestepList(idsTsChanged);
         shortTimesteps.removeAll(shortTimestepsNew );
         shortTimesteps.addAll(shortTimestepsNew );
        Map<Long, ShortEpisode>  episodeIdToOldShortEpisodes = episodeIdToShortEpisodes;
      //   List<Long> idsOfEpisodesThatNeedFullTesting =  episodeIdToShortEpisodes.values().stream().filter(e -> e.isNeedsFullTesting()).mapToLong(e -> e.getId()).boxed().collect(Collectors.toList());
         initShortEpisodes();
        episodeIdToShortEpisodes.values().stream().forEach(e -> {
            if (episodeIdToOldShortEpisodes.containsKey(e.getId())) {
                ShortEpisode oldShortEpisode = episodeIdToOldShortEpisodes.get(e.getId());
                e.setNeedsFullTesting(oldShortEpisode.isNeedsFullTesting());
                e.setCurrentUnrollSteps(oldShortEpisode.getCurrentUnrollSteps());
            }
        });
    }


    public Set<ShortTimestep> getShortTimestepSet( )  {
        if (shortTimesteps == null || shortTimesteps.isEmpty()) {
            int limit = 50000;

            int offset = 0;
            shortTimesteps = new HashSet<>();

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

            // fill episodeIdToShortEpisodes
            initShortEpisodes();


        }
        return shortTimesteps;
    }

    private void initShortEpisodes() {
        episodeIdToShortEpisodes = new HashMap();
        for (ShortTimestep shortTimestep : shortTimesteps) {
            Long episodeId = shortTimestep.getEpisodeId();
            ShortEpisode shortEpisode = episodeIdToShortEpisodes.get(episodeId);
            if (shortEpisode == null) {
                shortEpisode = ShortEpisode.builder().id(episodeId).shortTimesteps(new ArrayList<>()).build();
                episodeIdToShortEpisodes.put(episodeId, shortEpisode);
            }
            shortEpisode.getShortTimesteps().add(shortTimestep);
        }
        // sort shortTimesteps in shortEpisodes
        for (ShortEpisode shortEpisode : episodeIdToShortEpisodes.values()) {
            shortEpisode.getShortTimesteps().sort(Comparator.comparing(ShortTimestep::getT));
        }
        // fill episodeIdToMaxTime
        episodeIdToMaxTime = new HashMap<>();
        for (ShortTimestep shortTimestep : shortTimesteps) {
            Long episodeId = shortTimestep.getEpisodeId();
            Integer t = shortTimestep.getT();
            episodeIdToMaxTime.put(episodeId, Math.max(t, episodeIdToMaxTime.getOrDefault(episodeId, 0)));
        }

    }

    public void initNeedsFullTest(boolean needs) {
        for (ShortEpisode shortEpisode : episodeIdToShortEpisodes.values()) {
            shortEpisode.setNeedsFullTesting(needs);
        }
    }


//    public int unrollStepsEpisode(long episodeId) {
//        return episodeIdToShortEpisodes.get(episodeId).getUnrollSteps();
//    }


//    public Map<Integer, List<Long>> unrollStepsToEpisodeIds(boolean filterOnNonClosed ) {
//        getShortTimestepSet( );  // fill caches
//        Map<Integer, List<Long>> unrollStepsToEpisodeIds = new HashMap<>();
//        for (ShortEpisode shortEpisode : episodeIdToShortEpisodes.values()) {
//            if (filterOnNonClosed && shortEpisode.isClosed()) {
//                continue;
//            }
//            int unrollSteps = shortEpisode.getUnrollSteps();
//            List<Long> episodeIds = unrollStepsToEpisodeIds.get(unrollSteps);
//
//                if (episodeIds == null) {
//                    episodeIds = new ArrayList<>();
//                    unrollStepsToEpisodeIds.put(unrollSteps, episodeIds);
//                }
//                episodeIds.add(shortEpisode.getId());
//
//        }
//        return unrollStepsToEpisodeIds;
//    }

    public Map<Integer, Integer> unrollStepsToEpisodeCount(boolean filterOnNonClosed) {
        getShortTimestepSet( );  // fill caches
        Map<Integer, Integer> unrollStepsToEpisodeCount = new HashMap<>();
        for (ShortEpisode shortEpisode : episodeIdToShortEpisodes.values()) {
            if (filterOnNonClosed && shortEpisode.isClosed()) {
                continue;
            }
            int unrollSteps = shortEpisode.getUnrollSteps();
            unrollStepsToEpisodeCount.put(unrollSteps, unrollStepsToEpisodeCount.getOrDefault(unrollSteps, 0) + 1);
        }
        return unrollStepsToEpisodeCount;
    }


    public  Integer  numClosedEpisodes() {
        getShortTimestepSet();
        return (int) episodeIdToShortEpisodes.values().stream().filter(ShortEpisode::isClosed).count();
    }

    public  Integer  numEpisodes() {
        getShortTimestepSet();
        return episodeIdToShortEpisodes.size();
    }


    private static int[] convert(Integer[] input) {
        int[] result = new int[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = input[i];
        }
        return result;
    }

    public int getTmax(Long episodeId) {
        return episodeIdToMaxTime.get(episodeId);
    }

    public ShortTimestep[] getIdsRelevantForTraining(int n, int unrollSteps  ) {




        List<ShortTimestep> timeStepsThatNeedTrainingPrio1 = timeStepsThatNeedTrainingPrio1(unrollSteps);
        Collections.shuffle(timeStepsThatNeedTrainingPrio1);

        List<ShortTimestep> timeStepsThatNeedTrainingPrio2 = timeStepsThatNeedTrainingPrio2(unrollSteps);
        Collections.shuffle(timeStepsThatNeedTrainingPrio2);

        ShortTimestep[] stArray =
                this.getShortTimestepSet().stream().filter(st -> !st.needsTraining(unrollSteps)).toArray(ShortTimestep[]::new);

        double fractionNew = 0.5;

        int n_new = (int) (n * fractionNew);
        int n_new_prio1 = Math.min(n_new / 2, timeStepsThatNeedTrainingPrio1.size());
        int n_new_prio2 = Math.min(n_new - n_new_prio1 , timeStepsThatNeedTrainingPrio2.size());
        n_new = n_new_prio1 + n_new_prio2;
        int n_known =  Math.min(stArray.length, (int)(n_new / fractionNew));



        List<ShortTimestep>  timeStepsToTrain = timeStepsThatNeedTrainingPrio1.subList(0,  n_new_prio1);


        timeStepsToTrain.addAll(timeStepsThatNeedTrainingPrio2.subList(0, n_new_prio2));



        // also learn from the known ones

        // Generate Map<Integer, Integer> boxOccupations with the box as key, counting occurrences
        final Map<Integer, Integer> boxOccupations = Arrays.stream(stArray)
                .map(st -> {
                    int box = st.getLastBox(); //st.getBox( unRollStepsTimeStep);
                    return box;
                })  // Get the box from the array
                .collect(Collectors.toMap(
                        box -> box,   // Use the box as the key
                        box -> 1,     // Initialize count as 1
                        Integer::sum  // If the box is already present, sum the counts
                ));




        // generate weight array double[] g from box(unrollSteps) as 1/(2^(box-1))
        double[] g = Arrays.stream(stArray)
                .mapToDouble(st -> {
                    int box = st.getLastBox() ;
                    return 1.0 / Math.pow(2, box) / boxOccupations.get(box);
                }).toArray();

        AliasMethod aliasMethod = new AliasMethod(g);
        int[] samples = aliasMethod.sampleWithoutReplacement(n_known);
        List<ShortTimestep> tsKnownOnes = IntStream.range(0, samples.length).mapToObj(i -> stArray[samples[i]]).toList();
        timeStepsToTrain.addAll(tsKnownOnes);

Collections.shuffle(timeStepsToTrain);
    return timeStepsToTrain.toArray(new ShortTimestep[0]);

    }


    public Map<Integer, List<ShortTimestep>> mapByUnrollSteps(ShortTimestep[] allIdProjections, int unrollSteps) {
        return Arrays.stream(allIdProjections).collect(Collectors.groupingBy(ts -> {
            return ts.getUnrollSteps(getTmax(ts.getEpisodeId()), unrollSteps);
        }));
    }

    public List<ShortTimestep> timeStepsThatNeedTrainingPrio1( int unrollSteps) {
        Set<ShortTimestep> shortTimesteps = getShortTimestepSet();
        return shortTimesteps.stream()
                .filter(ts ->
                        ts.needsTrainingPrio1(getTmax(ts.getEpisodeId()), unrollSteps)
                )
                .collect(Collectors.toList()) ;
    }

    public List<ShortTimestep> timeStepsThatNeedTrainingPrio2( int unrollSteps) {
        Set<ShortTimestep> shortTimesteps = getShortTimestepSet();
        return shortTimesteps.stream()
                .filter(ts ->
                        ts.needsTrainingPrio2(getTmax(ts.getEpisodeId()), unrollSteps)
                )
                .collect(Collectors.toList()) ;
    }



    public List<Long> filterEpisodeIdsByTestNeed(List<Long> episodeIds) {
        getShortTimestepSet();
        return episodeIds.stream().filter(episodeId -> episodeIdToShortEpisodes.get(episodeId).isNeedsFullTesting()).collect(Collectors.toList());
    }

    public Map<Integer, Integer> selectUnrollStepsToEpisodeCount(boolean filterOnNonClosed) {
        Map<Integer, Integer>  unrollStepsToEpisodeCount =  unrollStepsToEpisodeCount(filterOnNonClosed);
        if (filterOnNonClosed) {
            log.info("num non closed episodes ...");
        } else {
            log.info("num all episodes ...");
        }
        unrollStepsToEpisodeCount.forEach((k, v) -> log.info("select unrollSteps: {}, episodeCount: {}", k, v));
        return  unrollStepsToEpisodeCount;
    }


    public int numNeedsTrainingPrio1(int unrollSteps) {
        int n =  (int)getShortTimestepSet().stream().filter(ts -> {
            int tmax = getTmax(ts.getEpisodeId());
            return ts.needsTrainingPrio1(tmax, unrollSteps);
        }).count();
        log.info("numBox0Prio1({}) = {}",  unrollSteps, n);
        return n;
    }


    public int findStartUnrollSteps() {
        int unrollSteps = 1;
        while (numNeedsTrainingPrio1(unrollSteps) == 0 && unrollSteps <= config.getMaxUnrollSteps()) {
            unrollSteps++;
        }
        return unrollSteps;
    }
}
