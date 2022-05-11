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
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.rational.GumbelFunctions.add;
import static ai.enpasos.muzero.platform.agent.rational.GumbelFunctions.sigmas;
import static ai.enpasos.muzero.platform.common.Functions.*;


@Slf4j
@Component
@Data
public class SelfPlay {

    @Autowired
    MuZeroConfig config;


    @Autowired
    ReplayBuffer replayBuffer;

    private long start;
    private Duration inferenceDuration;
    private List<Game> gameList;
    private List<Game> gamesDoneList;


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
        gameList.get(0).setDebug(true);
        gamesDoneList = new ArrayList<>();
    }

    public void init(List<Game> inputGames) {
        start = System.currentTimeMillis();
        inferenceDuration = new Duration();
        gameList = new ArrayList<>();
        gameList.addAll(inputGames);
        gamesDoneList = new ArrayList<>();
    }

    public void justReplayWithInitialInference(Network network) {
        int indexOfJustOneOfTheGames = getGameList().indexOf(justOneOfTheGames());

        List<Node> rootList = initRootNodes();

        List<NetworkIO> networkOutputFinal = initialInference(network, getGameList(), false, false, indexOfJustOneOfTheGames);

        IntStream.range(0, gameList.size()).forEach(g ->
        {
            Game game = gameList.get(g);
            Node root = rootList.get(g);
            double value = networkOutputFinal.get(g).getValue();
            root.setValueFromInitialInference(value);
            calculateSurprise(value, game);

            int nActionsReplayed = game.actionHistory().getActionIndexList().size();
            int actionIndex = game.getOriginalGameDTO().getActions().get(nActionsReplayed);

            try {
                game.apply(actionIndex);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
                throw new MuZeroException(e);
            }
            game.getGameDTO().getRootValuesFromInitialInference().add((float) root.getValueFromInitialInference());

        });

        keepTrackOfOpenGames();
    }

    private void calculateSurprise(double value, Game game) {
        int pos = game.getGameDTO().getSurprises().size() - 1;
        if (game.getGameDTO().getRootValuesFromInitialInference().size() > pos && pos >= 0) {
            double valueBefore = (config.getPlayerMode() == PlayerMode.TWO_PLAYERS ? -1 : 1) * game.getGameDTO().getRootValuesFromInitialInference().get(pos);
            double deltaValue = value - valueBefore;
            game.getGameDTO().getSurprises().add((float) (deltaValue * deltaValue));
        } else {
            game.getGameDTO().getSurprises().add(0f);
        }

    }

    public void play(Network network, boolean render, boolean fastRuleLearning, boolean justInitialInferencePolicy) {
        int indexOfJustOneOfTheGames = getGameList().indexOf(justOneOfTheGames());

        List<Game> gamesToApplyAction = new ArrayList<>();
        gamesToApplyAction.addAll(this.gameList);

        shortCutForGamesWithoutAnOption(gamesToApplyAction, render);

        int nGames = gamesToApplyAction.size();
        if (nGames != 0) {

            List<NetworkIO> networkOutput = null;
            if (!fastRuleLearning) {
                networkOutput = initialInference(network, gamesToApplyAction, render, fastRuleLearning, indexOfJustOneOfTheGames);
            }
            List<NetworkIO> networkOutputFinal = networkOutput;


            if (justInitialInferencePolicy) {
                playAfterJustWithInitialInference(fastRuleLearning, gamesToApplyAction, networkOutputFinal);
                return;
            }

            if (!fastRuleLearning) {
                IntStream.range(0, nGames).forEach(g -> {
                    Game game = gamesToApplyAction.get(g);
                    double value = networkOutputFinal.get(g).getValue();
                    calculateSurprise(value, game);
                });
            }

            List<GumbelSearch> searchManagers = gamesToApplyAction.stream().map(game -> {
                game.initSearchManager();
                return game.getSearchManager();
            }).collect(Collectors.toList());

            IntStream.range(0, nGames).forEach(i -> {
                GumbelSearch sm = searchManagers.get(i);
                sm.expandRootNode(fastRuleLearning, fastRuleLearning ? null : networkOutputFinal.get(i));
                sm.gumbelActionsStart();
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
                        sm.expandAndBackpropagate(networkOutputList.get(i));
                        sm.next();
                        sm.drawCandidateAndAddValue();
                    });


                    // if there is an equal number of simulations in each game
                    // the loop can be replaced by an explicit loop over the number of simulations
                    // if the simulations are different per game the loop needs to be adjust
                    // as the simulations would go on even for the game where simulation is over
                } while (searchManagers.stream().anyMatch(sm -> !sm.isSimulationsFinished()));

            }
            IntStream.range(0, nGames).forEach(i ->
                searchManagers.get(i).selectAndApplyActionAndStoreSearchStatistics(render, fastRuleLearning)
            );

        }
        renderNetworkGuess(network, render, indexOfJustOneOfTheGames);

        keepTrackOfOpenGames();
    }

    private void playAfterJustWithInitialInference(boolean fastRuleLearning, List<Game> gamesToApplyAction, List<NetworkIO> networkOutputFinal) {
        List<Node> roots = new ArrayList<>();
        IntStream.range(0, gamesToApplyAction.size()).forEach(i -> {
            Game game = gamesToApplyAction.get(i);
            List<Action> legalActions = game.legalActions();
            Node root = new Node(config, 0, true);
            roots.add(root);
            root.expandRootNode(game.toPlay(), legalActions, networkOutputFinal.get(i), fastRuleLearning);

            List<Pair<Action, Double>> distributionInput =
                root.getChildren().stream().map(node -> {
                        Pair<Action, Double> obj = new Pair(node.getAction(), (Double) node.getPrior());
                        return obj;
                    }
                ).collect(Collectors.toList());

            Action action = selectActionByDrawingFromDistribution(distributionInput);

            game.apply(action);
            storeSearchStatistics(game, root, true, config, null, new MinMaxStats(config.getKnownBounds()));
        });

        keepTrackOfOpenGames();
        return;
    }

    private void shortCutForGamesWithoutAnOption(List<Game> gamesToApplyAction, boolean render) {
        List<Game> gamesWithOnlyOneAllowedAction = gamesToApplyAction.stream().filter(game -> game.legalActions().size() == 1).collect(Collectors.toList());
        if (gamesWithOnlyOneAllowedAction.isEmpty()) return;


        gamesToApplyAction.removeAll(gamesWithOnlyOneAllowedAction);

        gamesWithOnlyOneAllowedAction.stream().forEach(game -> {
            Action action = game.legalActions().get(0);
            game.apply(action);

            float[] policyTarget = new float[config.getActionSpaceSize()];
            policyTarget[action.getIndex()] = 1f;

            game.getGameDTO().getPolicyTargets().add(policyTarget);
            if (game.getSearchManager() != null) {
                Node root = game.getSearchManager().getRoot();
                float value = (float) root.getValueFromInitialInference();
                game.getGameDTO().getRootValuesFromInitialInference().add(value);
                calculateSurprise(value, game);

            }

            if (render && game.isDebug()) {
                game.renderMCTSSuggestion(config, policyTarget);
                log.debug("\n" + game.render());
            }

        });


    }

    private void renderNetworkGuess(Network network, boolean render, int indexOfJustOneOfTheGames) {
        if (render && indexOfJustOneOfTheGames != -1 && justOneOfTheGames().terminal()) {
            NetworkIO networkOutput2 = network.initialInferenceDirect(gameList.get(indexOfJustOneOfTheGames));
            justOneOfTheGames().renderNetworkGuess(config, justOneOfTheGames().toPlay(), networkOutput2, true);
        }
    }

    private void keepTrackOfOpenGames() {
        List<Game> newGameDoneList = gameList.stream()
            .filter(game -> game.terminal() || game.getGameDTO().getActions().size() >= config.getMaxMoves())
            .collect(Collectors.toList());

        gamesDoneList.addAll(newGameDoneList);
        gameList.removeAll(newGameDoneList);
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


    private static void clean(@NotNull Node node) {
        if (node.getHiddenState() != null) {
            node.getHiddenState().close();
            node.setHiddenState(null);
        }
        node.getChildren().forEach(SelfPlay::clean);
    }

    public @NotNull List<Game> playGame(Network network, boolean render, boolean fastRuleLearning, boolean justInitialInferencePolicy) {
        init();
        if (render) {
            log.debug(justOneOfTheGames().render());
        }
        runEpisode(network, render, fastRuleLearning, justInitialInferencePolicy);
        long duration = System.currentTimeMillis() - getStart();
        log.info("duration game play [ms]: {}", duration);
        log.info("inference duration game play [ms]: {}", getInferenceDuration().value);
        log.info("java duration game play [ms]: {}", (duration - getInferenceDuration().value));
        return getGamesDoneList();
    }

    public @NotNull Game playGameFromCurrentState(Network network, Game replayGame, boolean untilEnd) {
        log.info("playGameFromCurrentState");
        init(List.of(replayGame));
        runEpisode(network, false, false, untilEnd);
        long duration = System.currentTimeMillis() - getStart();
        log.info("duration replay [ms]: {}", duration);
        log.info("inference duration replay [ms]: {}", getInferenceDuration().value);
        log.info("java duration replay [ms]: {}", (duration - getInferenceDuration().value));
        return getGameList().get(0);
    }

    public void playOneActionFromCurrentState(Network network, Game replayGame) {
        init(List.of(replayGame));
        play(network, false, false, false);
    }

    public @NotNull List<Game> playGamesFromTheirCurrentState(Network network, List<Game> replayGames) {
        log.info("play " + replayGames.size() + " GamesFromTheirCurrentState");
        init(replayGames);
        runEpisode(network, false, false, false);
        long duration = System.currentTimeMillis() - getStart();
        log.info("duration replay [ms]: {}", duration);
        log.info("inference duration replay [ms]: {}", getInferenceDuration().value);
        log.info("java duration replay [ms]: {}", (duration - getInferenceDuration().value));
        return getGamesDoneList();
    }

    private void runEpisode(Network network, boolean render, boolean fastRulesLearning, boolean justInitialInferencePolicy) {
        runEpisode(network, render, fastRulesLearning, true, justInitialInferencePolicy);
    }

    private void runEpisode(Network network, boolean render, boolean fastRulesLearning, boolean untilEnd, boolean justInitialInferencePolicy) {
        try (NDManager nDManager = network.getNDManager().newSubManager()) {
            List<NDArray> actionSpaceOnDevice = Network.getAllActionsOnDevice(config, nDManager);
            network.setActionSpaceOnDevice(actionSpaceOnDevice);
            network.createAndSetHiddenStateNDManager(nDManager, true);
            int count = 1;
            while (notFinished() && (untilEnd || count == 1)) {
                play(network, render, fastRulesLearning, justInitialInferencePolicy);
                log.info("mcts run " + count + " for " + config.getNumParallelGamesPlayed() + " games finished.");
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
        IntStream.range(0, config.getNumEpisodes()).forEach(i ->
        {
            List<Game> games = playGame(network, render, fastRuleLearning, justInitialInferencePolicy);
            games.forEach(replayBuffer::saveGame);

            log.info("Played {} games parallel, round {}", config.getNumParallelGamesPlayed(), i);
        });
    }


    public void replayGamesToEliminateSurprise(Network network, List<Game> gamesToReplay) {
        log.info("replayGamesToEliminateSurprise, {} games", gamesToReplay.size());
        List<List<Game>> gameBatches = ListUtils.partition(gamesToReplay, config.getNumParallelGamesPlayed());

        List<Game> resultGames = new ArrayList<>();
        for (List<Game> games : gameBatches) {
            resultGames.addAll(playGamesFromTheirCurrentState(network, games));
        }
        resultGames.forEach(replayBuffer::saveGame);
    }

    public static void storeSearchStatistics(Game game, @NotNull Node root, boolean justPriorValues, MuZeroConfig config, Action selectedAction, MinMaxStats minMaxStats) {

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
                root.getChildren().get(i).setImprovedPolicyValue(v);  // for debugging
                policyTarget[action] = (float) v;
            }
        }
        game.getGameDTO().getPolicyTargets().add(policyTarget);
        game.getGameDTO().getRootValuesFromInitialInference().add((float) root.getValueFromInitialInference());
    }

}


