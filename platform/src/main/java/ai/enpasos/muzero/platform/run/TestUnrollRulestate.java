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
import ai.enpasos.muzero.platform.agent.e_experience.memory2.ShortTimestep;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
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
    PlayService playService;

    @Autowired
    GameBuffer gameBuffer;

    @Autowired
    SelfPlayGame selfPlayGame;


    public void identifyRelevantTimestepsAndTestThemA(int unrollSteps, int epoch ) {


        int maxBox = timestepRepo.maxBoxA();
        List<Integer> boxesRelevant = Boxing.boxesRelevant(epoch, maxBox);
        log.info("boxesRelevant = {}", boxesRelevant.toString());

        if (boxesRelevant.size() == 0) {
            log.info("identifyRelevantTimestepsAndTestThem ... boxesRelevant.size() == 0 ... finished");
            return;
        }

        if (boxesRelevant.size() > 0 && boxesRelevant.get(0) == 0) {
            boxesRelevant.remove(0);
        }
        if (boxesRelevant.size() == 0) {
            log.info("identifyRelevantTimestepsAndTestThem ... boxesRelevant.size() == 0 ... finished");
            return;
        }

        log.info("identifyRelevantTimestepsAndTestThem boxesRelevant = {}", boxesRelevant.toString());
       // gameBuffer.resetRelevantIds();
      //  List<IdProjection> idProjections = gameBuffer.getIdsFromBoxesRelevantA(boxesRelevant);


        Set<ShortTimestep> shortTimesteps = gameBuffer.getShortTimestepSet( );
        List<ShortTimestep> idProjections = shortTimesteps.stream().filter(shortTimestep -> boxesRelevant.contains(shortTimestep.getBoxA())).collect(Collectors.toList());
     //   List<ShortTimestep> idProjectionsUnknown = idProjections.stream().filter(idProjection3 -> idProjection3.getBoxA() == 0).collect(Collectors.toList());


        log.info("identifyRelevantTimestepsAndTestThem timesteps = {}", idProjections.size());


        RulesBuffer rulesBuffer = new RulesBuffer();
        rulesBuffer.setWindowSize(1000);
        Set<Long> episodeIdsSet = idProjections.stream().map(ShortTimestep::getEpisodeId).collect(Collectors.toSet());
        List<Long> episodeIds = new ArrayList<>(episodeIdsSet);
        log.info("identifyRelevantTimestepsAndTestThem episodeIds = {}", episodeIds.size());
        rulesBuffer.setIds(episodeIds);
        Set<Long> timeStepIds = new HashSet<>(idProjections.stream().map(ShortTimestep::getId).collect(Collectors.toSet()));
        int count = 0;
        for (RulesBuffer.IdWindowIterator iterator = rulesBuffer.new IdWindowIterator(); iterator.hasNext(); ) {
            List<Long> episodeIdsRulesLearningList = iterator.next();
            count += episodeIdsRulesLearningList.size();
            log.info("identifyRelevantTimestepsAndTestThem count episodes = {} of {}", count, episodeIds.size());
            List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(episodeIdsRulesLearningList);
            List<Game> games = convertEpisodeDOsToGames(episodeDOList, config);
            games.stream().forEach(game -> game.getEpisodeDO().getTimeSteps().stream().forEach(timeStepDO
                                    -> timeStepDO.setToBeAnalysed(timeStepIds.contains(timeStepDO.getId())
                            )
                    )
            );

            playService.uOkAnalyseGames( games, false, unrollSteps);


            boolean[][][] bOK = ZipperFunctions.b_OK_From_UOk_in_Episodes(episodeDOList);
            ZipperFunctions.sandu_in_Episodes_From_b_OK(bOK, episodeDOList);


            List<TimeStepDO> relevantTimeSteps = episodeDOList.stream().flatMap(episodeDO -> episodeDO.getTimeSteps().stream()
                            .filter(timeStepDO -> timeStepIds.contains(timeStepDO.getId())))
                    .collect(Collectors.toList());


            relevantTimeSteps.forEach(timeStepDO -> {
                timeStepDO.setUOkTested(true);
            });


            // db update also in uOK and box
            List<Long> idsTsChanged = dbService.updateTimesteps_SandUOkandBoxA(relevantTimeSteps);
            gameBuffer.refreshCache(idsTsChanged);
        }

        log.info("identifyRelevantTimestepsAndTestThem A unrollStepsGlobally = {} ... finished");
    }


    public void identifyRelevantTimestepsAndTestThemB(int unrollSteps, int epoch ) {


        int maxBox = timestepRepo.maxBoxB();
        List<Integer> boxesRelevant = Boxing.boxesRelevant(epoch, maxBox);
        log.info("boxesRelevant = {}", boxesRelevant.toString());

        if (boxesRelevant.size() == 0) {
            log.info("identifyRelevantTimestepsAndTestThem ... boxesRelevant.size() == 0 ... finished");
            return;
        }

        if (boxesRelevant.size() > 0 && boxesRelevant.get(0) == 0) {
            boxesRelevant.remove(0);
        }
        if (boxesRelevant.size() == 0) {
            log.info("identifyRelevantTimestepsAndTestThem ... boxesRelevant.size() == 0 ... finished");
            return;
        }

        log.info("identifyRelevantTimestepsAndTestThem boxesRelevant = {}", boxesRelevant.toString());

        Set<ShortTimestep> shortTimesteps = gameBuffer.getShortTimestepSet( );
        List<ShortTimestep> idProjections = shortTimesteps.stream().filter(shortTimestep -> boxesRelevant.contains(shortTimestep.getBoxB())).collect(Collectors.toList());


        log.info("identifyRelevantTimestepsAndTestThem timesteps = {}", idProjections.size());


        RulesBuffer rulesBuffer = new RulesBuffer();
        rulesBuffer.setWindowSize(1000);
        Set<Long> episodeIdsSet = idProjections.stream().map(ShortTimestep::getEpisodeId).collect(Collectors.toSet());
        List<Long> episodeIds = new ArrayList<>(episodeIdsSet);
        log.info("identifyRelevantTimestepsAndTestThem episodeIds = {}", episodeIds.size());
        rulesBuffer.setIds(episodeIds);
        Set<Long> timeStepIds = new HashSet<>(idProjections.stream().map(ShortTimestep::getId).collect(Collectors.toSet()));
        int count = 0;
        for (RulesBuffer.IdWindowIterator iterator = rulesBuffer.new IdWindowIterator(); iterator.hasNext(); ) {
            List<Long> episodeIdsRulesLearningList = iterator.next();
            count += episodeIdsRulesLearningList.size();
            log.info("identifyRelevantTimestepsAndTestThem count episodes = {} of {}", count, episodeIds.size());
            List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(episodeIdsRulesLearningList);
            List<Game> games = convertEpisodeDOsToGames(episodeDOList, config);
            games.stream().forEach(game -> game.getEpisodeDO().getTimeSteps().stream().forEach(timeStepDO
                                    -> timeStepDO.setToBeAnalysed(timeStepIds.contains(timeStepDO.getId())
                            )
                    )
            );
            playService.uOkAnalyseGames( games, false, unrollSteps);


            boolean[][][] bOK = ZipperFunctions.b_OK_From_UOk_in_Episodes(episodeDOList);
            ZipperFunctions.sandu_in_Episodes_From_b_OK(bOK, episodeDOList);


            List<TimeStepDO> relevantTimeSteps = episodeDOList.stream().flatMap(episodeDO -> episodeDO.getTimeSteps().stream()
                            .filter(timeStepDO -> timeStepIds.contains(timeStepDO.getId())))
                    .collect(Collectors.toList());


            relevantTimeSteps.forEach(timeStepDO -> {
                timeStepDO.setUOkTested(true);
            });


            // db update also in uOK and box
            List<Long> idsTsChanged = dbService.updateTimesteps_SandUOkandBoxA(relevantTimeSteps);
            gameBuffer.refreshCache(idsTsChanged);
        }

        log.info("identifyRelevantTimestepsAndTestThem A unrollStepsGlobally = {} ... finished");
    }


//    public int getMinUnrollSteps() {
//        return timestepRepo.minUnrollSteps();
//    }

//    public void testBoxesUntilFail( int unrollStepsGlobally) {
//        boolean allTimeStepsWhichMeansLocally = false;
//        test(allTimeStepsWhichMeansLocally, unrollStepsGlobally, false);
//    }

    public void test( ) {
      test(true, 1, false);
    }

    public void testNewEpisodes( ) {
        test(true, 1, true);
    }

    public void test(int unrollSteps ) {
        test(false, unrollSteps, false);
    }

    private void test(boolean allTimeSteps, int unrollSteps, boolean newEpisodesOnly ) {


        int epoch = networkIOService.getLatestNetworkEpoch();
        log.info("testUnrollRulestate.run(), epoch = {}, unrollSteps = {}, allTimeSteps = {} ", epoch, unrollSteps, allTimeSteps );

        modelService.loadLatestModel(epoch).join();  // TODO: check if this is necessary

        RulesBuffer rulesBuffer = new RulesBuffer();
        rulesBuffer.setWindowSize(1000);
        List<Long> episodeIds = null;
        if (newEpisodesOnly) {
            episodeIds = dbService.getNewEpisodeIds( );
        } else {
            episodeIds =  gameBuffer.getEpisodeIds();
        }

        rulesBuffer.setIds(episodeIds);
        int count = 0;
        for (RulesBuffer.IdWindowIterator iterator = rulesBuffer.new IdWindowIterator(); iterator.hasNext(); ) {
            List<Long> episodeIdsRulesLearningList = iterator.next();
            count += episodeIdsRulesLearningList.size();
            log.info("testUnrollRulestate.run(), count episodes = {} of {}", count, rulesBuffer.getIds().size());
         //   System.out.print("\r");
            List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(episodeIdsRulesLearningList);
          //  log.info("step 1");
            List<Game> games = convertEpisodeDOsToGames(episodeDOList, config);
          //  log.info("step 2");
            games.stream().forEach(game -> game.getEpisodeDO().getTimeSteps().stream().forEach(timeStepDO
                                    -> timeStepDO.setToBeAnalysed(true)
                            )
            );
            playService.uOkAnalyseGames(games, allTimeSteps, unrollSteps  );
         //   log.info("step 3");
            boolean[][][] bOK = ZipperFunctions.b_OK_From_UOk_in_Episodes(episodeDOList);
       //    log.info("step 4");
            ZipperFunctions.sandu_in_Episodes_From_b_OK(bOK, episodeDOList);
       //     log.info("step 5");
            List<TimeStepDO> relevantTimeSteps = episodeDOList.stream().flatMap(episodeDO -> episodeDO.getTimeSteps().stream())
                    .collect(Collectors.toList());
         //   log.info("step 6");
            // db update also in uOK and box
            List<Long> idsTsChanged = dbService.updateTimesteps_SandUOkandBox(relevantTimeSteps);
            gameBuffer.refreshCache(idsTsChanged);
         //   log.info("step 7");

        }

        List<Integer> uOkList = timestepRepo.uOkList();


        log.info("uOkList: {} ", uOkList.toString());
    }


    public void testOneGame(long episodeId , int unrollSteps ) {

        int epoch = networkIOService.getLatestNetworkEpoch();
        boolean allTimeStepsWhichMeansLocally = true;

        timestepRepo.resetUOk();
        modelService.loadLatestModel(epoch).join();
        EpisodeDO episodeDO = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(List.of(episodeId)).get(0);
        List<EpisodeDO> episodeDOList = List.of(episodeDO);
        List<Game> games = convertEpisodeDOsToGames(episodeDOList, config);
        selfPlayGame.uOkAnalyseGame(games.get(0), allTimeStepsWhichMeansLocally, -1);

        boolean[][][] bOK = ZipperFunctions.b_OK_From_UOk_in_Episodes(episodeDOList);
        ZipperFunctions.sandu_in_Episodes_From_b_OK(bOK, episodeDOList);
      //  ZipperFunctions.calculateUnrollSteps(episodeDOList);

        List<TimeStepDO> relevantTimeSteps = episodeDOList.stream().flatMap(episodeDO2 -> episodeDO2.getTimeSteps().stream())
                .collect(Collectors.toList());


        // db update also in uOK and box

        List<Long> idsTsChanged = dbService.updateTimesteps_SandUOkandBox(relevantTimeSteps);
        gameBuffer.refreshCache(idsTsChanged);

    }



}
