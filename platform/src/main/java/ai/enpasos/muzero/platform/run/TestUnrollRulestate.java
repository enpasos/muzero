package ai.enpasos.muzero.platform.run;


import ai.enpasos.muzero.platform.agent.a_loopcontrol.parallelEpisodes.PlayService;
import ai.enpasos.muzero.platform.agent.b_episode.SelfPlayGame;
import ai.enpasos.muzero.platform.agent.e_experience.box.Boxes;
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
import ai.enpasos.muzero.platform.agent.e_experience.memory2.ShortEpisode;
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

    public void identifyRelevantTimestepsAndTestThem(int epoch, int unrollSteps ) {


        ShortTimestep[] tsList = gameBuffer.getIdsRelevantForTesting(  unrollSteps, epoch    );
        List<ShortTimestep> relevantShortTimesteps = Arrays.stream(tsList).collect(Collectors.toList());
        List<Long> relevantIds =Arrays.stream(tsList).mapToLong(ShortTimestep::getId).boxed().collect(Collectors.toList());

        log.info("identifyRelevantTimestepsAndTestThem timesteps = {}", relevantShortTimesteps.size());



        Set<Long> episodeIdsSet = relevantShortTimesteps.stream().map(ShortTimestep::getEpisodeId).collect(Collectors.toSet());
        List<Long> episodeIds = new ArrayList<>(episodeIdsSet);
        log.info("identifyRelevantTimestepsAndTestThem episodeIds = {}", episodeIds.size());

        test(unrollSteps, episodeIds, relevantShortTimesteps,    Boxing.boxesRelevant(epoch ));

        log.info("identifyRelevantTimestepsAndTestThem ... finished");
    }

    private void test(int unrollSteps, List<Long> episodeIds, List<ShortTimestep> relevantShortTimesteps , List<Integer> relevantBoxes) {
        List<Long> relevantIds = relevantShortTimesteps.stream().map(ShortTimestep::getId).collect(Collectors.toList());
        RulesBuffer rulesBuffer = new RulesBuffer();
        rulesBuffer.setWindowSize(1000);
        rulesBuffer.setIds(episodeIds);
        Set<Long> timeStepIds = new HashSet<>(relevantShortTimesteps.stream().map(ShortTimestep::getId).collect(Collectors.toSet()));
        int count = 0;
        for (RulesBuffer.IdWindowIterator iterator = rulesBuffer.new IdWindowIterator(); iterator.hasNext(); ) {
            List<Long> episodeIdsRulesLearningList = iterator.next();
            count += episodeIdsRulesLearningList.size();
            log.info("testing episodes = {} of {}", count, episodeIds.size());
            List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(episodeIdsRulesLearningList);
            List<Game> games = convertEpisodeDOsToGames(episodeDOList, config);
            games.stream().forEach(game -> game.getEpisodeDO().getTimeSteps().stream().forEach(timeStepDO
                                    -> timeStepDO.setToBeAnalysed(timeStepIds.contains(timeStepDO.getId())
                            )
                    )
            );
            playService.uOkAnalyseGamesAllTimesteps( games);


            boolean[][][] bOK = ZipperFunctions.b_OK_From_UOk_in_Episodes(episodeDOList);
            ZipperFunctions.sandu_in_Episodes_From_b_OK(bOK, episodeDOList);


            List<TimeStepDO> relevantTimeSteps = episodeDOList.stream().flatMap(episodeDO -> episodeDO.getTimeSteps().stream()
                            .filter(timeStepDO -> timeStepIds.contains(timeStepDO.getId())))
                    .collect(Collectors.toList());


            relevantTimeSteps.forEach(timeStepDO -> {
                if (relevantIds.contains(timeStepDO.getId())) timeStepDO.setUOkTested(true);
                    }
             );


            // db update also in uOK and box
            List<Long> idsTsChanged = dbService.updateTimesteps_SandUOkandBox(relevantTimeSteps, relevantBoxes, unrollSteps);
            gameBuffer.refreshCache(idsTsChanged);
        }
    }

    private @NotNull List<Integer> getBoxesRelevant(int epoch) {
        List<Integer> boxesRelevant;
        int maxBox =  timestepRepo.maxBox();
        boxesRelevant = Boxing.boxesRelevant(epoch);
        log.info("boxesRelevant (not with 0) = {}", boxesRelevant.toString());

        if (boxesRelevant.size() == 0) {
            log.info("identifyRelevantTimestepsAndTestThem ... boxesRelevant.size() == 0 ... finished");
            return boxesRelevant;
        }

        log.info("identifyRelevantTimestepsAndTestThem boxesRelevant = {}", boxesRelevant.toString());
        return boxesRelevant;
    }



    public void test( ) {
      test(true, 1, false, false);

    }

    public void testEpisodesThatNeedTo() {
        test(true, 1, false, true);
        gameBuffer.initNeedsFullTest(false);
    }



    public void testNewEpisodes( ) {
        test(true, 1, true, false);
    }

    public void test(int unrollSteps ) {
        test(false, unrollSteps, false, false);
    }

    private void test(boolean allTimeSteps, int unrollSteps, boolean newEpisodesOnly, boolean onlyEpisodesThatNeedTo)  {


        int epoch = networkIOService.getLatestNetworkEpoch();
        log.info("testUnrollRulestate.run(), epoch = {}, allTimeSteps = {}, newEpisodesOnly = {}, onlyEpisodesThatNeedTo = {} ", epoch,  allTimeSteps, newEpisodesOnly, onlyEpisodesThatNeedTo );

       // List<Integer> boxesRelevant = getBoxesRelevant(epoch);



        List<Long> episodeIds = null;
        if (newEpisodesOnly) {
            episodeIds = dbService.getNewEpisodeIds( );
            if (episodeIds == null || episodeIds.size() == 0) {
                log.info("testUnrollRulestate.run(), no new episodes found");
                return;
            }
        } else {
            episodeIds =  gameBuffer.getEpisodeIds();
        }
        if (onlyEpisodesThatNeedTo) {
            log.info("episodeIds before filter = {}", episodeIds.size());
            episodeIds = gameBuffer.filterEpisodeIdsByTestNeed(episodeIds);
            log.info("episodeIds after filter = {}", episodeIds.size());
        }

        test(allTimeSteps, unrollSteps, episodeIds);

    }

    private void test(boolean allTimeSteps, int unrollSteps, List<Long> episodeIds) {
        RulesBuffer rulesBuffer = new RulesBuffer();
        rulesBuffer.setWindowSize(1000);
        rulesBuffer.setIds(episodeIds);
        int count = 0;
        for (RulesBuffer.IdWindowIterator iterator = rulesBuffer.new IdWindowIterator(); iterator.hasNext(); ) {
            List<Long> episodeIdsRulesLearningList = iterator.next();
            count += episodeIdsRulesLearningList.size();
            log.info("testUnrollRulestate.run(), count episodes = {} of {}", count, rulesBuffer.getIds().size());

            List<EpisodeDO> episodeDOList = episodeRepo.findEpisodeDOswithTimeStepDOsEpisodeDOIdDesc(episodeIdsRulesLearningList);

            List<Game> games = convertEpisodeDOsToGames(episodeDOList, config);

            games.stream().forEach(game -> game.getEpisodeDO().getTimeSteps().stream().forEach(timeStepDO
                                    -> timeStepDO.setToBeAnalysed(true)
                            )
            );
            playService.uOkAnalyseGames(games, allTimeSteps, unrollSteps);

            boolean[][][] bOK = ZipperFunctions.b_OK_From_UOk_in_Episodes(episodeDOList);

            ZipperFunctions.sandu_in_Episodes_From_b_OK(bOK, episodeDOList);

            List<TimeStepDO> relevantTimeSteps = episodeDOList.stream().flatMap(episodeDO -> episodeDO.getTimeSteps().stream())
                    .collect(Collectors.toList());

            relevantTimeSteps.stream().forEach(timeStepDO -> timeStepDO.setUOkTested(true));

            // db update also in uOK and box
            List<Long> idsTsChanged = dbService.updateTimesteps_SandUOkandBox(relevantTimeSteps, List.of(0), unrollSteps);
            gameBuffer.refreshCache(idsTsChanged);
        }
    }




}
