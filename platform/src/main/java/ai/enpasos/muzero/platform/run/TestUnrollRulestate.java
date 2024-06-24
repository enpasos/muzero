package ai.enpasos.muzero.platform.run;


import ai.enpasos.muzero.platform.agent.a_loopcontrol.parallelEpisodes.PlayService;
import ai.enpasos.muzero.platform.agent.b_episode.SelfPlayGame;
import ai.enpasos.muzero.platform.agent.d_model.djl.RulesBuffer;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.d_model.service.ZipperFunctions;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.NetworkIOService;
import ai.enpasos.muzero.platform.agent.e_experience.db.DBService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.ValueRepo;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

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
        run(1);
    }


    @Data
    @AllArgsConstructor
    public class Result {
        private List<Integer> uOkList;
        private int unrollStepsMin;
        private int unrollStepsMax;

    }


    public void run(int unrollsteps) {
        int epoch = networkIOService.getLatestNetworkEpoch();

        timestepRepo.resetBoxAndSAndUOk();
        modelService.loadLatestModel(epoch).join();


        RulesBuffer rulesBuffer = new RulesBuffer();
        rulesBuffer.setWindowSize(1000);
        rulesBuffer.setEpisodeIds(gameBuffer.getEpisodeIds());
        for (RulesBuffer.EpisodeIdsWindowIterator iterator = rulesBuffer.new EpisodeIdsWindowIterator(); iterator.hasNext(); ) {
            List<Long> episodeIdsRulesLearningList = iterator.next();
        //    boolean save = !iterator.hasNext();
            List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(episodeIdsRulesLearningList);
            List<Game> gameBuffer = convertEpisodeDOsToGames(episodeDOList, config);
            playService.uOkAnalyseGames(gameBuffer, unrollsteps);

            boolean[][][] bOK = ZipperFunctions.b_OK_From_UOk_in_Episodes(episodeDOList);
            ZipperFunctions.sandu_in_Episodes_From_b_OK(bOK, episodeDOList);
//            episodeDOList.stream().forEach(episodeDO -> episodeDO.getTimeSteps().stream().forEach(timeStepDO -> {
//                timeStepDO.setUOkTested(true);
//            }));

            // db update also in uOK and box
            dbService.updateEpisodes_SandUOkandBox(episodeDOList);

        }


        //return getResult();
    }

    public @NotNull Result getResult() {
        List<Integer> uOkList = timestepRepo.uOkList();
        int unrollStepsMin = uOkList.getFirst() +1;
        unrollStepsMin = Math.max(unrollStepsMin, 1);

        int unrollStepsMax = uOkList.getLast() +1;
        unrollStepsMax = Math.min(unrollStepsMax, config.getMaxUnrollSteps());

        log.info("uOkList: {}, unrollStepsMin: {}, unrollStepsMax: {}", uOkList, unrollStepsMin, unrollStepsMax);
        return new Result(uOkList, unrollStepsMin, unrollStepsMax);
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

        dbService.updateEpisodes_SandUOkandBox(List.of( episodeDO));

    }
}
