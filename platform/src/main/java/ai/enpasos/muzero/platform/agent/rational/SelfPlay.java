/*
 *  Copyright (c) 2021 enpasos GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ai.enpasos.muzero.platform.agent.rational;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.intuitive.Network;
import ai.enpasos.muzero.platform.agent.intuitive.NetworkIO;
import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.GameBuffer;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayTypeKey;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.rational.GumbelFunctions.add;
import static ai.enpasos.muzero.platform.agent.rational.GumbelFunctions.sigmas;
import static ai.enpasos.muzero.platform.common.Functions.entropy;
import static ai.enpasos.muzero.platform.common.Functions.selectActionByDrawingFromDistribution;
import static ai.enpasos.muzero.platform.common.Functions.softmax;
import static ai.enpasos.muzero.platform.common.Functions.toDouble;
import static ai.enpasos.muzero.platform.config.PlayTypeKey.HYBRID;
import static ai.enpasos.muzero.platform.config.PlayTypeKey.REANALYSE;


@Slf4j
@Component
@Data
public class SelfPlay {

    @Autowired
    MuZeroConfig config;


    @Autowired
    GameBuffer gameBuffer;

    private long start;
    private Duration inferenceDuration;
    private List<Game> gameList;
    private List<Game> gamesDoneList;

    private static void clean(@NotNull Node node) {
        if (node.getHiddenState() != null) {
            node.getHiddenState().close();
            node.setHiddenState(null);
        }
        node.getChildren().forEach(SelfPlay::clean);
    }

    public static void storeSearchStatistics(Game game, @NotNull Node root, boolean justPriorValues, MuZeroConfig config, Action selectedAction, MinMaxStats minMaxStats) {

        game.getGameDTO().getRootValueTargets().add((float) root.getImprovedValue());

        float[] policyTarget = new float[config.getActionSpaceSize()];
        if (justPriorValues) {
            root.getChildren().forEach(node -> policyTarget[node.getAction().getIndex()] = (float) node.getPrior());
        } else if (root.getChildren().size() == 1) {
            policyTarget[selectedAction.getIndex()] = 1f;
        } else {

            double[] logits = root.getChildren().stream().mapToDouble(node -> node.getGumbelAction().getLogit()).toArray();

            double[] completedQsNormalized = root.getCompletedQValuesNormalized(minMaxStats);

            int[] actions = root.getChildren().stream().mapToInt(node -> node.getAction().getIndex()).toArray();

            int maxActionVisitCount = root.getChildren().stream().mapToInt(Node::getVisitCount).max().getAsInt();
            double[] raw = add(logits, sigmas(completedQsNormalized, maxActionVisitCount, config.getCVisit(), config.getCScale()));

            double[] improvedPolicy = softmax(raw);


            for (int i = 0; i < raw.length; i++) {
                int action = actions[i];
                double v = improvedPolicy[i];
                policyTarget[action] = (float) v;
            }
        }
        game.getGameDTO().getPolicyTargets().add(policyTarget);
    }

    private static void storeEntropyInfo(List<Game> gamesToApplyAction, List<NetworkIO> networkOutputFinal) {
        IntStream.range(0, gamesToApplyAction.size()).forEach(g -> {
            Game game = gamesToApplyAction.get(g);
            List<Action> legalActions = game.legalActions();
            game.getGameDTO().getMaxEntropies().add((float) Math.log(legalActions.size()));
            if (networkOutputFinal != null) {
                float[] ps = networkOutputFinal.get(g).getPolicyValues();
                game.getGameDTO().getEntropies().add((float) entropy(toDouble(ps)));
            }
        });
    }

    @PostConstruct
    public void postConstruct() {
        init();
    }

    public void init() {
        start = System.currentTimeMillis();
        inferenceDuration = new Duration();
        gameList = IntStream.rangeClosed(1, config.getNumParallelGamesPlayed())
            .mapToObj(i -> config.newGame())
            .collect(Collectors.toList());
        gameList.stream().forEach(game -> {
            game.getGameDTO().setTdSteps(config.getTdSteps());
            game.setPlayTypeKey(this.config.getPlayTypeKey());
        });
        if (config.getTrainingTypeKey() == HYBRID) {
            hybridConfiguration();
        }
        gameList.get(0).setDebug(true);
        gamesDoneList = new ArrayList<>();
    }

    public void init(List<Game> inputGames) {
        start = System.currentTimeMillis();
        inferenceDuration = new Duration();
        gameList = new ArrayList<>();
        gameList.addAll(inputGames);
        gameList.stream().forEach(game -> {
            game.getGameDTO().setTdSteps(config.getTdSteps());
            game.setPlayTypeKey(this.config.getPlayTypeKey());
        });
        if (config.getTrainingTypeKey() == HYBRID) {
            hybridConfiguration();
        }
        if (config.getPlayTypeKey() == REANALYSE) {
            reanalyseConfiguration();
        }
        gamesDoneList = new ArrayList<>();
    }

    private void reanalyseConfiguration() {
        gameList.forEach(game -> {
            game.getGameDTO().getPolicyTargets().clear();
            game.getGameDTO().setRootValueTargets(new ArrayList<>());
            game.getGameDTO().setEntropies(new ArrayList<>());
            game.getGameDTO().setMaxEntropies(new ArrayList<>());
             game.getGameDTO().setRootValuesFromInitialInference(new ArrayList<>());
             game.replayToPosition(0);
        });
    }

    private void hybridConfiguration() {
        int gameLength = gameBuffer.getAverageGameLength();
        hybridConfiguration(gameLength);
    }

    private void hybridConfiguration(int gameLength) {
        gameList.stream().forEach(game -> {
            game.getGameDTO().setHybrid(true);
            if (game.getGameDTO().getTHybrid() == -1) {
                game.getGameDTO().setTHybrid(ThreadLocalRandom.current().nextInt(0, gameLength + 1));
            }
        });
    }

    public void justReplayWithInitialInference(Network network) {
        int indexOfJustOneOfTheGames = getGameList().indexOf(justOneOfTheGames());

        List<Node> rootList = initRootNodes();

        List<NetworkIO> networkOutputFinal = initialInference(network, getGameList(), false, false, indexOfJustOneOfTheGames);


        IntStream.range(0, gameList.size()).forEach(g ->
        {
            Game game = gameList.get(g);
            Node root = rootList.get(g);
            double value = Objects.requireNonNull(networkOutputFinal).get(g).getValue();
            root.setValueFromInitialInference(value);
            game.getGameDTO().getRootValuesFromInitialInference().add((float) value);


        });

        keepTrackOfOpenGamesReplay();

        IntStream.range(0, gameList.size()).forEach(g ->
        {
            Game game = gameList.get(g);

            int nActionsReplayed = game.actionHistory().getActionIndexList().size();
            if (nActionsReplayed < game.getOriginalGameDTO().getActions().size()) {
                int actionIndex = game.getOriginalGameDTO().getActions().get(nActionsReplayed);

                try {
                    game.apply(actionIndex);
                } catch (Exception e) {
                    log.error(e.getMessage(), e);
                    throw new MuZeroException(e);
                }

            }

        });


    }

    public void play(Network network, boolean render, boolean fastRuleLearning, boolean justInitialInferencePolicy, double pRandomActionRawAverage, boolean replay) {
        play(network, true, render, fastRuleLearning, justInitialInferencePolicy, pRandomActionRawAverage, replay);
    }

    @SuppressWarnings("squid:S3776")
    public void play(Network network, boolean withRandomness, boolean render, boolean fastRuleLearning, boolean justInitialInferencePolicy, double pRandomActionRawAverage, boolean replay) {

        if (replay) {
            int i = 42;
        }

        Game justOneOfTheGames = justOneOfTheGames();
      //  justOneOfTheGames.setRecordValueImprovements(true);

        int indexOfJustOneOfTheGames = getGameList().indexOf(justOneOfTheGames);
//        getGameList().stream().forEach(g -> {
//            if (g.isRecordValueImprovements()) {
//                g.getValueImprovements().add(10d);
//            }
//        });

        List<Game> gamesToApplyAction = new ArrayList<>(this.gameList);
        gamesToApplyAction.forEach(game -> game.setActionApplied(false));

        List<NetworkIO> networkOutput = null;
        if (!fastRuleLearning) {
            networkOutput = initialInference(network, gamesToApplyAction, render, false, indexOfJustOneOfTheGames);
        }
        List<NetworkIO> networkOutputFinal = networkOutput;


        storeEntropyInfo(gamesToApplyAction, networkOutputFinal);

        if (justInitialInferencePolicy) {
            playAfterJustWithInitialInference(fastRuleLearning, gamesToApplyAction, networkOutputFinal);
            return;
        }

        if (!fastRuleLearning) {
            IntStream.range(0, gamesToApplyAction.size()).forEach(g -> {
                Game game = gamesToApplyAction.get(g);
                double value = Objects.requireNonNull(networkOutputFinal).get(g).getValue();
                game.getGameDTO().getRootValuesFromInitialInference().add((float) value);

            });
        }

        if (!replay) {
            IntStream.range(0, gamesToApplyAction.size()).forEach(g -> {
                Game game = gamesToApplyAction.get(g);
                boolean[] legalActions = new boolean[config.getActionSpaceSize()];
                for (Action action : game.legalActions()) {
                    legalActions[action.getIndex()] = true;
                }
                game.getGameDTO().getLegalActions().add(legalActions);
            });
        }

        shortCutForGamesWithoutAnOption(gamesToApplyAction, render, fastRuleLearning, network, replay);


        int nGames = gamesToApplyAction.size();
        if (nGames != 0) {

            List<GumbelSearch> searchManagers = gamesToApplyAction.stream().map(game -> {
                game.initSearchManager(pRandomActionRawAverage);
                return game.getSearchManager();
            }).collect(Collectors.toList());

            IntStream.range(0, nGames).forEach(i -> {
                GumbelSearch sm = searchManagers.get(i);
                sm.expandRootNode(fastRuleLearning, fastRuleLearning ? null : Objects.requireNonNull(networkOutputFinal).get(i));
                if (!fastRuleLearning) sm.addExplorationNoise();
                sm.gumbelActionsStart(withRandomness);
                sm.drawCandidateAndAddValueStart();

            });
            if (!fastRuleLearning) {
                do {
                    List<List<Node>> searchPathList = new ArrayList<>();
                    List<GumbelSearch> searchManagersLocal =
                        searchManagers.stream().filter(sm -> !sm.isSimulationsFinished()).collect(Collectors.toList());

                    IntStream.range(0, searchManagersLocal.size()).forEach(i -> searchPathList.add(searchManagersLocal.get(i).search()));

                    if (inferenceDuration != null) inferenceDuration.value -= System.currentTimeMillis();
                    List<NetworkIO> networkOutputList = network.recurrentInference(searchPathList);
                    if (inferenceDuration != null) inferenceDuration.value += System.currentTimeMillis();

                    IntStream.range(0, searchManagersLocal.size()).forEach(i -> {
                        GumbelSearch sm = searchManagersLocal.get(i);
                        sm.expandAndBackpropagate(Objects.requireNonNull(networkOutputList).get(i));
                        sm.next();
                        sm.drawCandidateAndAddValue();
                    });


                    // if there is an equal number of simulations in each game
                    // the loop can be replaced by an explicit loop over the number of simulations
                    // if the simulations are different per game the loop needs to be adjust
                    // as the simulations would go on even for the game where simulation is over
                } while (searchManagers.stream().anyMatch(sm -> !sm.isSimulationsFinished()));

            }
            IntStream.range(0, nGames).forEach(i -> {
                    searchManagers.get(i).storeSearchStatictics(render, fastRuleLearning);
                    if (!replay) {
                        searchManagers.get(i).selectAndApplyAction(render, fastRuleLearning, replay);
                    }
                }
            );
        }
        renderNetworkGuess(network, render, indexOfJustOneOfTheGames);

        keepTrackOfOpenGames(replay);
    }

    @SuppressWarnings({"squid:S3740", "unchecked"})
    private void playAfterJustWithInitialInference(boolean fastRuleLearning, List<Game> gamesToApplyAction, List<NetworkIO> networkOutputFinal) {
        List<Node> roots = new ArrayList<>();
        IntStream.range(0, gamesToApplyAction.size()).forEach(i -> {
            Game game = gamesToApplyAction.get(i);
            List<Action> legalActions = game.legalActions();
            Node root = new Node(config, 0, true);
            roots.add(root);
            root.expandRootNode(game.toPlay(), legalActions, networkOutputFinal.get(i), fastRuleLearning);

            List<Pair<Action, Double>> distributionInput =
                root.getChildren().stream().map(node ->
                    (Pair<Action, Double>) new Pair(node.getAction(), node.getPrior())
                ).collect(Collectors.toList());

            Action action = selectActionByDrawingFromDistribution(distributionInput);

            game.apply(action);
            storeSearchStatistics(game, root, true, config, null, new MinMaxStats(config.getKnownBounds()));
        });

        keepTrackOfOpenGames(false);
    }

    private void shortCutForGamesWithoutAnOption(List<Game> gamesToApplyAction, boolean render, boolean fastRuleLearning, Network network, boolean replay) {
        List<Game> gamesWithOnlyOneAllowedAction = gamesToApplyAction.stream().filter(game -> game.legalActions().size() == 1).collect(Collectors.toList());
        if (gamesWithOnlyOneAllowedAction.isEmpty()) return;

        List<NetworkIO> networkOutput = null;
        if (!fastRuleLearning) {
            networkOutput = initialInference(network, gamesToApplyAction, false, false, 0);
        }
        List<NetworkIO> networkOutputFinal = networkOutput;

        IntStream.range(0, gamesWithOnlyOneAllowedAction.size()).forEach(g -> {
            Game game = gamesWithOnlyOneAllowedAction.get(g);
            Action action = game.legalActions().get(0);
            game.apply(action);


            float value = 0f;
            if (!fastRuleLearning) {
                value = (float) Objects.requireNonNull(networkOutputFinal).get(g).getValue();
            }

            game.getGameDTO().getRootValueTargets().add(value);

            float[] policyTarget = new float[config.getActionSpaceSize()];
            policyTarget[action.getIndex()] = 1f;
            game.getGameDTO().getPolicyTargets().add(policyTarget);
            game.getGameDTO().getPlayoutPolicy().add(policyTarget);
            if (render && game.isDebug()) {
                game.renderMCTSSuggestion(config, policyTarget);
                log.info("\n" + game.render());
            }
            game.setActionApplied(true);
        });

        gamesToApplyAction.removeIf(Game::isActionApplied);
    }


    private void renderNetworkGuess(Network network, boolean render, int indexOfJustOneOfTheGames) {
        if (render && indexOfJustOneOfTheGames != -1 && justOneOfTheGames().terminal()) {
            NetworkIO networkOutput2 = network.initialInferenceDirect(gameList.get(indexOfJustOneOfTheGames));
            justOneOfTheGames().renderNetworkGuess(config, justOneOfTheGames().toPlay(), networkOutput2, true);
        }
    }

    private void keepTrackOfOpenGames(boolean replay) {
        gameList.forEach(game -> game.setDone(
            !replay && game.terminal()
                || !replay && game.getGameDTO().getActions().size() >= config.getMaxMoves()
                || replay && game.getGameDTO().getActions().size() <= game.getGameDTO().getRootValueTargets().size()
        ));
        List<Game> newGameDoneList = gameList.stream()
            .filter(Game::isDone)
            .collect(Collectors.toList());
        gamesDoneList.addAll(newGameDoneList);
        gameList.removeIf(Game::isDone);
    }

    private void keepTrackOfOpenGamesReplay() {
        gameList.forEach(game -> game.setDone(game.getGameDTO().getActions().size() == game.getOriginalGameDTO().getActions().size()));
        List<Game> newGameDoneList = gameList.stream()
            .filter(Game::isDone)
            .collect(Collectors.toList());
        gamesDoneList.addAll(newGameDoneList);
        gameList.removeIf(Game::isDone);
    }

    @Nullable
    private List<NetworkIO> initialInference(Network network, List<Game> gamesToApplyAction, boolean render, boolean fastRuleLearning, int indexOfJustOneOfTheGames) {
        inferenceDuration.value -= System.currentTimeMillis();
        List<NetworkIO> networkOutput = fastRuleLearning ? null : network.initialInferenceListDirect(gamesToApplyAction);
        inferenceDuration.value += System.currentTimeMillis();

        if (render && indexOfJustOneOfTheGames != -1) {
            justOneOfTheGames().renderNetworkGuess(config, justOneOfTheGames().toPlay(), Objects.requireNonNull(networkOutput).get(indexOfJustOneOfTheGames), false);
        }
        return networkOutput;
    }

    @NotNull
    public List<Node> initRootNodes() {
        // At the root of the search tree we use the representation function to
        // obtain a hidden state given the current observation.
        return IntStream.range(0, gameList.size())
            .mapToObj(i -> new Node(config, 0, true))
            .collect(Collectors.toList());
    }

    public Game justOneOfTheGames() {
        return gameList.get(0);
    }

    public boolean notFinished() {
        return !gameList.isEmpty();
    }

    public @NotNull List<Game> playGame(Network network, boolean render, boolean fastRuleLearning, boolean justInitialInferencePolicy) {
        init();
        if (render) {
            log.info(justOneOfTheGames().render());
        }
        runEpisode(network, render, fastRuleLearning, justInitialInferencePolicy, false);
        long duration = System.currentTimeMillis() - getStart();
        log.info("duration game play [ms]: {}", duration);
        log.info("inference duration game play [ms]: {}", getInferenceDuration().value);
        log.info("java duration game play [ms]: {}", (duration - getInferenceDuration().value));
        return getGamesDoneList();
    }

    public @NotNull Game playGameFromCurrentState(Network network, Game replayGame, boolean untilEnd) {
        log.info("playGameFromCurrentState");
        init(List.of(replayGame));
        runEpisode(network, false, false, untilEnd, false);
        long duration = System.currentTimeMillis() - getStart();
        log.info("duration replay [ms]: {}", duration);
        log.info("inference duration replay [ms]: {}", getInferenceDuration().value);
        log.info("java duration replay [ms]: {}", (duration - getInferenceDuration().value));
        return getGameList().get(0);
    }

    public void playOneActionFromCurrentState(Network network, Game replayGame) {
        init(List.of(replayGame));
        play(network, false, false, false,  this.gameBuffer.getPRandomActionRawAverage(), false);
    }

    public @NotNull List<Game> playGamesFromTheirCurrentState(Network network, List<Game> replayGames) {
        log.info("play " + replayGames.size() + " GamesFromTheirCurrentState");
        init(replayGames);
        runEpisode(network, false, false, false, false);
        long duration = System.currentTimeMillis() - getStart();
        log.info("duration replay [ms]: {}", duration);
        log.info("inference duration replay [ms]: {}", getInferenceDuration().value);
        log.info("java duration replay [ms]: {}", (duration - getInferenceDuration().value));
        return getGamesDoneList();
    }

    public @NotNull List<Game> replayGamesBatch(Network network, List<Game> replayGames) {
        log.info("play " + replayGames.size() + " GamesFromTheirCurrentState");
        init(replayGames);
        runEpisode(network, false, false, false, true);
        long duration = System.currentTimeMillis() - getStart();
        log.info("duration replay [ms]: {}", duration);
        log.info("inference duration replay [ms]: {}", getInferenceDuration().value);
        log.info("java duration replay [ms]: {}", (duration - getInferenceDuration().value));
        return getGamesDoneList();
    }


    private void runEpisode(Network network, boolean render, boolean fastRulesLearning, boolean justInitialInferencePolicy, boolean replay) {
        try (NDManager nDManager = network.getNDManager().newSubManager()) {
            List<NDArray> actionSpaceOnDevice = Network.getAllActionsOnDevice(config, nDManager);
            network.setActionSpaceOnDevice(actionSpaceOnDevice);
            network.createAndSetHiddenStateNDManager(nDManager, true);
            int count = 1;
            while (notFinished()) {
                play(network, render, fastRulesLearning, justInitialInferencePolicy,  this.gameBuffer.getPRandomActionRawAverage(), replay);
                log.info("move " + count + " for " + config.getNumParallelGamesPlayed() + " games (where necessary) finished.");
                count++;
            }
        }
    }

    public @NotNull List<Game> justReplayGamesWithInitialInference(Network network, List<Game> inputGames) {
        init(inputGames);

        try (NDManager nDManager = network.getNDManager().newSubManager()) {
            List<NDArray> actionSpaceOnDevice = Network.getAllActionsOnDevice(config, nDManager);
            network.setActionSpaceOnDevice(actionSpaceOnDevice);
            network.createAndSetHiddenStateNDManager(nDManager, true);
            while (notFinished()) {
                justReplayWithInitialInference(network);
            }
        }

        return getGamesDoneList();
    }

    public void playMultipleEpisodes(Network network, boolean render, boolean fastRuleLearning, boolean justInitialInferencePolicy) {
        List<Game> games = new ArrayList<>();
        List<Game>  gamesToReanalyse = null;
        if (config.getPlayTypeKey() == PlayTypeKey.REANALYSE) {
             gamesToReanalyse = gameBuffer.getGamesToReanalyse();

        }
        for (int i = 0; i < config.getNumEpisodes(); i++) {
            List<Game> gamesPart;
            if (config.getPlayTypeKey() == PlayTypeKey.REANALYSE) {
                gamesPart = replayGames(network, gamesToReanalyse);
            } else {
                gamesPart = playGame(network, render, fastRuleLearning, justInitialInferencePolicy);
            }

            log.info("Played {} games parallel, round {}", gamesPart.size(), i);
            games.addAll(gamesPart);
        }
        gameBuffer.addGames(network.getModel(), games, false);
    }


    public List<Game> replayGames(Network network, List<Game> games) {
        log.info("replayGames, {} games", games.size());
        List<List<Game>> gameBatches = ListUtils.partition(games, config.getNumParallelGamesPlayed());

        List<Game> resultGames = new ArrayList<>();
        for (List<Game> gamesLocal : gameBatches) {
            resultGames.addAll(replayGamesBatch(network, gamesLocal));
        }
        return resultGames;
//        log.info("gameBuffer size (before gameBuffer::saveGame): " + gameBuffer.getBuffer().getGames().size());
//        log.info("resultGames size: " + resultGames.size());
//        gameBuffer.addGames(network.getModel(), resultGames, false);
//        log.info("gameBuffer size (after gameBuffer::saveGame): " + gameBuffer.getBuffer().getGames().size());
//        resultGames.clear();
    }

}


