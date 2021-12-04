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

package ai.enpasos.muzero.platform.config;

import ai.djl.Device;
import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import ai.enpasos.muzero.platform.agent.slow.play.KnownBounds;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

@Data
@Builder
public class MuZeroConfig {

    public static boolean hiddenStateRemainOnGPU = false;


    private final PlayerMode playerMode;

    private final @NotNull String modelName;
    private final @NotNull Class<?> gameClass;
    private final @NotNull Class<?> actionClass;
    // game/environment
    private final int size;
    private final int maxMoves;
    private final int boardHeight;
    private final int boardWidth;
    private final int actionSpaceSize;
    // network sizing
    private final int numObservationLayers;
    private final int numActionLayers;
    private final int numChannels;
    private final int numHiddenStateChannels;
    private final int numResiduals;

    private final int windowSize;
    private final int batchSize;
    private final int numUnrollSteps;
    private final int tdSteps;
    private final double discount;
    // loss details
    private final float weightDecay;
    private final float valueLossWeight;
    // network training - adam optimizer
    private final float lrInit;
    private final int numParallelPlays;
    private final int numPlays;
    private final double rootDirichletAlpha;
    private final double rootExplorationFraction;
    private final @NotNull BiFunction<Integer, Integer, Double> visitSoftmaxTemperatureFn;
    private final KnownBounds knownBounds;
    // play - PUCB params from paper
    private final int pbCBase;
    private final double pbCInit;
    private final @NotNull Function<NDArray, List<NDArray>> symmetryFunction;
    private final int symmetryEnhancementFactor;
    // go
    private float komi;
    private boolean absorbingStateDropToZero;
    // play
    private int numSimulations;
    // inference device
    private @NotNull Device inferenceDevice;
    // network training
    private int numberOfTrainingSteps;
    private int numberOfTrainingStepsPerEpoch;
    // local file based storage
    private String outputDir;
    private String networkBaseDir;
    private int numberTrainingStepsOnRandomPlay;

    public Game newGame() {
        try {
            Constructor<?> constructor = gameClass.getConstructor(MuZeroConfig.class);
            return (Game) constructor.newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public Action newAction(int index) {
        Action action = this.newAction();
        action.setIndex(index);
        return action;
    }

    public Action newAction() {
        try {
            Constructor<?> constructor = actionClass.getConstructor(MuZeroConfig.class);
            return (Action) constructor.newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
