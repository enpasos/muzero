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
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.e_experience.GameBuffer.convertEpisodeDOsToGames;

@Slf4j
@Component
public class FillValueTable {
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
        int start;
        int oldStop = -1;
        int n = 10;
        int stop = networkIOService.getLatestNetworkEpoch();
        do {
            start = oldStop + 1;
            oldStop = stop;
            IntStream.range(start, stop + 1).forEach(epoch -> {
                fillValueTableForNetworkOfEpoch(epoch);
                temperatureCalculator.aggregateValueStatisticsUp(epoch, n);
            });
            stop = networkIOService.getLatestNetworkEpoch();
            if (oldStop == stop) {
                try {
                    TimeUnit.MINUTES.sleep(1);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        } while (true); //oldStop != stop);
    }

    public void fillValueTableForNetworkOfEpoch(int epoch) {
        log.info("filling value table for epoch {}", epoch);
        modelService.loadLatestModel(epoch).join();
        valueRepo.deleteValuesOfEpoch(epoch);
        // the network from epoch
        // has seen trainingEpoch 0...epoch
        for (int trainingEpoch = 0; trainingEpoch <= epoch; trainingEpoch++) {
            fillTableForEpochAndTrainingEpoch(epoch, trainingEpoch);
        }


    }

    private void fillTableForEpochAndTrainingEpoch(int epoch, int trainingEpoch) {

     //   valueRepo.deleteValuesOfEpoch(epoch);

        List<Long>  episodeIds0 =  episodeRepo.findAllNonArchivedEpisodeIdsForAnEpoch(trainingEpoch);
        List<Long> episodeIds = timestepRepo.findEpisodeIdsWithoutNonExploringValueForAnEpoch(epoch,episodeIds0);
        if (episodeIds.isEmpty()) return;

        List<EpisodeDO> episodeDOS = dbService.findEpisodeDOswithTimeStepDOsAndValues(episodeIds);

        List<Game> games = convertEpisodeDOsToGames(episodeDOS, config);
        games.stream().forEach(game ->  game.setEpoch(epoch));
        gameProvider.measureValueAndSurprise(games);

        games.stream().forEach(game -> {
            game.getEpisodeDO().getTimeSteps().stream().filter(timestep -> !timestep.isExploring()).forEach(timestep -> {
                double value = timestep.getRootValueFromInitialInference();
                valueRepo.myInsert(epoch, value, timestep.getId());
            });
        });


    }
}
