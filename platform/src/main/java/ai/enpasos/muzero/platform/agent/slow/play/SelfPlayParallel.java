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

package ai.enpasos.muzero.platform.agent.slow.play;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.fast.model.Network;
import ai.enpasos.muzero.platform.agent.fast.model.NetworkIO;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import umontreal.ssj.randvarmulti.DirichletGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.agent.slow.play.PlayManager.getAllActionsOnDevice;
import static ai.enpasos.muzero.platform.environment.EnvironmentBase.render;


@Slf4j
public class SelfPlayParallel {


    private static final RandomStream randomStreamBase = new MRG32k3a("rnd");
    private final @Nullable DirichletGen dg = null;


    public static @NotNull List<Game> playGame(@NotNull MuZeroConfig config, Network network, boolean render, boolean fastRuleLearning,  boolean explorationNoise) {
        long start = System.currentTimeMillis();
        Duration inferenceDuration = new Duration();
        network.debugDump();
        List<Game> gameList = IntStream.rangeClosed(1, config.getNumParallelPlays())
                .mapToObj(i -> config.newGame())
                .collect(Collectors.toList());


        List<Game> gamesDoneList = new ArrayList<>();

        MCTS mcts = new MCTS(config);

        Game justOneOfTheGames = gameList.get(0);


        if (render) {
            System.out.println(justOneOfTheGames.render());
        }
        try (NDManager nDManager =
                     network != null ? network.getNDManager().newSubManager() : null) {
            List<NDArray> actionSpaceOnDevice = getAllActionsOnDevice(network.getConfig(), nDManager);
            network.setActionSpaceOnDevice(actionSpaceOnDevice);
            network.debugDump();
            if (network != null) {
                network.createAndSetHiddenStateNDManager(nDManager, true);
            }
            while (gameList.size() > 0) {

                int indexOfJustOneOfTheGames = gameList.indexOf(justOneOfTheGames);

                // At the root of the search tree we use the representation function to
                // obtain a hidden state given the current observation.
                List<Node> rootList = IntStream.rangeClosed(1, gameList.size())
                        .mapToObj(i -> new Node(config, 0, true))
                        .collect(Collectors.toList());

                inferenceDuration.value -= System.currentTimeMillis();

             //   network.debugDump();
              //  NDManager ndManager2 = nDManager.newSubManager();

              //  network.debugDump();
                List<NetworkIO> networkOutput = fastRuleLearning ? null : network.initialInferenceListDirect(gameList);
                network.debugDump();
                // on this networkOutput p and v are copied to the cpu,
                // while the hiddenstates stay on the gpu


                inferenceDuration.value += System.currentTimeMillis();


                if (render && indexOfJustOneOfTheGames != -1) {
                    justOneOfTheGames.renderNetworkGuess(config, justOneOfTheGames.toPlay(), Objects.requireNonNull(networkOutput).get(indexOfJustOneOfTheGames), false);
                }

                for (int g = 0; g < gameList.size(); g++) {
                    NetworkIO networkIO = null;
                    if (networkOutput != null) {
                        networkIO = networkOutput.get(g);
                    }
                    mcts.expandNode(rootList.get(g),
                            gameList.get(g).toPlay(),
                            gameList.get(g).legalActions(),
                            networkIO, fastRuleLearning, config);
                }

                //  double fraction = fastRuleLearning ? 0.001f : config.getRootExplorationFraction();   // TODO check
                double fraction = config.getRootExplorationFraction();
                if (explorationNoise) {
                    rootList.forEach(root -> addExplorationNoise(fraction, config.getRootDirichletAlpha(), root));

                    if (render && indexOfJustOneOfTheGames != -1) {
                        justOneOfTheGames.renderSuggestionFromPriors(config, rootList.get(indexOfJustOneOfTheGames));
                    }
                }
                List<MinMaxStats> minMaxStatsList = null;
                if (!fastRuleLearning) {
                    minMaxStatsList = mcts.runParallel(rootList,
                            gameList.stream().map(Game::actionHistory).collect(Collectors.toList()),
                            network, inferenceDuration, config.getNumSimulations());
                }


                for (int g = 0; g < gameList.size(); g++) {
                    Game game = gameList.get(g);
                    Node root = rootList.get(g);
                    Action action = null;

                    if (fastRuleLearning) {
                        action = getRandomAction(root, config);
                    } else {
                        action = mcts.selectAction(root, minMaxStatsList.get(g));
                    }
                    game.apply(action);
                    game.storeSearchStatistics(root, fastRuleLearning, minMaxStatsList == null ? new MinMaxStats(config.getKnownBounds()) : minMaxStatsList.get(g));


                    if (render && indexOfJustOneOfTheGames != -1 && g == indexOfJustOneOfTheGames) {
                        List<float[]> childVisitsList = justOneOfTheGames.getGameDTO().getPolicyTarget();
                        float[] childVisits = childVisitsList.get(childVisitsList.size() - 1);
                        justOneOfTheGames.renderMCTSSuggestion(config, childVisits);

                        System.out.println("\n" + game.render());
                    }

                }

                List<Game> newGameDoneList = gameList.stream()
                        .filter(game -> game.terminal() || game.getGameDTO().getActionHistory().size() >= config.getMaxMoves())
                        .collect(Collectors.toList());

                if (render && indexOfJustOneOfTheGames != -1 && justOneOfTheGames.terminal()) {
                    //System.out.println("reward: " + justOneOfTheGames.reward);
                    NetworkIO networkOutput2 = network.initialInferenceDirect(gameList.get(indexOfJustOneOfTheGames));
                    justOneOfTheGames.renderNetworkGuess(config, justOneOfTheGames.toPlay(), networkOutput2, true);
                }

                gamesDoneList.addAll(newGameDoneList);
                gameList.removeAll(newGameDoneList);
            }
        }


        long duration = System.currentTimeMillis() - start;
        log.info("duration game play [ms]: {}", duration);
        log.info("inference duration game play [ms]: {}", inferenceDuration.value);
        log.info("java duration game play [ms]: {}", (duration - inferenceDuration.value));

        return gamesDoneList;
    }


    private static Action getRandomAction(Node root, MuZeroConfig config) {
        SortedMap<Action, Node> children = root.getChildren();
        int index = randomStreamBase.nextInt(0, children.size() - 1);
        return ((Map.Entry<Action, Node>) children.entrySet().toArray()[index]).getKey();
    }








    public static void addExplorationNoise(double rootExplorationFraction, double rootDirichletAlpha, @NotNull Node node) {
        Action[] actions = node.getChildren().keySet().toArray(new Action[0]);
        double[] noise = numpyRandomDirichlet(rootDirichletAlpha, actions.length);
        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];
            Node child = node.getChildren().get(action);
            child.prior = (1 - rootExplorationFraction) * child.prior + rootExplorationFraction * noise[i];
        }
    }

    private static double @NotNull [] numpyRandomDirichlet(double alpha, int dims) {

        double[] alphas = new double[dims];
        Arrays.fill(alphas, alpha);
        DirichletGen dg = new DirichletGen(randomStreamBase, alphas);
        double[] p = new double[dims];

        dg.nextPoint(p);
        return p;
    }
}


