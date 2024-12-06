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
    public boolean test(int unrollSteps, int numEpisodesToTest, boolean startFlag) {

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
        List<Game> games = convertEpisodeDOsToGames(episodeDOList, config);
        Collections.shuffle(games);


        // list the process steps
        // 1. test the episodes
        // 2. store the results in the db
        // 3. decide which episodes need to be trained and add them to the episodeBuffer
        // 4. return true if there are more episodes to test
        // 5. return false if there are no more episodes to test
        // 6. the next call will start again with step 1
        // 7. the process will be repeated until all episodes are tested
        // 8. the process will be repeated until all episodes are trained





        return true;
    }


    public void reset() {
        rulesBuffer = null;
        iterator = null;
    }



}
