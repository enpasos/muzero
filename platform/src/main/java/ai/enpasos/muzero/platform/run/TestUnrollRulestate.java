package ai.enpasos.muzero.platform.run;


import ai.enpasos.muzero.platform.agent.a_loopcontrol.parallelEpisodes.PlayService;
import ai.enpasos.muzero.platform.agent.b_episode.SelfPlayGame;
import ai.enpasos.muzero.platform.agent.d_model.Boxing;
import ai.enpasos.muzero.platform.agent.d_model.djl.RulesBuffer;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.d_model.service.ZipperFunctions;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.NetworkIOService;
import ai.enpasos.muzero.platform.agent.e_experience.db.DBService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.IdProjection;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.ValueRepo;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static ai.enpasos.muzero.platform.agent.e_experience.GameBuffer.convertEpisodeDOsToGames;

@Slf4j
@Component
public class TestUnrollRulestate {
    @Autowired
    NetworkIOService networkIOService;
    @Autowired
    ModelService modelService;
    @Autowired
    ValueRepo valueRepo;
    @Autowired
    EpisodeRepo episodeRepo;
    @Autowired
    TimestepRepo timestepRepo;
    @Autowired
    DBService dbService;
    @Autowired
    MuZeroConfig config;

    @Autowired
    GameProvider gameProvider;

    @Autowired
    TemperatureCalculator temperatureCalculator;

    @Autowired
    PlayService playService;

    @Autowired
    GameBuffer gameBuffer;

    @Autowired
    SelfPlayGame selfPlayGame;



//    public void run( ) {
//        run(1  );
//    }

    public void identifyRelevantTimestepsAndTestThem(    ) {

        int epoch = networkIOService.getLatestNetworkEpoch();
        log.info("identifyRelevantTimestepsAndTestThem epoch {} ... starting", epoch );
        int maxBox = timestepRepo.maxBox();
        List<Integer> boxesRelevant = Boxing.boxesRelevant(epoch, maxBox);

        if (boxesRelevant.size() == 0 ||(boxesRelevant.size() > 0 && boxesRelevant.getLast() < 1)) {
            log.info("identifyRelevantTimestepsAndTestThem no relevant boxes (>0) found ... finished");
            return;
        }

        // TODO: do something similar
//        for (int i = 0; i <= learnUntilMaxBox; i++) {
//            if (boxesRelevant.size() > 0 && boxesRelevant.get(0) == learnUntilMaxBox) {
//                boxesRelevant.remove(0);
//            }
//        }



        log.info("identifyRelevantTimestepsAndTestThem boxesRelevant = {}", boxesRelevant.toString());
        gameBuffer.resetRelevantIds();
        List<IdProjection> idProjections = gameBuffer.getIdsFromBoxesRelevant(boxesRelevant);
        log.info("identifyRelevantTimestepsAndTestThem timesteps = {}", idProjections.size());

     //   modelService.loadLatestModel(epoch).join();   // check


        RulesBuffer rulesBuffer = new RulesBuffer();
        rulesBuffer.setWindowSize(1000);
        Set<Long> episodeIdsSet = idProjections.stream().map(IdProjection::getEpisodeId).collect(Collectors.toSet());
        List<Long> episodeIds = new ArrayList<>(episodeIdsSet);
        log.info("identifyRelevantTimestepsAndTestThem episodeIds = {}", episodeIds.size());
        rulesBuffer.setIds(episodeIds);
        Set<Long> timeStepIds = idProjections.stream().map(IdProjection::getId).collect(Collectors.toSet());
        int count = 0;
        for (RulesBuffer.IdWindowIterator iterator = rulesBuffer.new IdWindowIterator(); iterator.hasNext(); ) {
            List<Long> episodeIdsRulesLearningList = iterator.next();
            count += episodeIdsRulesLearningList.size();
            log.info( "identifyRelevantTimestepsAndTestThem count episodes = {} of {}", count, episodeIds.size());
            List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(episodeIdsRulesLearningList);
            List<Game> gameBuffer = convertEpisodeDOsToGames(episodeDOList, config);
            playService.uOkAnalyseGames(gameBuffer, false );

            boolean[][][] bOK = ZipperFunctions.b_OK_From_UOk_in_Episodes(episodeDOList);
            ZipperFunctions.sandu_in_Episodes_From_b_OK(bOK, episodeDOList);


            List<TimeStepDO> relevantTimeSteps =  episodeDOList.stream().flatMap(episodeDO -> episodeDO.getTimeSteps().stream()
                            .filter(timeStepDO -> timeStepIds.contains(timeStepDO.getId())))
                    .collect(Collectors.toList());


            relevantTimeSteps.forEach(timeStepDO -> {
                        timeStepDO.setUOkTested(true);
                    }) ;

            // db update also in uOK and box
            dbService.updateTimesteps_SandUOkandBox(relevantTimeSteps );

        }

        log.info("identifyRelevantTimestepsAndTestThem unrollSteps = {} ... finished" );
    }

//
//    @Data
//    @AllArgsConstructor
//    public class Result {
//        private List<Integer> uOkList;
//        private int unrollSteps;
//        private long box0;
//
//    }




    public void run( boolean allTimeSteps ) {

        int epoch = networkIOService.getLatestNetworkEpoch();
        log.info("testUnrollRulestate.run(), epoch = {}",  epoch);

        modelService.loadLatestModel(epoch).join();  // TODO: check if this is necessary

        RulesBuffer rulesBuffer = new RulesBuffer();
        rulesBuffer.setWindowSize(1000);
        rulesBuffer.setIds(gameBuffer.getEpisodeIds());
        for (RulesBuffer.IdWindowIterator iterator = rulesBuffer.new IdWindowIterator(); iterator.hasNext(); ) {
            List<Long> episodeIdsRulesLearningList = iterator.next();
            List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(episodeIdsRulesLearningList);
            List<Game> gameBuffer = convertEpisodeDOsToGames(episodeDOList, config);
            playService.uOkAnalyseGames(gameBuffer, allTimeSteps );

            boolean[][][] bOK = ZipperFunctions.b_OK_From_UOk_in_Episodes(episodeDOList);
            ZipperFunctions.sandu_in_Episodes_From_b_OK(bOK, episodeDOList);


            List<TimeStepDO> relevantTimeSteps =  episodeDOList.stream().flatMap(episodeDO -> episodeDO.getTimeSteps().stream()  )
                    .collect(Collectors.toList());

            if (allTimeSteps) {
                dbService.updateUnrollStepsOnEpisode(episodeDOList);
            }

            // db update also in uOK and box
            dbService.updateTimesteps_SandUOkandBox(relevantTimeSteps );

        }

        List<Integer> uOkList = timestepRepo.uOkList();
        int unrollSteps = config.getMaxUnrollSteps();
        if (!uOkList.isEmpty()) {
            unrollSteps = uOkList.getFirst() + 1;
            unrollSteps = Math.max(unrollSteps, 1);
        }

        log.info("uOkList: {}, unrollSteps = {}", uOkList.toString(), unrollSteps);
    }


    public void runOneGame(long episodeId ) {

        int epoch = networkIOService.getLatestNetworkEpoch();

        timestepRepo.resetUOk(  );
        modelService.loadLatestModel(epoch).join();
        EpisodeDO episodeDO = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(List.of(episodeId)).get(0);
        List<EpisodeDO> episodeDOList = List.of(episodeDO);
        List<Game> gameBuffer = convertEpisodeDOsToGames(episodeDOList, config);
        selfPlayGame.uOkAnalyseGame(gameBuffer.get(0), true);

        boolean[][][] bOK = ZipperFunctions.b_OK_From_UOk_in_Episodes(episodeDOList);
       ZipperFunctions.sandu_in_Episodes_From_b_OK(bOK, episodeDOList);

        List<TimeStepDO> relevantTimeSteps =  episodeDOList.stream().flatMap(episodeDO2 -> episodeDO2.getTimeSteps().stream()  )
                .collect(Collectors.toList());


        // db update also in uOK and box
        dbService.updateTimesteps_SandUOkandBox(relevantTimeSteps );

    }

//    public int toBeTrained(int unrollSteps) {
//       return timestepRepo.toBeTrained(unrollSteps).orElse(0);
//    }
}
