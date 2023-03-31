package ai.enpasos.muzero.platform.agent.a_loopcontrol.episode;

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
        games.stream().forEach(game -> game.getGameDTO().setTdSteps(config.getTdSteps()));
        if (config.getTrainingTypeKey() == HYBRID) {
            hybridConfiguration(games);
        }

        return playGames( games, playParameters);
    }

    public List<Game> reanalyseGames(int numGames, PlayParameters playParameters, List<Game> games) {

        games.forEach(game -> {
            game.getGameDTO().setTdSteps(config.getTdSteps());
            game.setOriginalGameDTO(game.getGameDTO().copy());
            game.getGameDTO().getPolicyTargets().clear();
            game.getGameDTO().setRootValueTargets(new ArrayList<>());
            game.getGameDTO().setRootEntropyValueTargets(new ArrayList<>());
            game.getGameDTO().setEntropies(new ArrayList<>());
            game.getGameDTO().setMaxEntropies(new ArrayList<>());
            game.getGameDTO().setRootValuesFromInitialInference(new ArrayList<>());
            game.getGameDTO().setRootEntropyValuesFromInitialInference(new ArrayList<>());
            game.getGameDTO().setActions(new ArrayList<>());
            game.getGameDTO().setRewards(new ArrayList<>());
            game.getGameDTO().setObservations(new ArrayList<>());
            game.getGameDTO().setLegalActions(new ArrayList<>());

            game.getGameDTO().getObservations().add(game.getOriginalGameDTO().getObservations().get(0));
            game.getGameDTO().getLegalActions().add(game.getOriginalGameDTO().getLegalActions().get(0));
        });

        return playGames(games, playParameters);
    }

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
        games.stream().forEach(game -> {
            game.getGameDTO().setHybrid(true);
            if (game.getGameDTO().getTHybrid() == -1) {
                game.getGameDTO().setTHybrid(ThreadLocalRandom.current().nextInt(0, gameLength + 1));
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
        playParameters.setAverageGameLength(gameLength);  // TODO rename

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
