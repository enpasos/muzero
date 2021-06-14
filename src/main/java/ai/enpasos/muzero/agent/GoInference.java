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

package ai.enpasos.muzero.agent;

import ai.djl.Device;
import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.gamebuffer.Game;
import ai.enpasos.muzero.agent.fast.model.Network;
import ai.enpasos.muzero.agent.fast.model.NetworkIO;
import ai.enpasos.muzero.agent.slow.play.Action;
import ai.enpasos.muzero.agent.slow.play.MCTS;
import ai.enpasos.muzero.agent.slow.play.Node;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.agent.slow.play.PlayManager.getAllActionsOnDevice;

public class GoInference {






    public static int aiDecision(List<Integer> actions, boolean withMCTS, String networkDir, int size) {
        int actionIndexSelectedByNetwork;
        MuZeroConfig config = MuZeroConfig.getGoInstance(size);
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


    public static Game getGame(MuZeroConfig config, List<Integer> actions) {
        Game game = config.newGame();
        actions.stream().forEach(a -> game.apply(new Action(config, a)));
        return game;
    }


    private static int aiDecision(@NotNull Network network, boolean withMCTS, Game game) {
        // boolean selectByDrawing = false;
        NetworkIO networkOutput = network.initialInference(game.getObservation(network.getNDManager()));
        double aiValue = networkOutput.getValue();
        int actionIndexSelectedByNetwork = -1;
        MCTS mcts = new MCTS(game.getConfig());
        List<Action> legalActions = game.legalActions();
        if (legalActions.size() == 0) return -1;
        if (!withMCTS) {

            float[] policyValues = networkOutput.getPolicyValues();
            List<Pair<Action, Double>> distributionInput =
                    IntStream.range(0, game.getConfig().getActionSpaceSize())
                    .mapToObj(i -> {
                        Action action = new Action(game.getConfig(), i);
                        double v = policyValues[i];
                        return new Pair<Action, Double>(action, v);
                    }).collect(Collectors.toList());

            Action action = mcts.selectActionByDrawingFromDistribution(distributionInput);
            actionIndexSelectedByNetwork = action.getIndex();

        } else {
            Node root = new Node(0);


            mcts.expandNode(root, game.toPlay(), legalActions, networkOutput, false);
            List<NDArray> actionSpaceOnDevice = getAllActionsOnDevice(network.getConfig(), network.getNDManager());
            mcts.run(root, game.actionHistory(), network, null, actionSpaceOnDevice);

            Action action = null;
//            if (selectByDrawing) {
                mcts.selectActionByDrawingFromDistribution(game.getGameDTO().getActionHistory().size(), root, network);
//            } else {
//                mcts.selectActionByMaxFromDistribution(game.getGameDTO().getActionHistory().size(), root, network);
//            }
            actionIndexSelectedByNetwork = action.getIndex();
        }
        return actionIndexSelectedByNetwork;
    }
}
