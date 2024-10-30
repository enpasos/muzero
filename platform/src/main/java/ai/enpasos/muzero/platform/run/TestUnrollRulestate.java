package ai.enpasos.muzero.platform.run;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.parallelEpisodes.PlayService;
import ai.enpasos.muzero.platform.agent.e_experience.box.Boxing;
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
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.TimestepRepo;
import ai.enpasos.muzero.platform.agent.e_experience.memory2.ShortTimestep;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
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
    PlayService playService;

    @Autowired
    GameBuffer gameBuffer;

    public void identifyRelevantTimestepsAndTestThem(int epoch, int unrollSteps) {
        ShortTimestep[] tsList = gameBuffer.getIdsRelevantForTesting(unrollSteps, epoch);
        List<ShortTimestep> relevantShortTimesteps = Arrays.stream(tsList).collect(Collectors.toList());

        log.info("identifyRelevantTimestepsAndTestThem timesteps = {}", relevantShortTimesteps.size());

        Set<Long> episodeIdsSet = relevantShortTimesteps.stream().map(ShortTimestep::getEpisodeId).collect(Collectors.toSet());
        List<Long> episodeIds = new ArrayList<>(episodeIdsSet);
        log.info("identifyRelevantTimestepsAndTestThem episodeIds = {}", episodeIds.size());

        testEpisodesWithRulesBuffer(unrollSteps, episodeIds, relevantShortTimesteps, Boxing.boxesRelevant(epoch), true);

        log.info("identifyRelevantTimestepsAndTestThem ... finished");
    }

    private void test(boolean allTimeSteps, int unrollSteps, boolean newEpisodesOnly, boolean onlyEpisodesThatNeedTo) {
        int epoch = networkIOService.getLatestNetworkEpoch();
        log.info("testUnrollRulestate.run(), epoch = {}, allTimeSteps = {}, newEpisodesOnly = {}, onlyEpisodesThatNeedTo = {} ", epoch, allTimeSteps, newEpisodesOnly, onlyEpisodesThatNeedTo);

        List<Long> episodeIds;
        if (newEpisodesOnly) {
            episodeIds = dbService.getNewEpisodeIds();
            if (episodeIds == null || episodeIds.isEmpty()) {
                log.info("testUnrollRulestate.run(), no new episodes found");
                return;
            }
        } else {
            episodeIds = gameBuffer.getEpisodeIds();
        }
        if (onlyEpisodesThatNeedTo) {
            log.info("episodeIds before filter = {}", episodeIds.size());
            episodeIds = gameBuffer.filterEpisodeIdsByTestNeed(episodeIds, epoch);
            log.info("episodeIds after filter = {}", episodeIds.size());
        }

        testEpisodesWithRulesBuffer(unrollSteps, episodeIds, null, List.of(0), allTimeSteps);
    }

    private void testEpisodesWithRulesBuffer(int unrollSteps, List<Long> episodeIds, List<ShortTimestep> relevantShortTimesteps, List<Integer> relevantBoxes, boolean allTimeSteps) {
        RulesBuffer rulesBuffer = new RulesBuffer();
        rulesBuffer.setWindowSize(1000);
        rulesBuffer.setIds(episodeIds);
        Set<Long> timeStepIds = relevantShortTimesteps != null ? relevantShortTimesteps.stream().map(ShortTimestep::getId).collect(Collectors.toSet()) : null;
        int count = 0;
        for (RulesBuffer.IdWindowIterator iterator = rulesBuffer.new IdWindowIterator(); iterator.hasNext(); ) {
            List<Long> episodeIdsRulesLearningList = iterator.next();
            count += episodeIdsRulesLearningList.size();
            log.info("Processing episodes = {} of {}", count, rulesBuffer.getIds().size());
            processEpisodes(episodeIdsRulesLearningList, unrollSteps, timeStepIds, relevantBoxes, allTimeSteps);
        }
    }

    private void processEpisodes(List<Long> episodeIdsRulesLearningList, int unrollSteps, Set<Long> timeStepIds, List<Integer> relevantBoxes, boolean allTimeSteps) {
        List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(episodeIdsRulesLearningList);
        List<Game> games = convertEpisodeDOsToGames(episodeDOList, config);

        if (timeStepIds != null) {
            games.forEach(game -> game.getEpisodeDO().getTimeSteps().forEach(timeStepDO -> timeStepDO.setToBeAnalysed(timeStepIds.contains(timeStepDO.getId()))));
        } else {
            games.forEach(game -> game.getEpisodeDO().getTimeSteps().forEach(timeStepDO -> timeStepDO.setToBeAnalysed(true)));
        }

        List<TimeStepDO> relevantTimeSteps = episodeDOList.stream().flatMap(episodeDO -> episodeDO.getTimeSteps().stream()
                        .filter(timeStepDO -> timeStepIds == null || timeStepIds.contains(timeStepDO.getId())))
                .collect(Collectors.toList());

        playService.uOkAnalyseGames(games, allTimeSteps, unrollSteps);

        boolean[][][] bOK = ZipperFunctions.b_OK_From_UOk_in_Episodes(episodeDOList);
        ZipperFunctions.sandu_in_Episodes_From_b_OK(bOK, episodeDOList);



        relevantTimeSteps.forEach(timeStepDO -> timeStepDO.setUOkTested(true));

        // db update also in uOK and box
        List<Long> idsTsChanged = dbService.updateTimesteps_SandUOkandBox(relevantTimeSteps, relevantBoxes, unrollSteps);
        gameBuffer.refreshCache(idsTsChanged, networkIOService.getLatestNetworkEpoch());
    }

    public void test() {
        test(true, 1, false, false);
    }

//    public void testEpisodesThatNeedTo() {
//        test(true, 1, false, true);
//        gameBuffer.initNeedsFullTest(false);
//    }

    public void testNewEpisodes() {
        test(true, 1, true, false);
    }

    public void test(int unrollSteps) {
        test(false, unrollSteps, false, false);
    }

//    private @NotNull List<Integer> getBoxesRelevant(int epoch) {
//        List<Integer> boxesRelevant = Boxing.boxesRelevant(epoch);
//        log.info("boxesRelevant (not with 0) = {}", boxesRelevant);
//
//        if (boxesRelevant.isEmpty()) {
//            log.info("identifyRelevantTimestepsAndTestThem ... boxesRelevant.size() == 0 ... finished");
//        } else {
//            log.info("identifyRelevantTimestepsAndTestThem boxesRelevant = {}", boxesRelevant);
//        }
//        return boxesRelevant;
//    }
}
