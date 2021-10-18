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

import ai.djl.Device;
import ai.enpasos.muzero.agent.slow.play.KnownBounds;
import ai.enpasos.muzero.gamebuffer.Game;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.util.function.BiFunction;

@Data
@Builder
public class MuZeroConfig {

    private final @NotNull String modelName;
    private final @NotNull Class<?> gameClass;

    // go
    private float komi;

    // game/environment
    private final int size;
    private final int maxMoves;
    private final int boardHeight;
    private final int boardWidth;
    private final int actionSpaceSize;

    // network sizing
    private final int numObservationLayers;
    private final int numChannels;
    private final int numHiddenStateChannels;

    private final int numResiduals;
    private final int windowSize;
    private final int batchSize;
    private final int numUnrollSteps;
    private final int tdSteps;
    private final double discount;

    private boolean absorbingStateDropToZero;


    // loss details
    private final float weightDecay;
    private final float valueLossWeight;
    // network training - adam optimizer
    private final float lrInit;

    // play
    private  int numSimulations;
    private final int numParallelPlays;
    private final int numPlays;

    private final double rootDirichletAlpha;
    private final double rootExplorationFraction;
    private final @NotNull BiFunction<Integer, Integer, Double> visitSoftmaxTemperatureFn;
    private final @NotNull KnownBounds knownBounds;
    // play - PUCB params from paper
    private final int pbCBase;
    private final double pbCInit;
    // inference device
    private @NotNull Device inferenceDevice;
    // network training
    private int numberOfTrainingSteps;
    private int numberOfTrainingStepsPerEpoch;
    // local file based storage
    private String outputDir;
    private String networkBaseDir;

    private int numberTrainingStepsOnRandomPlay;







    public @Nullable Game newGame() {

        try {
            Constructor<?> constructor = gameClass.getConstructor(MuZeroConfig.class);
            return (Game) constructor.newInstance(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

    }

}
