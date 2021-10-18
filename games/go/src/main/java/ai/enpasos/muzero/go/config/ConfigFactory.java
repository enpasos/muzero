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

package ai.enpasos.muzero.go.config;

import ai.djl.Device;
import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.slow.play.KnownBounds;
import lombok.extern.slf4j.Slf4j;

import java.util.function.BiFunction;

@Slf4j
public class ConfigFactory {

    public static MuZeroConfig getGoInstance(int size) {


        // TODO
        // should depend on the size and the correpondingly the average number of moves in a game
        // here 5 for size 5
        // in the paper 30 for size 19
        BiFunction<Integer, Integer, Double> visitSoftmaxTemperature = (numMoves, trainingSteps) -> {
            return (numMoves < 30) ? 1.0 : 0.1;   // TODO:  instead of 0.1 here use max rather than softmax
        };


        MuZeroConfig.MuZeroConfigBuilder builder = MuZeroConfig.builder()
                .modelName("MuZero-Go-" + size )
                .gameClass(GoGame.class)

                .komi(0.5f)  // start training


                // game/environment
                .maxMoves(10000) // a high number
                .size(size)
                .boardHeight(size)
                .boardWidth(size)
                .actionSpaceSize(size * size + 1) // place a stone on the board or pass

                // network sizing
                .numObservationLayers(17)  // 8 history * 2 player + 1 color of next player
                .numHiddenStateChannels(19)  // squeezing the hidden state from c to observationLayers + 2

                .numResiduals(16)        // 16 in the paper


                // network training
                .numberOfTrainingSteps(300000)  // 1000000 in paper
                .numberOfTrainingStepsPerEpoch(100)  // each "epoch" the network state is saved
                .windowSize(50000)     // 1000000 in the paper

                .numUnrollSteps(5)      // 5 in paper
                .tdSteps(10000)         // equals maxMoves
                .discount(1.0)
                // loss details
                .weightDecay(0.0001f)  // in katago 0.00003  in paper 0.0001
                .valueLossWeight(1f)    // 0.25f on reanalyse but 1f on the normal run in the paper
                // network training - adam optimizer
                .lrInit(0.0001f)          // initial learning rate for muzero unplugged  (in paper cos drop to 0)
                .absorbingStateDropToZero(true)

                // play


                .numSimulations(800)     // 800 in the paper



                .rootDirichletAlpha(0.03)  //  in paper ... go19: 0.03, chess: 0.3, shogi: 0.15 ... looks like alpha * typical no legal moves is about 8-10
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


                        .numSimulations(12)
                        .numParallelPlays(250)
                        .numPlays(4)


                        .numSimulations(100)
                        .numParallelPlays(100)
                        .numPlays(10)


                        .numSimulations(200)
                        .numParallelPlays(50)
                        .numPlays(20)


                        .komi(6.5f)


//                        .numSimulations(600)
//                        .numParallelPlays(20)
//                        .numPlays(50)


//                        .numSimulations(20)
//                        .numParallelPlays(250)
//                        .numPlays(4)
//
//
//                        .numSimulations(40)
//                        .numParallelPlays(125)
//                        .numPlays(8)
//
////                        .numSimulations(100)
////                        .numParallelPlays(100)
////                        .numPlays(10)
//
//
//                        .numSimulations(200)
//                        .numParallelPlays(50)
//                        .numPlays(20)
//
//
//                        .numSimulations(600)
//                        .numParallelPlays(20)
//                        .numPlays(50)
//
//                        .numSimulations(1000)
//                        .numParallelPlays(10)
//                        .numPlays(100)

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




}
