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

package ai.enpasos.muzero;

import ai.djl.Model;
import ai.djl.Device;
import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.gamebuffer.Game;
import ai.enpasos.muzero.network.Network;
import ai.enpasos.muzero.network.NetworkIO;
import ai.enpasos.muzero.play.Action;
import ai.enpasos.muzero.play.MCTS;
import ai.enpasos.muzero.play.Node;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static ai.enpasos.muzero.play.PlayManager.getAllActionsOnDevice;

public class Inference {


    public static void main(String[] args) {

        boolean withMCTS = false;

        String nextAction = aiDecision(new ArrayList<String>(), withMCTS, "trained_networks");

    }

    private static String[] actionNames = {"a3", "b3", "c3", "a2", "b2", "c2", "a1", "b1", "c1"};
    public static String actionIndexToName(int i) {
        return actionNames[i];
    }

    public static int actionNameToIndex(String name) {
        return ArrayUtils.indexOf(actionNames, name.trim());
    }

    public static String aiDecision(List<String> actions, boolean withMCTS, String networkDir) {

        List<Integer> actionInts = actions.stream().map(n -> actionNameToIndex(n)).collect(Collectors.toList());
        return actionIndexToName(aiDecisionInternal(actionInts, withMCTS, networkDir));
    }

    private static int aiDecisionInternal(List<Integer> actions, boolean withMCTS, String networkDir) {
        int actionIndexSelectedByNetwork;
        MuZeroConfig config = MuZeroConfig.getTicTacToeInstance();
        config.setNetworkBaseDir(networkDir);
        config.setInferenceDevice(Device.cpu());
        Game game = getGame(config, actions);


        try (Model model = Model.newInstance(config.getModelName(), config.getInferenceDevice())) {
            Path modelPath = Paths.get("./");

            Network network = new Network(config, model); //, modelPath);

            actionIndexSelectedByNetwork = aiDecision(network, withMCTS, game);

        }
        return actionIndexSelectedByNetwork;
    }


    private static Game getGame(MuZeroConfig config,List<Integer> actions) {
        Game game = config.newGame();
        actions.stream().forEach(a -> game.apply(new Action(config, a)));
        return game;
    }


    private static int aiDecision(@NotNull Network network, boolean withMCTS, Game game) {
        NetworkIO networkOutput = network.initialInference(game.getObservation(network.getNDManager()));
        double aiValue = networkOutput.getValue();
        int actionIndexSelectedByNetwork = -1;
        if (!withMCTS) {
            float maxValue = 0f;
            for (int i = 0; i < networkOutput.getPolicyValues().length; i++) {
                float v = networkOutput.getPolicyValues()[i];
                if (v > maxValue) {
                    maxValue = v;
                    actionIndexSelectedByNetwork = i;
                }
            }
        } else {
            Node root = new Node(0);
            MCTS mcts = new MCTS(game.getConfig());
            List<Action> legalActions = game.legalActions();
            mcts.expandNode(root, game.toPlay(), legalActions, networkOutput, false);
            List<NDArray> actionSpaceOnDevice = getAllActionsOnDevice(network.getConfig(), network.getNDManager());
            mcts.run(root, game.actionHistory(), network, null, actionSpaceOnDevice);

            Action action = mcts.selectActionByMaxFromDistribution(game.getGameDTO().getActionHistory().size(), root, network);
            actionIndexSelectedByNetwork = action.getIndex();
        }
        return actionIndexSelectedByNetwork;
    }
}
