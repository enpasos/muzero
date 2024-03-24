package ai.enpasos.muzero.platform.run;


import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.NetworkIOService;
import ai.enpasos.muzero.platform.agent.e_experience.db.DBService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.ValueRepo;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static ai.enpasos.muzero.platform.agent.e_experience.GameBuffer.convertEpisodeDOsToGames;

@Slf4j
@Component
public class FillRulesLoss {
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



    public void run() {
         int epoch = networkIOService.getLatestNetworkEpoch();
        evaluatedRulesLearningForNetworkOfEpoch( epoch );
     }

    public void evaluatedRulesLearningForNetworkOfEpoch(int epoch ) {
         log.info("evaluate rules learning for epoch {}", epoch);
        modelService.loadLatestModel(epoch).join();
        timestepRepo.deleteRulesLearningResults();
        // the network from epoch
         int offset = 0;
         int limit = 50000;
         boolean existsMore = true;
        int[] changeCount = new int[1];
         do {
             changeCount = new int[1];
             existsMore = evaluateRulesLearning( offset, limit, changeCount );
             offset += limit;
         }  while (existsMore  );

    }
    public void evaluatedRulesLearningForNetworkOfEpochForBox0(int epoch) {
        log.info("evaluate rules learning for epoch {}", epoch);
        modelService.loadLatestModel(epoch).join();
        timestepRepo.deleteRulesLearningResults();
        // the network from epoch
        int offset = 0;
        int limit = 50000;
        boolean existsMore = true;
        int[] changeCount = new int[1];
        do {
            changeCount = new int[1];
            existsMore = evaluateRulesLearning( offset, limit, changeCount, 0);
            offset += limit;
        }  while (existsMore  );

    }
//    public void evaluatedRulesLearningForNetworkOfEpoch(int epoch ) {
//        log.info("evaluate rules learning for epoch {}", epoch);
//        modelService.loadLatestModel(epoch).join();
//        timestepRepo.deleteRulesLearningResults();
//        // the network from epoch
//        int offset = 0;
//        int limit = 50000;
//        boolean existsMore = true;
//        int[] changeCount = new int[1];
//        do {
//            changeCount = new int[1];
//            existsMore = evaluateRulesLearning( offset, limit, changeCount, maxBox);
//            offset += limit;
//        }  while (existsMore  );
//
//    }

    private boolean evaluateRulesLearning( int offset, int limit, int[] changeCount, int maxBox) {
        List<Long> episodeIds = episodeRepo.findAllEpisodeIdsWithBoxSmallerOrEqualsMinBox(limit, offset, maxBox);
        return evaluateRulesLearning(changeCount, episodeIds);
    }

    private boolean evaluateRulesLearning( int offset, int limit, int[] changeCount ) {
        List<Long> episodeIds = episodeRepo.findAllEpisodeIds (limit, offset );
        return evaluateRulesLearning(changeCount, episodeIds);
    }

    private boolean evaluateRulesLearning(int[] changeCount, List<Long> episodeIds) {
        if (episodeIds.isEmpty()) return false;

        List<EpisodeDO> episodeDOS = dbService.findEpisodeDOswithTimeStepDOsAndValues(episodeIds);

        List<Game> games = convertEpisodeDOsToGames(episodeDOS, config);

        gameProvider.measureRewardExpectations(games);


        double thresholdA = config.getLegalActionLossMaxThreshold();
        double thresholdR = config.getRewardLossThreshold();


        games.stream().forEach(
                game -> game.getEpisodeDO().getTimeSteps().stream().forEach(
                        timestep -> {
                            boolean known = timestep.getRewardLoss() < thresholdR && timestep.getLegalActionLossMax() < thresholdA;
                            // box 0, ...: box for learning
                            int box = timestep.getBox();
                            int oldBox = box;
                            box = known ? box + 1 : 0;
                            if (oldBox != box) {
                                changeCount[0]++;
                            }
                            timestepRepo.updateRewardLoss(timestep.getId(), timestep.getRewardLoss(), timestep.getLegalActionLossMax(), box);
                        }
                )
        );
        log.info("changeCount: " + changeCount[0] + " for " + episodeIds.size() + " episodes");
        episodeRepo.updateMinBox(  );
        return true;
    }

    public int numBox( int n) {
        return timestepRepo.numBox(n);
    }
}
