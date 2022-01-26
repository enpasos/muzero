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

package ai.enpasos.muzero.go.selfcritical;

import ai.djl.engine.Engine;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.types.Shape;
import ai.djl.translate.Batchifier;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.enpasos.muzero.platform.agent.fast.model.NetworkIO;
import ai.enpasos.muzero.platform.agent.fast.model.Observation;
import ai.enpasos.muzero.platform.agent.gamebuffer.Game;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


public class SelfCriticalTranslator implements Translator<SelfCriticalDataSet, List<Integer>> {


    @Override
    public @Nullable Batchifier getBatchifier() {
        return null;
    }

    @Override
    public List<Integer> processOutput(TranslatorContext ctx, @NotNull NDList list) {

        List<Integer> result = new ArrayList<>();
        long length = list.get(0).getShape().get(0);
        NDArray softmaxed = list.get(0).softmax(1);
        for (int i = 0; i < length; i++) {
            float[] probabilities = softmaxed.get(i).toFloatArray();
            float max = 0;
            int maxI = -1;
            for (int k = 0; k < probabilities.length; k++) {
                if (probabilities[k] > max) {
                    max = probabilities[k];
                    maxI = k;
                }
            }

            result.add(maxI);
        }
        return result;
    }

    @Override
    public @NotNull NDList processInput(@NotNull TranslatorContext ctx, @NotNull SelfCriticalDataSet dataSet) {
        List<SelfCriticalGame> gameList = dataSet.data;
        int maxFullMoves = dataSet.maxFullMoves;

        int length = gameList.size();

        float[] dataArray = new float[length * 2 * maxFullMoves];

        for (int i = 0; i < length; i++) {

            SelfCriticalGame game = gameList.get(i);
            int fullMove = 0;

            for (Map.Entry<SelfCriticalPosition, Float> entry : game.normalizedEntropyValues.entrySet()) {
                SelfCriticalPosition pos = entry.getKey();
                float entropy = entry.getValue();

                dataArray[i * 2 * maxFullMoves + fullMove + 0] = pos.getPlayer() == OneOfTwoPlayer.PLAYER_A ? 0f : 1f;
                dataArray[i * 2 * maxFullMoves + fullMove + 1] = entropy;


                fullMove++;
            }

        }
        NDArray[]  data = new NDArray[]{ctx.getNDManager().create(dataArray, new Shape(length, 1, 2, dataSet.maxFullMoves))};


        return new NDList(data);
    }


}
