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

package ai.enpasos.muzero.play;

import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.environments.OneOfTwoPlayer;
import ai.enpasos.muzero.gamebuffer.Game;
import ai.enpasos.muzero.network.Network;
import ai.enpasos.muzero.network.NetworkIO;
import lombok.extern.slf4j.Slf4j;
import umontreal.ssj.randvarmulti.DirichletGen;
import umontreal.ssj.rng.MRG32k3a;
import umontreal.ssj.rng.RandomStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.environments.EnvironmentBaseBoardGames.render;


@Slf4j
public class SelfPlayParallel {


    private static final RandomStream randomStreamBase = new MRG32k3a("rnd");
    private DirichletGen dg = null;


    public static List<Game> playGame(MuZeroConfig config, Network network, boolean render, boolean fastRuleLearning, int gameNo, List<NDArray> actionSpaceOnDevice) {
        long start = System.currentTimeMillis();
        Duration inferenceDuration = new Duration();
        List<Game> gameList = IntStream.rangeClosed(1, gameNo)
                .mapToObj(i -> config.newGame())
                .collect(Collectors.toList());


        List<Game> gamesDoneList = new ArrayList<>();

        MCTS mcts = new MCTS(config);

        Game justOneOfTheGames = gameList.get(0);


        if (render) {
            System.out.println(justOneOfTheGames.render());
        }

        while (gameList.size() > 0) {

            int indexOfJustOneOfTheGames = gameList.indexOf(justOneOfTheGames);

            // At the root of the search tree we use the representation function to
            // obtain a hidden state given the current observation.
            List<Node> rootList = IntStream.rangeClosed(1, gameList.size())
                    .mapToObj(i -> new Node(0))
                    .collect(Collectors.toList());

            inferenceDuration.value -= System.currentTimeMillis();


            List<NetworkIO> networkOutput = fastRuleLearning ? null : network.initialInferenceList(
                    gameList.stream().map(g -> g.getObservation(network.getNDManager())).collect(Collectors.toList())
            );

            // on this networkOutput p and v are copied to the cpu,
            // while the hiddenstates stay on the gpu


            inferenceDuration.value += System.currentTimeMillis();

            if (render && indexOfJustOneOfTheGames != -1) {
                renderNetworkGuess(config, justOneOfTheGames.toPlay(), networkOutput.get(indexOfJustOneOfTheGames), false);
            }

            for (int g = 0; g < gameList.size(); g++) {
                mcts.expandNode(rootList.get(g),
                        gameList.get(g).toPlay(),
                        gameList.get(g).legalActions(),
                        networkOutput.get(g), fastRuleLearning);
            }

            double fraction = fastRuleLearning ? 0.001f : config.getRootEplorationFraction();

            rootList.forEach(root -> addExplorationNoise(fraction, config.getRootDirichletAlpha(), root));


            if (render && indexOfJustOneOfTheGames != -1) {
                renderSuggestionFromPriors(config, rootList.get(indexOfJustOneOfTheGames));
            }

            if (!fastRuleLearning) {
                mcts.runParallel(rootList,
                        gameList.stream().map(game -> game.actionHistory()).collect(Collectors.toList()),
                        network, inferenceDuration, actionSpaceOnDevice, indexOfJustOneOfTheGames);
            }


            for (int g = 0; g < gameList.size(); g++) {
                Game game = gameList.get(g);
                Node root = rootList.get(g);
                Action action = mcts.selectActionByDrawingFromDistribution(game.getGameDTO().getActionHistory().size(), root, network);

                game.apply(action);

                game.storeSearchStatistics(root, fastRuleLearning);


                if (render && indexOfJustOneOfTheGames != -1 && g == indexOfJustOneOfTheGames) {
                    List<float[]> childVisitsList = justOneOfTheGames.getGameDTO().getChildVisits();
                    float[] childVisits = childVisitsList.get(childVisitsList.size() - 1);
                    renderMCTSSuggestion(config, justOneOfTheGames.toPlay(), childVisits);

                    System.out.println("\n" + game.render());
                }

            }

            List<Game> newGameDoneList = gameList.stream()
                    .filter(game -> game.terminal() || game.getGameDTO().getActionHistory().size() >= config.getMaxMoves())
                    .collect(Collectors.toList());

            if (render && indexOfJustOneOfTheGames != -1 && justOneOfTheGames.terminal()) {
                //System.out.println("reward: " + justOneOfTheGames.reward);
                NetworkIO networkOutput2 = fastRuleLearning ? null : network.initialInference(gameList.get(indexOfJustOneOfTheGames).getObservation(network.getNDManager()));
                renderNetworkGuess(config, justOneOfTheGames.toPlay(), networkOutput2, true);
            }


            gamesDoneList.addAll(newGameDoneList);
            gameList.removeAll(newGameDoneList);


        }


        long duration = System.currentTimeMillis() - start;
        log.info("duration game play [ms]: {}", duration);
        log.info("inference duration game play [ms]: {}", inferenceDuration.value);
        log.info("java duration game play [ms]: {}", (duration - inferenceDuration.value));

        return gamesDoneList;
    }

    private static void renderMCTSSuggestion(MuZeroConfig config, Player toPlay, float[] childVisits) {
        String[][] values = new String[config.getBoardHeight()][config.getBoardWidth()];
        System.out.println("");
        System.out.println("mcts suggestion:");
        for (int i = 0; i < childVisits.length; i++) {
            values[Action.getRow(config, i)][Action.getCol(config, i)] = String.format("%2d", Math.round(100.0 * childVisits[i])) + "%";
        }
        System.out.println(render(config, values));

    }

    private static void renderNetworkGuess(MuZeroConfig config, Player toPlay, NetworkIO networkOutput, boolean gameOver) {
        String[][] values = new String[config.getBoardHeight()][config.getBoardWidth()];
        if (networkOutput != null) {
            double v = networkOutput.getValue();
            double p = (v + 1) / 2 * 100;
            int percent = (int) Math.round(p);
            System.out.println("");
            System.out.println("network guess:");
            if (!gameOver) {
                for (int i = 0; i < networkOutput.getPolicyValues().length; i++) {
                    values[Action.getRow(config, i)][Action.getCol(config, i)] = String.format("%2d", Math.round(100.0 * networkOutput.getPolicyValues()[i])) + "%";  // because softmax
                }
                System.out.println(render(config, values));

            }
            System.out.println("Estimated chance for " + ((OneOfTwoPlayer) toPlay).getSymbol() + " to win: " + percent + "%");

        }
    }

    private static void renderSuggestionFromPriors(MuZeroConfig config, Node node) {
        String[][] values = new String[config.getBoardHeight()][config.getBoardWidth()];
        System.out.println("");
        System.out.println("with exploration noise suggestion:");
        for (int i = 0; i < config.getActionSpaceSize(); i++) {
            Action a = new Action(config, i);
            float value = 0f;
            if (node.getChildren().containsKey(a)) {
                value = (float) node.getChildren().get(a).getPrior();
            }
            values[Action.getRow(config, i)][Action.getCol(config, i)]
                    = String.format("%2d", Math.round(100.0 * value)) + "%";
        }
        System.out.println(render(config, values));

    }


    public static void addExplorationNoise(double rootEplorationFraction, double rootDirichletAlpha, Node node) {
        Action[] actions = node.getChildren().keySet().toArray(new Action[0]);
        double[] noise = numpyRandomDirichlet(rootDirichletAlpha, actions.length);
        for (int i = 0; i < actions.length; i++) {
            Action action = actions[i];
            Node child = node.getChildren().get(action);
            child.prior = (1 - rootEplorationFraction) * child.prior + rootEplorationFraction * noise[i];
        }
    }

    private static double[] numpyRandomDirichlet(double alpha, int dims) {

        double[] alphas = new double[dims];
        Arrays.fill(alphas, alpha);
        DirichletGen dg = new DirichletGen(randomStreamBase, alphas);
        double[] p = new double[dims];

        dg.nextPoint(p);
        return p;
    }
}


