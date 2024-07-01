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

import java.util.List;
import java.util.Optional;

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



    public void run( ) {
        run(1  );
    }

    public void identifyRelevantTimestepsAndTestThem(  int unrollSteps) {
        int epoch = networkIOService.getLatestNetworkEpoch();
        int maxBox = timestepRepo.maxBox();
        List<Integer> boxesRelevant = Boxing.boxesRelevant(epoch, maxBox);
        gameBuffer.resetRelevantIds();
        List<IdProjection> idProjections = gameBuffer.getRelevantIds2(boxesRelevant);

     //   modelService.loadLatestModel(epoch).join();   // check


        RulesBuffer rulesBuffer = new RulesBuffer();
        rulesBuffer.setWindowSize(1000);
        List<Long> episodeIds = idProjections.stream().map(IdProjection::getEpisodeId).toList();
        rulesBuffer.setIds(episodeIds);
        for (RulesBuffer.IdWindowIterator iterator = rulesBuffer.new IdWindowIterator(); iterator.hasNext(); ) {
            List<Long> episodeIdsRulesLearningList = iterator.next();
            List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(episodeIdsRulesLearningList);
            List<Game> gameBuffer = convertEpisodeDOsToGames(episodeDOList, config);
            playService.uOkAnalyseGames(gameBuffer, unrollSteps);  // TODO: optimize in analysing only relevant timesteps

            boolean[][][] bOK = ZipperFunctions.b_OK_From_UOk_in_Episodes(episodeDOList);
            ZipperFunctions.sandu_in_Episodes_From_b_OK(bOK, episodeDOList);


            episodeDOList.stream().forEach(episodeDO -> episodeDO.getTimeSteps().stream()
                    .filter(timeStepDO -> idProjections.stream().anyMatch(idProjection -> idProjection.getTimeStepId().equals(timeStepDO.getId())))
                            .forEach(timeStepDO -> {
                        timeStepDO.setUOkTested(true);
                    }));


            // db update also in uOK and box
            dbService.updateEpisodes_SandUOkandBox(episodeDOList, unrollSteps );

        }


    }


    @Data
    @AllArgsConstructor
    public class Result {
        private List<Integer> uOkList;
        private int unrollSteps;
        private long box0;

    }




    public Result run(int unrollsteps  ) {
        int epoch = networkIOService.getLatestNetworkEpoch();

       // timestepRepo.resetBoxAndSAndUOk();  // just for testing
        modelService.loadLatestModel(epoch).join();


        RulesBuffer rulesBuffer = new RulesBuffer();
        rulesBuffer.setWindowSize(1000);
        rulesBuffer.setIds(gameBuffer.getEpisodeIds());
        for (RulesBuffer.IdWindowIterator iterator = rulesBuffer.new IdWindowIterator(); iterator.hasNext(); ) {
            List<Long> episodeIdsRulesLearningList = iterator.next();
            List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(episodeIdsRulesLearningList);
            List<Game> gameBuffer = convertEpisodeDOsToGames(episodeDOList, config);
            playService.uOkAnalyseGames(gameBuffer, unrollsteps);

            boolean[][][] bOK = ZipperFunctions.b_OK_From_UOk_in_Episodes(episodeDOList);
            ZipperFunctions.sandu_in_Episodes_From_b_OK(bOK, episodeDOList);

            // check if and why touching timesteps is necessary (jpa transaction boundaries?)
            episodeDOList.stream().forEach(episodeDO -> episodeDO.getTimeSteps().stream().forEach(timeStepDO -> {
                timeStepDO.setUOkTested(false);
            }));

            // db update also in uOK and box
            dbService.updateEpisodes_SandUOkandBox(episodeDOList, unrollsteps );

        }

        List<Integer> uOkList = timestepRepo.uOkList();
        int unrollSteps = config.getMaxUnrollSteps();
        if (!uOkList.isEmpty()) {
            unrollSteps = uOkList.getFirst() + 1;
            unrollSteps = Math.max(unrollSteps, 1);
        }
      //  int toBeTrained =  toBeTrained(unrollSteps);

        log.info("uOkList: {}, unrollSteps = {}", uOkList.toString(), unrollSteps);
        long box0 = timestepRepo.numBox(0);
        return new Result(uOkList, unrollSteps, box0);
    }


    public void runOneGame(long episodeId, int unrollSteps) {

        int epoch = networkIOService.getLatestNetworkEpoch();

        timestepRepo.resetUOk(  );
        modelService.loadLatestModel(epoch).join();
        EpisodeDO episodeDO = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(List.of(episodeId)).get(0);
        List<EpisodeDO> episodeDOList = List.of(episodeDO);
        List<Game> gameBuffer = convertEpisodeDOsToGames(episodeDOList, config);
        selfPlayGame.uOkAnalyseGame(gameBuffer.get(0), unrollSteps);

        boolean[][][] bOK = ZipperFunctions.b_OK_From_UOk_in_Episodes(episodeDOList);
       ZipperFunctions.sandu_in_Episodes_From_b_OK(bOK, episodeDOList);

        dbService.updateEpisodes_SandUOkandBox(List.of( episodeDO), unrollSteps );

    }

//    public int toBeTrained(int unrollSteps) {
//       return timestepRepo.toBeTrained(unrollSteps).orElse(0);
//    }
}
