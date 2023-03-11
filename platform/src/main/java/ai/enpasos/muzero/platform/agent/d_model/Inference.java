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

package ai.enpasos.muzero.platform.agent.d_model;

import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.platform.agent.d_model.service.ModelService;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.episode.PlayParameters;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.episode.PlayService;
import ai.enpasos.muzero.platform.common.MuZeroException;
import ai.enpasos.muzero.platform.config.DeviceType;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static ai.enpasos.muzero.platform.common.Functions.entropy;
import static ai.enpasos.muzero.platform.common.Functions.selectActionByMaxFromDistribution;
import static ai.enpasos.muzero.platform.common.Functions.toDouble;

@Component
@Slf4j
public class Inference {

    @Autowired
    MuZeroConfig config;


    @Autowired
    PlayService playService;

    @Autowired
    ModelService modelService;




    public int aiDecision(List<Integer> actions, boolean withMCTS, String networkDir) {
        return aiDecision(actions, withMCTS, networkDir, DeviceType.CPU);
    }


    public int aiDecisionForGame(List<Integer> actions, boolean withMCTS, int epoch) {

        Game game = getGame(actions);

        try {

            modelService.loadLatestModel(epoch).get();  // TODO options
            return aiDecision(withMCTS, game).getSecond();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }

    }

    public int[] aiDecisionForGames(List<Game> games, boolean withMCTS, int epoch) {
        return aiDecisionForGames(games, withMCTS, true, epoch);
    }


    // TODO withRandomness
    public int[] aiDecisionForGames(List<Game> games, boolean withMCTS, boolean withRandomness, int epoch) {
        try {
            modelService.loadLatestModel(epoch).get();
            return aiDecision(withMCTS, games).stream().mapToInt(p -> p.getSecond() ).toArray();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }

    }

    public double[][] getInMindValues(int epoch, int[] actions, int extra, int actionspace) {
        modelService.loadLatestModel(epoch).join();
        return getInMindValues( actions, extra, actionspace);
    }


    private double[][] getInMindValues(  int[] actions, int extra, int actionspace) {
        double[][] values = new double[actions.length + 1][actions.length + 1 + extra];
        Game game = config.newGame();
        for (int t = 0; t <= actions.length; t++) {
            NetworkIO infResult = modelService.initialInference(game).join();
            NDArray s = infResult.getHiddenState();
            values[actions.length][t] = infResult.getValue();
            System.arraycopy(values[actions.length], 0, values[t], 0, t + 1);
            for (int r = t; r < actions.length + extra; r++) {
                int action;
                if (r < actions.length) {
                    action = actions[r];
                } else {
                    action = ThreadLocalRandom.current().nextInt(actionspace);
                }
                infResult = modelService.recurrentInference(s, action).join();
                s = infResult.getHiddenState();
                values[t][r + 1] = infResult.getValue();
            }
            if (t < actions.length) game.apply(actions[t]);
        }
        return values;
    }


    public int aiDecision(List<Integer> actions, boolean withMCTS, String networkDir, DeviceType deviceType) {
        try {
            if (networkDir != null) {
                config.setNetworkBaseDir(networkDir);
            }
            config.setInferenceDeviceType(deviceType);
            Game game = getGame(actions);
            modelService.loadLatestModel().get();
            return aiDecision(withMCTS, game).getSecond();
        } catch (Exception e) {
            log.error(e.getMessage());
            throw new MuZeroException(e);
        }
    }

    public double aiStartValue(int epoch) {
        Game game = config.newGame();
        modelService.loadLatestModel(epoch).join();
        return aiDecision(false, game).getFirst();
    }

    public double aiEntropy(List<Integer> actions, String networkDir) {
        config.setNetworkBaseDir(networkDir);
        config.setInferenceDeviceType(DeviceType.CPU);
        Game game = getGame(actions);
        return aiEntropy(List.of(game))[0];
    }

    public double aiValue(List<Integer> actions, String networkDir) {
        config.setNetworkBaseDir(networkDir);
        config.setInferenceDeviceType(DeviceType.CPU);
        Game game = getGame(actions);
        return aiValue(List.of(game))[0];
    }

    public double[] aiEntropy(List<Game> games) {
        modelService.loadLatestModel(-1).join();
        List<NetworkIO> networkOutputs = modelService.initialInference(games).join();
        return Objects.requireNonNull(networkOutputs).stream().mapToDouble(io -> entropy(toDouble(io.getPolicyValues()))).toArray();
    }

    public double[] aiValue(List<Game> games) {
        modelService.loadLatestModel(-1).join();
        List<NetworkIO> networkOutputs = modelService.initialInference(games).join();
        return Objects.requireNonNull(networkOutputs).stream().mapToDouble(NetworkIO::getValue).toArray();
    }



    public Game getGame(List<Integer> actions) {
        Game game = config.newGame();
        actions.forEach(a -> game.apply(config.newAction(a)));
        return game;
    }


    private Pair<Double, Integer> aiDecision( boolean withMCTS, Game game) {
        return aiDecision( withMCTS, List.of(game)).get(0);
    }


    @SuppressWarnings("java:S1135")
    private List<Pair<Double, Integer>> aiDecision( boolean withMCTS, List<Game> gamesInput) {


        List<Game> games = new ArrayList<>();
        for (Game game : gamesInput) {
            games.add(game.copy());
        }

        List<NetworkIO> networkOutputList = null;


        int actionIndexSelectedByNetwork;

        List<Pair<Double, Integer>> result = new ArrayList<>();

        if (!withMCTS) {
         //   modelService.startScope();
            try {
                networkOutputList = modelService.initialInference(games).get();
            } catch (InterruptedException e) {
                throw new MuZeroException(e);
            } catch (ExecutionException e) {
                throw new MuZeroException(e);
            }

            for (int g = 0; g < games.size(); g++) {
                Game game = games.get(g);
                List<Action> legalActions = game.legalActions();
                float[] policyValues = Objects.requireNonNull(networkOutputList).get(g).getPolicyValues();
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

                Action action = selectActionByMaxFromDistribution(distributionInput);
                actionIndexSelectedByNetwork = action.getIndex();
                double aiValue = networkOutputList.get(g).getValue();
                result.add(Pair.create(aiValue, actionIndexSelectedByNetwork));
            }
        //    modelService.endScope();

        } else {

            playService.playGames(games,
                PlayParameters.builder()
                    .render(false)
                    .fastRulesLearning(false)
                    .justInitialInferencePolicy(false)
                    .pRandomActionRawAverage(0)
                    .untilEnd(false)
                    .replay(false)
                    .build());

            List<Action> actions = games.stream().map(g -> config.newAction(g.getGameDTO().getActions().get(g.getGameDTO().getActions().size()-1))).collect(Collectors.toList());

            for (int g = 0; g < games.size(); g++) {
                Game game = games.get(g);
                Action action = actions.get(g);
                actionIndexSelectedByNetwork = action.getIndex();
                List<Float> values = game.getGameDTO().getRootValuesFromInitialInference();
                double aiValue =  values.get(values.size()-1);
                result.add(Pair.create(aiValue, actionIndexSelectedByNetwork));
            }
        }
        return result;
    }



}
