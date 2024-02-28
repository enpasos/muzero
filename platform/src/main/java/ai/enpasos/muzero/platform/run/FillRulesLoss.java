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
        fillRulesLossForNetworkOfEpoch( epoch);
     }

    public void fillRulesLossForNetworkOfEpoch(int epoch) {
        log.info("filling rules loss for epoch {}", epoch);
        modelService.loadLatestModel(epoch).join();
        timestepRepo.deleteLegalActionLoss();
        // the network from epoch
        // has seen trainingEpoch 0...epoch
        for (int trainingEpoch = 0; trainingEpoch <= epoch; trainingEpoch++) {
            fillRulesLossForTrainingEpoch(trainingEpoch);
        }

        timestepRepo.calculateAWeight();
        timestepRepo.calculateAWeight2();

        float weightACumulatedMax = timestepRepo.getWeightACumulatedMax();
        int n = 5;
        for (int i = 0; i <= n; i--) {
            timestepRepo.calculateAWeightClass(i, n, weightACumulatedMax);
        }
    }

    private void fillRulesLossForTrainingEpoch(int trainingEpoch) {

        List<Long> episodeIds = episodeRepo.findAllNonArchivedEpisodeIdsForAnEpoch(trainingEpoch);
        if (episodeIds.isEmpty()) return;

        List<EpisodeDO> episodeDOS = dbService.findEpisodeDOswithTimeStepDOsAndValues(episodeIds);

        List<Game> games = convertEpisodeDOsToGames(episodeDOS, config);

        gameProvider.measureRewardExpectations(games);

        games.stream().forEach(
                game -> game.getEpisodeDO().getTimeSteps().stream().forEach(
                        timestep -> timestepRepo.updateRewardLoss(timestep.getId(), timestep.getRewardLoss(), timestep.getLegalActionLossMax())
                )
        );

    }
}
