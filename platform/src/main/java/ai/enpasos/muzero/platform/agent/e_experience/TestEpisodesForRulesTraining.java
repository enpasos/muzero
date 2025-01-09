package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.parallelEpisodes.PlayService;
import ai.enpasos.muzero.platform.agent.d_model.djl.RulesBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.box.Boxing;
import ai.enpasos.muzero.platform.agent.e_experience.db.DBService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.memory2.ShortTimestep;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

import static ai.enpasos.muzero.platform.agent.e_experience.GameBuffer.convertEpisodeDOsToGames;

@Slf4j
@Component
public class TestEpisodesForRulesTraining {

    @Autowired
    MuZeroConfig config;

    @Autowired
    GameBuffer gameBuffer;

    @Autowired
    EpisodeRepo episodeRepo;

    @Autowired
    PlayService playService;

    @Autowired
    DBService dbService;

    RulesBuffer rulesBuffer;


    RulesBuffer.IdWindowIterator iterator;




    // test numEpisodesToTest episodes in a ref list of episodes starting from a cursor
    // list of episodes refs in the full epoch must exist
    // a cursor to the list must exist (hide behind an interator)
    // if startFlag is true, the list is shuffled
    // load the episodes to be tested from the db
    // test all timesteps in the episodes with rollout of unrollSteps
    // store the result in the db
    // deside which of the tested episodes need to be trained and add them to the episodeBuffer
    public boolean test(int unrollSteps, int numEpisodesToTest, boolean startFlag, int epoch) {
        log.info("test: epoch={}, startFlag={}, unrollSteps={}, numEpisodesToTest={} ... ", epoch, startFlag, unrollSteps, numEpisodesToTest);
        if (startFlag) {
            reset();
        }
        if (rulesBuffer == null) {
            // to iterate over the episodes we use RulesBuffer
            rulesBuffer = new RulesBuffer();
            rulesBuffer.setWindowSize(numEpisodesToTest);
            List<Long> episodeIdsToTrain = new ArrayList<>(gameBuffer.getShortTimestepSetFromCacheFillCacheIfEmpty().stream()
                    .mapToLong(ShortTimestep::getEpisodeId).boxed().collect(Collectors.toSet()));
            Collections.shuffle(episodeIdsToTrain);
            rulesBuffer.setIds(episodeIdsToTrain);
        }
        if (iterator == null) {
            iterator = rulesBuffer.new IdWindowIterator();
        }
        if (!iterator.hasNext()) {
            return false;
        }

        List<Long> relatedEpisodeIds = iterator.next();
        List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(relatedEpisodeIds);
        List<Game> gamesToAnalyse = convertEpisodeDOsToGames(episodeDOList, config);
        Collections.shuffle(gamesToAnalyse);


        analyseGames(unrollSteps, epoch, gamesToAnalyse);

        logStatisticalInfoAboutGames("newly tested data", gamesToAnalyse,   unrollSteps,  epoch);


        gamesToAnalyse.forEach(g -> {
             // if any timestep needs training, add the game to the rulesBuffer
            if (g.getEpisodeDO().getTimeSteps().stream().anyMatch(TimeStepDO::isToBeTrained))
                gameBuffer.getRulesBuffer().addGame(g);
        });
        return true;
    }

    public void analyseGames(int unrollSteps, int epoch, List<Game> gamesToAnalyse) {

        gamesToAnalyse.forEach(g -> g.getEpisodeDO().getTimeSteps().forEach(TimeStepDO::memorizeNormedSampleError));
        uOkAnalyseGames(gamesToAnalyse, unrollSteps);

        // db update also in uOK and box
        List<TimeStepDO> allTimeSteps = gamesToAnalyse.stream().flatMap(game -> game.getEpisodeDO().getTimeSteps().stream())
                .collect(Collectors.toList());
        // TODO check and likely remove the relevant boxes here.
        List<Integer> relevantBoxes = Boxing.boxesRelevant(epoch);
        List<Long> idsTsChanged = dbService.updateTimesteps_SandUOkandBox(allTimeSteps, relevantBoxes, unrollSteps);
        gameBuffer.refreshCache(idsTsChanged, epoch);


        gamesToAnalyse.forEach(g -> g.getEpisodeDO().getTimeSteps().forEach(ts ->   ts.setToBeTrained(false)));
        // deside which of the tested episodes need to be trained and add them to the episodeBuffer
        gamesToAnalyse.forEach(g -> {
            boolean needToTrain = g.getEpisodeDO().getTimeSteps().stream().anyMatch(ts -> {
                boolean needsTraining = ts.needsTrainingNow() ;
                ts.setToBeTrained(needsTraining);
                return needsTraining ;
           }
             );
            if (needToTrain) {
                gameBuffer.getRulesBuffer().addGame(g);
            }
        });
    }


    public void reset() {
        rulesBuffer = null;
        iterator = null;
    }

    private void uOkAnalyseGames(  List<Game> bufferGames, int unrollSteps) {

        List<List<Game>> batches = new ArrayList<>();
        int batchSize = config.getNumParallelGamesPlayed();
        for (int i = 0; i < bufferGames.size(); i += batchSize) {
            batches.add(bufferGames.subList(i, Math.min(i + batchSize, bufferGames.size())));
        }
        batches.forEach(batch -> playService.uOkAnalyseGames(batch,  false, unrollSteps, true));

    }

    public void logStatisticalInfoAboutGames(String dataSourceDescription, List<Game> bufferGames, int unrollSteps, int epoch) {
        // count the number of timesteps to be trained
        // calculate the average sample error

        // sortiere isToBeTrained TS nach normedSampleError. Logge dann diese TS mit id, normedSampleError, normedSampleErrorBefore, normedSampleErrorChange, uOk, uOkClosed
        List<TimeStepDO> timesteps = bufferGames.stream().map(g -> g.getEpisodeDO().getTimeSteps().stream().filter(ts -> ts.isToBeTrained()).collect(Collectors.toList())).flatMap(List::stream).collect(Collectors.toList());
        Collections.sort(timesteps, Comparator.comparing(TimeStepDO::getNormedSampleError).reversed());
        timesteps.forEach(ts -> log.info("datasource: {}, id: {}, normedSampleError: {}, normedSampleErrorBefore: {}, normedSampleErrorChange: {}, uOk: {}, uOkClosed: {}",
                dataSourceDescription, ts.getId(), ts.getNormedSampleError(), ts.getNormedSampleErrorBefore(), ts.getSampleErrorChange(), ts.getUOk(), ts.isUOkClosed()));

        int numTimestepsToBeTrained = timesteps.size();
        double avgSampleError = timesteps.stream().mapToDouble(TimeStepDO::getNormedSampleError).average().orElse(0);

        log.info("epoch: {}, unrollSteps: {}, datasource: {}, dataSource episode num: {}, numTimestepsToBeTrained: {}, avgSampleError: {}",
                epoch, unrollSteps, dataSourceDescription, bufferGames.size(), numTimestepsToBeTrained, avgSampleError);

    }

    public void removeGamesWithNoTimestepsToBeTrained(List<Game> bufferGames) {
        bufferGames.removeIf(g -> g.getEpisodeDO().getTimeSteps().stream().noneMatch(TimeStepDO::isToBeTrained));
    }
}
