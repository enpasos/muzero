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

package ai.enpasos.muzero.pegsolitair.config;

import ai.djl.Device;
import ai.djl.ndarray.NDArray;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.PlayerMode;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.function.BiFunction;

@Slf4j
public class PegSolitairConfigFactory {

    public static MuZeroConfig getSolitairInstance() {

        BiFunction<Integer, Integer, Double> visitSoftmaxTemperature = (numMoves, trainingSteps) -> {
            return (numMoves < 30) ? 1.0 : 0.0; // always returns 1.0 here as nomMoves always below 30
        };

        int size = 7;

        return MuZeroConfig.builder()

                .modelName("MuZero-PegSolitair")
                .gameClass(PegSolitairGame.class)
                .actionClass(PegSolitairAction.class)
                .playerMode(PlayerMode.SINGLE_PLAYER)
                .networkWithRewardHead(false)

                // game/environment
                .maxMoves(size * size) // in a game
                .size(size)
                .boardHeight(size)
                .boardWidth(size)
                .actionSpaceSize(size * size * 4)  // point to start from and 4 directions

                // network sizing
                .numObservationLayers(1)
                .numActionLayers(4)  // one for each direction
                .numChannels(128)        // 256 in the paper
                .numHiddenStateChannels(3)
                .numResiduals(16)        // 16 in the paper

                // network training
                .numberOfTrainingSteps(10000)  // 1000000 in paper
                .numberOfTrainingStepsPerEpoch(100)  // each "epoch" the network state is saved
                .windowSize(10000)     // 1000000 in the paper
                .batchSize(64)         // in paper 2048   // here: symmetry operations give a multiplication by 8
                .numUnrollSteps(5)      // 5 in paper
                .tdSteps(size * size)         // equals maxMoves equals actionSpaceSize
                .discount(1.0)
                // loss details
                .weightDecay(0.0001f)  // as in muzero unplugged paper
                .valueLossWeight(1f)    // 0.25f on reanalyse but 1f on the normal run in the paper
                // network training - adam optimizer
                .lrInit(0.0001f)          // initial learning rate for muzero unplugged  (in paper cos drop to 0)
                .absorbingStateDropToZero(true)

                // play
                //      .numSimulations(160)     // 800 in the paper
                //     .numParallelPlays(250)
                //    .numPlays(2)
                .numberTrainingStepsOnRandomPlay(0)   // 3000

                .rootDirichletAlpha(2)  //  in paper ... go: 0.03, chess: 0.3, shogi: 0.15 ... looks like alpha * typical no legal moves is about 10
                .rootExplorationFraction(0.25)   // as in paper
                .visitSoftmaxTemperatureFn(visitSoftmaxTemperature)
                //.knownBounds(new KnownBounds(-1d, 1d))  // no known bounds
                // play - PUCB params from paper
                .pbCInit(1.25)
                .pbCBase(19652)
                // inference device
                .inferenceDevice(Device.gpu())

                // local file based storage
                .outputDir("./memory/")

                .numEpisodes(1)
                .numParallelGamesPlayed(1000)
                .numSimulations(10)
                .windowSize(10000)
                .numChannels(128)
                //  .absorbingStateDropToZero(true)
                .numberOfTrainingStepsPerEpoch(100)

                // faster for integration test
                .absorbingStateDropToZero(true)
                .numberOfTrainingSteps(100000)


                // using the symmetry of the board to enhance the number of games played by the symmetryEnhancementFactor
                .symmetryEnhancementFactor(8)
                .symmetryFunction(a -> {
                    NDArray a2 = a.rotate90(1, new int[]{2, 3});
                    NDArray a3 = a.rotate90(2, new int[]{2, 3});
                    NDArray a4 = a.rotate90(3, new int[]{2, 3});
                    NDArray a5 = a.transpose(0, 1, 3, 2);

                    NDArray a6 = a5.rotate90(1, new int[]{2, 3});
                    NDArray a7 = a5.rotate90(2, new int[]{2, 3});
                    NDArray a8 = a5.rotate90(3, new int[]{2, 3});
                    return List.of(a, a2, a3, a4, a5, a6, a7, a8);
                })


                .build();

    }


}
