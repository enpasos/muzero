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

package ai.enpasos.muzero.platform.agent;

import ai.djl.Model;
import ai.djl.ndarray.NDManager;
import ai.enpasos.muzero.platform.agent.fast.model.Network;
import ai.enpasos.muzero.platform.agent.fast.model.NetworkIO;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.agent.slow.play.MCTS;
import ai.enpasos.muzero.platform.agent.slow.play.MinMaxStats;
import ai.enpasos.muzero.platform.agent.slow.play.Node;
import ai.enpasos.muzero.platform.config.DeviceType;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.apache.commons.math3.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Component
public class Inference {

    @Autowired
    MuZeroConfig config;

    @Autowired
    MCTS mcts;

    public int aiDecision(List<Integer> actions, boolean withMCTS, String networkDir) {
        return aiDecision(actions, withMCTS, networkDir, DeviceType.CPU);
    }

    public int aiDecision(List<Integer> actions, boolean withMCTS, String networkDir, DeviceType deviceType) {

        int actionIndexSelectedByNetwork;
        config.setNetworkBaseDir(networkDir);
        config.setInferenceDeviceType(deviceType);
        Game game = getGame(actions);


        try (Model model = Model.newInstance(config.getModelName(), config.getInferenceDevice())) {

            Network network = new Network(config, model);
            try (NDManager nDManager = network.getNDManager().newSubManager()) {

                network.initActionSpaceOnDevice(nDManager);
                network.setHiddenStateNDManager(nDManager);

                actionIndexSelectedByNetwork = aiDecision(network, withMCTS, game).getSecond();
            }

        }
        return actionIndexSelectedByNetwork;
    }


    public double aiValue(List<Integer> actions, String networkDir) {
        double valueByNetwork;
        config.setNetworkBaseDir(networkDir);
        config.setInferenceDeviceType(DeviceType.CPU);
        Game game = getGame(actions);


        try (Model model = Model.newInstance(config.getModelName(), config.getInferenceDevice())) {

            Network network = new Network(config, model);
            try (NDManager nDManager = network.getNDManager().newSubManager()) {


                network.setHiddenStateNDManager(nDManager);

                valueByNetwork = aiDecision(network, false, game).getFirst();
            }

        }
        return valueByNetwork;
    }


    public Game getGame(List<Integer> actions) {
        Game game = config.newGame();
        actions.forEach(a -> game.apply(config.newAction(a)));
        return game;
    }


    private Pair<Double, Integer> aiDecision(@NotNull Network network, boolean withMCTS, Game game) {
        NetworkIO networkOutput = network.initialInferenceDirect(game);
        double aiValue = networkOutput.getValue();
        int actionIndexSelectedByNetwork;
        List<Action> legalActions = game.legalActions();
        if (!withMCTS) {

            float[] policyValues = networkOutput.getPolicyValues();
            List<Pair<Action, Double>> distributionInput =
                    IntStream.range(0, game.getConfig().getActionSpaceSize())
                            .filter(i -> {
                                Action action = game.getConfig().newAction(i);
                                return legalActions.contains(action);
                            })
                            .mapToObj(i -> {
                                Action action = game.getConfig().newAction(i);
                                double v = policyValues[i];
                                return new Pair<>(action, v);
                            }).collect(Collectors.toList());

            Action action = mcts.selectActionByMaxFromDistribution(distributionInput);
            actionIndexSelectedByNetwork = action.getIndex();

        } else {
            Node root = new Node(network.getConfig(), 0);


            mcts.expandNode(root, game.toPlay(), legalActions, networkOutput, false);

            MinMaxStats minMaxStats = mcts.run(root, game.actionHistory(), network, null);
            Action action = mcts.selectActionByMax(root, minMaxStats);
            actionIndexSelectedByNetwork = action.getIndex();


        }
        return Pair.create(aiValue, actionIndexSelectedByNetwork);
    }
}
