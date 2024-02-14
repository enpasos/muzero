package ai.enpasos.muzero.platform.agent.a_loopcontrol.parallelEpisodes;

import ai.enpasos.muzero.platform.agent.b_episode.EpisodeRunner;
import ai.enpasos.muzero.platform.agent.b_episode.PlayParameters;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;

import static ai.enpasos.muzero.platform.config.PlayTypeKey.HYBRID;

@Component
@Slf4j
public class PlayService {

    @Autowired
    EpisodeRunner episodeRunner;

    @Autowired
    ModelService modelService;

    @Autowired
    GameBuffer gameBuffer;


    @Autowired
    MuZeroConfig config;

    public List<Game> playNewGames( int numGames, PlayParameters playParameters) {
        List<Game> games = new ArrayList<>();
        for (int i = 0; i < numGames; i++) {
            Game game = config.newGame(true,true);
            games.add(game);
        }
        games.stream().forEach(game -> game.getEpisodeDO().setTdSteps(config.getTdSteps()));
        if (config.getTrainingTypeKey() == HYBRID) {
            hybridConfiguration(games);
        }

        return playGames( games, playParameters);
    }

    public List<Game> reanalyseGames(int numGames, PlayParameters playParameters, List<Game> games) {

        games.forEach(game -> {
            game.getEpisodeDO().setTdSteps(config.getTdSteps());
            game.setOriginalEpisodeDO(game.getEpisodeDO().copy());
            game.setReanalyse(true);

            game.getEpisodeDO().getTimeSteps().stream().forEach(
                    timeStepDO -> {
                        timeStepDO.setPolicyTarget(null);
                        timeStepDO.setRootValueTarget(0);
                        timeStepDO.setVMix(0);
                        timeStepDO.setRootEntropyValueTarget(0);
                        timeStepDO.setEntropy(0);
                        timeStepDO.setRootValueFromInitialInference(0);
                        timeStepDO.setRootEntropyValueFromInitialInference(0);
                    }
            );
            // just remove the last action!
            game.getEpisodeDO().removeTheLastAction();
            game.getEpisodeDO().getLastTimeStep().setK(1d);
          });

        return playGames(games, playParameters);
    }


//    public List<Game> hybrid2Games(int numGames, PlayParameters playParameters, List<Game> games) {
//
//        // the new played games will be saved to db under a new id
//
//        List<Game> newGames = new ArrayList<>();
//        games.forEach(game -> {
//            Game newGame = config.newGame(true,true);
//            newGame.setOriginalEpisodeDO(game.getEpisodeDO());
//            newGame.getEpisodeDO().setTdSteps(config.getTdSteps());
//            newGame.getEpisodeDO().setTStartNormal(game.getEpisodeDO().getTStartNormal());
//            //     newGame.setHybrid2(true);
//            newGames.add(newGame);
//        });
//
//        return playGames(newGames, playParameters);
//    }

    public List<Game> justReplayGamesWithInitialInference(List<Game> games) {
        return  playGames( games,
            PlayParameters.builder()
            .render(false)
            .fastRulesLearning(false)
            .justReplayWithInitialReference(true)
            .build());
    }

    private void hybridConfiguration(List<Game> games) {
        int gameLength = gameBuffer.getMaxGameLength();
        hybridConfiguration(games, gameLength);
    }

    private void hybridConfiguration(List<Game> games, int gameLength) {
        double fractionOfPureExplorationAdded = this.config.getFractionOfPureExplorationAdded();
        double fractionOfPureExploitationAdded = this.config.getFractionOfPureExploitationAdded();
        games.stream().forEach(game -> {
            game.getEpisodeDO().setHybrid(true);
            if (game.getEpisodeDO().getTStartNormal() == -1) {
                int effectiveT = (int)((gameLength + 1) * (1d + fractionOfPureExplorationAdded ));
                int effectiveStart = - (int)((gameLength + 1) * (fractionOfPureExploitationAdded ));
                game.getEpisodeDO().setTStartNormal(ThreadLocalRandom.current().nextInt(effectiveStart, effectiveT));
            }
        });
    }





    @SuppressWarnings("unchecked")
    public List<Game> playGames(List<Game> games, PlayParameters playParameters) {
        List<Game> gamesReturn = new ArrayList<>();

        modelService.startScope();

        giveOneOfTheGamesADebugFlag(games);
        int gameLength = gameBuffer.getMaxGameLength();
        playParameters.setPRandomActionRawAverage(this.gameBuffer.getPRandomActionRawAverage());
        playParameters.setReferenceGameLength(gameLength);

        CompletableFuture<Game>[] futures = games.stream().map(g ->
                episodeRunner.playGame(playParameters, g)
        ).toArray(CompletableFuture[]::new);

        // wait for all futures to complete
        CompletableFuture.allOf(futures).join();


        // collect games from futures

        for (CompletableFuture<Game> future : futures) {
            try {
                gamesReturn.add(future.get());
            } catch (InterruptedException e) {
                log.warn("Interrupted!", e);
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                throw new MuZeroException(e);
            }
        }


        modelService.endScope();


        return gamesReturn;
    }

    private void giveOneOfTheGamesADebugFlag(List<Game> games) {
        games.stream().forEach(game -> game.setDebug(false));
        if (!games.isEmpty()) {
            games.get(0).setDebug(true);
        }
    }
}
