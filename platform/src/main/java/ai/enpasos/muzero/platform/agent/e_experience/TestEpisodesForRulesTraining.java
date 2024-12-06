package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.parallelEpisodes.PlayService;
import ai.enpasos.muzero.platform.agent.d_model.djl.RulesBuffer;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.d_model.service.ZipperFunctions;
import ai.enpasos.muzero.platform.agent.e_experience.box.Boxing;
import ai.enpasos.muzero.platform.agent.e_experience.db.DBService;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.agent.e_experience.memory2.ShortTimestep;
import ai.enpasos.muzero.platform.common.DurAndMem;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

        if (startFlag || rulesBuffer == null) {
            // to iterate over the episodes we use RulesBuffer
            rulesBuffer = new RulesBuffer();
            rulesBuffer.setWindowSize(numEpisodesToTest);
            List<Long> episodeIdsToTrain = gameBuffer.getShortTimestepSetFromCacheFillCacheIfEmpty().stream()
                    .mapToLong(ShortTimestep::getId).boxed().collect(Collectors.toList());
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

        gamesToAnalyse.forEach(g -> g.getEpisodeDO().getTimeSteps().forEach(TimeStepDO::memorizeNormedSampleError));
        uOkAnalyseGames(  gamesToAnalyse,  unrollSteps);

        // db update also in uOK and box
        List<TimeStepDO> allTimeSteps = episodeDOList.stream().flatMap(episodeDO -> episodeDO.getTimeSteps().stream())
                .collect(Collectors.toList());
        // TODO check and likely remove the relevant boxes here.
        List<Integer> relevantBoxes = Boxing.boxesRelevant(epoch);
        List<Long> idsTsChanged = dbService.updateTimesteps_SandUOkandBox(allTimeSteps, Boxing.boxesRelevant(epoch), unrollSteps);
        gameBuffer.refreshCache(idsTsChanged, epoch);

        int i = 42;







        return true;
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

}
