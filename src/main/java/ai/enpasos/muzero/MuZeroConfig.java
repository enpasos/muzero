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
import ai.enpasos.muzero.environments.go.GoGame;
import ai.enpasos.muzero.environments.tictactoe.TicTacToeGame;
import ai.enpasos.muzero.gamebuffer.Game;
import ai.enpasos.muzero.agent.slow.play.KnownBounds;
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

    // game/environment
    private final int size;
    private final int maxMoves;
    private final int boardHeight;
    private final int boardWidth;
    private final int actionSpaceSize;

    // network sizing
    private final int numObservationLayers;
    private final int numChannels;
    private final int numResiduals;
    private final int windowSize;
    private final int batchSize;
    private final int numUnrollSteps;
    private final int tdSteps;
    private final double discount;
    // loss details
    private final float weightDecay;
    private final float valueLossWeight;
    // network training - sgd optimizer
    private final float lrInit;
    private final double momentum;
    // play
    private int numSimulations;
    private int numParallelPlays;

    private final int numParallelHiddenStates;

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


    public int getNumParallelPlays() {
        if (numSimulations == 0) return numParallelHiddenStates;
        return numParallelHiddenStates / numSimulations;
    }


    public static MuZeroConfig getTicTacToeInstance() {

        BiFunction<Integer, Integer, Double> visitSoftmaxTemperature = (numMoves, trainingSteps) -> {
            return (numMoves < 30) ? 1.0 : 0.0; // always returns 1.0 here as nomMoves always below 30
        };

        int size = 3;

        return MuZeroConfig.builder()
                .modelName("MuZero-TicTacToe")
                .gameClass(TicTacToeGame.class)

                // game/environment
                .maxMoves(size * size) // in a game
                .size(size)
                .boardHeight(size)
                .boardWidth(size)
                .actionSpaceSize(size * size)

                // network sizing
                .numObservationLayers(3)
                .numChannels(32)        // 256 in the paper
                .numResiduals(4)        // 16 in the paper

                // network training
                .numberOfTrainingSteps(100000)  // 1000000 in paper
                .numberOfTrainingStepsPerEpoch(100)  // each "epoch" the network state is saved
                .windowSize(100000)     // 1000000 in the paper
                .batchSize(256)         // in paper 2048   // here: symmetry operations give a multiplication by 8
                .numUnrollSteps(5)      // 5 in paper
                .tdSteps(size * size)         // equals maxMoves equals actionSpaceSize
                .discount(1.0)
                // loss details
                .weightDecay(0.0001f)
                .valueLossWeight(1f)    // 0.25f on reanalyse but 1f on the normal run in the paper
                // network training - sgd optimizer
                .lrInit(0.02f)          // 0.01f in paper for go, 0.1f for chess
                .momentum(0.9f)

                // play
                .numSimulations(50)     // 800 in the paper
                .rootDirichletAlpha(2)  //  in paper ... go: 0.03, chess: 0.3, shogi: 0.15 ... looks like alpha * typical no legal moves is about 10
                .rootExplorationFraction(0.25)   // as in paper
                .visitSoftmaxTemperatureFn(visitSoftmaxTemperature)
                .knownBounds(new KnownBounds(-1d, 1d))  // as in the paper
                // play - PUCB params from paper
                .pbCInit(1.25)
                .pbCBase(19652)
                // inference device
                .inferenceDevice(Device.gpu())

                // local file based storage
                .outputDir("./memory/tictactoe/")

                .build();

    }



    public static MuZeroConfig getGoInstance(int size) {

        BiFunction<Integer, Integer, Double> visitSoftmaxTemperature = (numMoves, trainingSteps) -> {
            return (numMoves < 30) ? 1.0 : 0.1;   // TODO:  instead of 0.1 here use max rather than softmax
        };


        return MuZeroConfig.builder()
                .modelName("MuZero-Go-" + size )
                .gameClass(GoGame.class)

                // game/environment
                .maxMoves(27000) // as in pseudocode
                .size(size)
                .boardHeight(size)
                .boardWidth(size)
                .actionSpaceSize(size * size + 1) // place a stone on the board or pass

                // network sizing
                .numObservationLayers(17)  // 8 history * 2 player + 1 color of next player
                .numChannels(64)        // 256 in the paper
                .numResiduals(16)        // 16 in the paper

                // network training
                .numberOfTrainingSteps(300000)  // 1000000 in paper
                .numberOfTrainingStepsPerEpoch(100)  // each "epoch" the network state is saved
                .windowSize(100000)     // 1000000 in the paper
                .batchSize(256)         // in paper 2048   // here: symmetry operations give a multiplication by 8
                .numUnrollSteps(5)      // 5 in paper
                .tdSteps(size * size + 1)         // equals maxMoves equals actionSpaceSize
                .discount(1.0)
                // loss details
                .weightDecay(0.0001f)
                .valueLossWeight(1f)    // 0.25f on reanalyse but 1f on the normal run in the paper
                // network training - sgd optimizer
                .lrInit(0.01f)          // 0.01f in paper for go, 0.1f for chess
                .momentum(0.9f)

                // play

                .numParallelHiddenStates(4000)   // numSimulations * numParallelPlays

                .numSimulations(100)     // 800 in the paper
//                .numParallelPlay(3)


                .rootDirichletAlpha(0.03)  //  in paper ... go: 0.03, chess: 0.3, shogi: 0.15 ... looks like alpha * typical no legal moves is about 10
                .rootExplorationFraction(0.25)   // as in paper
                .visitSoftmaxTemperatureFn(visitSoftmaxTemperature)
                .knownBounds(new KnownBounds(-1d, 1d))  // as in the paper
                // play - PUCB params from paper
                .pbCInit(1.25)
                .pbCBase(19652)
                // inference device
                .inferenceDevice(Device.gpu())

                // local file based storage
                .outputDir("./memory/go"+ size + "/")

                .build();

    }



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
