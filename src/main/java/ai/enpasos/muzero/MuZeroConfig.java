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

    // play
    private final int numSimulations;
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
                .numChannels(128)        // 256 in the paper
                .numHiddenStateChannels(3)
                .numResiduals(8)        // 16 in the paper



                // network training
                .numberOfTrainingSteps(80000)  // 1000000 in paper
                .numberOfTrainingStepsPerEpoch(100)  // each "epoch" the network state is saved
                .windowSize(10000)     // 1000000 in the paper
                .batchSize(256)         // in paper 2048   // here: symmetry operations give a multiplication by 8
                .numUnrollSteps(5)      // 5 in paper
                .tdSteps(size * size)         // equals maxMoves equals actionSpaceSize
                .discount(1.0)
                // loss details
                .weightDecay(0.0001f)
                .valueLossWeight(1f)    // 0.25f on reanalyse but 1f on the normal run in the paper
                // network training - adam optimizer
                .lrInit(0.0001f)          // initial learning rate for muzero unplugged  (in paper cos drop to 0)

                // play
                .numSimulations(160)     // 800 in the paper
                .numParallelPlays(250)
                .numPlays(2)
                .numberTrainingStepsOnRandomPlay(0)   // 3000

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



                // with these settings so far it takes 2 1/2 hours
                // let's try to get it faster ...
                .batchSize(128)
                // -> 1:48 hours
                // ...
                .numSimulations(80)
                .numParallelPlays(500)
                .numPlays(1)
                // -> 1:16 hours
                // ...
                .numSimulations(40)
                .numParallelPlays(1000)
                .numPlays(2)
                .numberOfTrainingSteps(45000)
                // -> 0:46

                .build();

    }



    public static MuZeroConfig getGoInstance(int size) {


        // TODO
        // should depend on the size and the correpondingly the average number of moves in a game
        // here 5 for size 5
        // in the paper 30 for size 19
        BiFunction<Integer, Integer, Double> visitSoftmaxTemperature = (numMoves, trainingSteps) -> {
            return (numMoves < 30) ? 1.0 : 0.1;   // TODO:  instead of 0.1 here use max rather than softmax
        };


        MuZeroConfigBuilder builder = MuZeroConfig.builder()
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
                .numHiddenStateChannels(5)  // squeezing the hidden state from c to 5

                   .numResiduals(16)        // 16 in the paper


                // network training
                .numberOfTrainingSteps(300000)  // 1000000 in paper
                .numberOfTrainingStepsPerEpoch(100)  // each "epoch" the network state is saved
                .windowSize(10000)     // 1000000 in the paper

                .numUnrollSteps(5)      // 5 in paper
                .tdSteps(size * size + 1)         // equals maxMoves equals actionSpaceSize
                .discount(1.0)
                // loss details
                .weightDecay(0.0001f)  // in katago 0.00003  in paper 0.0001
                .valueLossWeight(1f)    // 0.25f on reanalyse but 1f on the normal run in the paper
                // network training - adam optimizer
                .lrInit(0.0001f)          // initial learning rate for muzero unplugged  (in paper cos drop to 0)


                // play


                .numSimulations(800)     // 800 in the paper



                .rootDirichletAlpha(0.2)  //  in paper ... go19: 0.03, chess: 0.3, shogi: 0.15 ... looks like alpha * typical no legal moves is about 8-10
                .rootExplorationFraction(0.25)   // as in paper
                .visitSoftmaxTemperatureFn(visitSoftmaxTemperature)
                .knownBounds(new KnownBounds(-1d, 1d))  // as in the paper
                // play - PUCB params from paper
                .pbCInit(1.25)
                .pbCBase(19652)
                // inference device
                .inferenceDevice(Device.gpu())

                // local file based storage
                .outputDir("./memory/go"+ size + "/");



                // non functional size dependence


        // Quatro RTX 5000 - 16 GB - 3072 CudaCores
        switch(size) {
            case 5:
                builder
                        .numberTrainingStepsOnRandomPlay(0)

//                        .numSimulations(800)
//                        .numParallelPlays(10)
//                        .numPlays(5)
//
//                        .numSimulations(10)
//                        .numParallelPlays(500)
//                        .numPlays(2)


                        .numSimulations(20)
                        .numParallelPlays(250)
                        .numPlays(4)

          //              .numSimulations(100)
//                        .numParallelPlays(80)
//                        .numPlays(1)

//                        .numSimulations(200)
//                        .numParallelPlays(50)
//                        .numPlays(8)

//                        .numSimulations(200)
//                        .numParallelPlays(40)
//                        .numPlays(12)

//                        .numSimulations(100)
//                        .numParallelPlays(80)
//                        .numPlays(4)

//                        .numSimulations(100)
//                        .numParallelPlays(80)
//                        .numPlays(4)



                        .batchSize(128)         // in paper 2048   // here: symmetry operations give a multiplication by 8
                        .numChannels(128);        // 256 in the paper
                break;
            case 9:
                builder
                        .numberTrainingStepsOnRandomPlay(3000)
                        .numParallelPlays(3)
                        .numPlays(10)
                        .batchSize(48)         // in paper 2048   // here: symmetry operations give a multiplication by 8
                        .numChannels(128);        // 256 in the paper  // 64 for 5x5
                break;
        }



                // RTX 3090 - 24 GB - 10496 CudaCores
//                switch(size) {
//                    case 5:
//                        builder
//                             .numberTrainingStepsOnRandomPlay(3000)
//                            .numParallelPlays(16)
//                            .numPlays(5)
//                            .batchSize(256)         // in paper 2048   // here: symmetry operations give a multiplication by 8
//                            .numChannels(128);        // 256 in the paper  // 64 for 5x5
//                        break;
//                    case 9:
//                        builder
//                                .numberTrainingStepsOnRandomPlay(3000)
//                            .numParallelPlays(4)
//                            .numPlays(10)
//                            .batchSize(64)         // in paper 2048   // here: symmetry operations give a multiplication by 8
//                            .numChannels(128);        // 256 in the paper  // 64 for 5x5
//                        break;
//                }




                return builder.build();

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
