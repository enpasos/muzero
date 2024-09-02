package ai.enpasos.muzero.platform.run;


import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.NetworkIOService;
import ai.enpasos.muzero.platform.agent.e_experience.db.DBService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static ai.enpasos.muzero.platform.agent.e_experience.GameBuffer.convertEpisodeDOsToGames;

@Slf4j
@Component
public class TestRulesLossFromStart {
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




    public void run() {
         int epoch = networkIOService.getLatestNetworkEpoch();

        log.info("test rules loss for all experienced episodes for epoch {}", epoch);
        modelService.loadLatestModel(epoch).join();

      //  episodeRepo.markAllNonArchived();

        timestepRepo.deleteRulesLearningResults();

        // the network from epoch
         int offset = 0;
         int limit = 50000;
         boolean existsMore = true;
         do {
             existsMore = evaluateBatch( offset, limit );
             offset += limit;
         }  while (existsMore  );

    }
    public void evaluatedBatch(int epoch) {
        log.info("evaluated batch for epoch {}", epoch);

        // the network from epoch
        int offset = 0;
        int limit = 50000;
        boolean existsMore = true;
        do {
            existsMore = evaluateBatch( offset, limit );
            offset += limit;
        }  while (existsMore  );

    }



    private boolean evaluateBatch( int offset, int limit ) {
        List<Long> episodeIds = episodeRepo.findAllEpisodeIds (limit, offset );
        return evaluateBatch(episodeIds);
    }

    private boolean evaluateBatch( List<Long> episodeIds) {
        if (episodeIds.isEmpty()) return false;

        List<EpisodeDO> episodeDOS = dbService.findEpisodeDOswithTimeStepDOs(episodeIds);

        List<Game> games = convertEpisodeDOsToGames(episodeDOS, config);

        gameProvider.measureRewardExpectationsFromStart(games);


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
//                            if (oldBox != box) {
//                                changeCount[0]++;
//                            }
                            timestepRepo.updateRewardLoss(timestep.getId(), timestep.getRewardLoss(), timestep.getLegalActionLossMax(), box);
                        }
                )
        );
    //    log.info("changeCount: " + changeCount[0] + " for " + episodeIds.size() + " episodes");
      //  episodeRepo.updateMinBox(  );
        return true;
    }

    public long numBox( int n) {
        return timestepRepo.numBox(n);
    }
}
