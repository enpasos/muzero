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
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.platform.agent.e_experience.Target;
import ai.enpasos.muzero.platform.agent.a_loopcontrol.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class InputOutputConstruction {

    @Autowired
    MuZeroConfig config;


    public List<NDArray> constructInput(@NotNull NDManager ndManager, int numUnrollSteps, @NotNull List<Sample> batch, boolean withSymmetryEnrichment, boolean isWithConsistencyLoss) {

        List<NDArray> inputs = new ArrayList<>();
        List<NDArray> inputsH = new ArrayList<>();
        List<NDArray> inputsA = new ArrayList<>();
        addObservation(numUnrollSteps, ndManager, batch, inputsH, isWithConsistencyLoss);
        addActionInput(numUnrollSteps, batch, ndManager, inputsA, withSymmetryEnrichment);
        inputs.add(inputsH.get(0));
        IntStream.range(0, inputsA.size()).forEach(i -> {
            inputs.add(inputsA.get(i));
            if (isWithConsistencyLoss) {
                inputs.add(inputsH.get(1 + i));
            }
        });
        return inputs;

    }

    public NDArray symmetryEnhancerReturnNDArray(@NotNull NDArray a) {
        List<NDArray> list = config.getSymmetryType().getSymmetryFunction().apply(a);
        return NDArrays.concat(new NDList(list));
    }

    public NDArray symmetryEnhancerValue(@NotNull NDArray a) {
        List<NDArray> list = new ArrayList<>();
        list.add(a);
        for (int i = 1; i < config.getSymmetryType().getSymmetryEnhancementFactor(); i++) {
            list.add(a.duplicate());
        }
        return NDArrays.concat(new NDList(list));
    }

    public NDArray symmetryEnhancerPolicy(@NotNull NDArray a) {
        Shape oldShape = a.getShape();

        int boardsize = config.getBoardHeight() * config.getBoardWidth();
        NDList splitted = a.split(new long[]{boardsize}, 1);
        NDArray a2 = splitted.get(0);
        Shape oldShape2 = a2.getShape();

        NDArray policyOutput3 = a2.reshape(new Shape(oldShape2.get(0), 1, config.getBoardHeight(), config.getBoardWidth()));


        List<NDArray> ndArrayList = config.getSymmetryType().getSymmetryFunction().apply(policyOutput3);
        List<NDArray> ndArrayList2 = ndArrayList.stream().map(
            transformedA -> {
                NDArray a0 = transformedA.reshape(new Shape(transformedA.getShape().get(0), oldShape2.get(1)));
                if (!splitted.isEmpty()) {
                    NDArray a1 = splitted.get(1);
                    a0 = a0.concat(a1, 1);
                }
                return a0;
            }
        ).collect(Collectors.toList());


        NDArray policyOutput4 = NDArrays.concat(new NDList(ndArrayList2));


        return policyOutput4.reshape(new Shape(policyOutput4.getShape().get(0), oldShape.get(1)));

    }


    private void addObservation(int numUnrollSteps, @NotNull NDManager ndManager, @NotNull List<Sample> batch, @NotNull List<NDArray> inputs, boolean isWithConsistencyLoss) {
        for (int k = 0; k < (isWithConsistencyLoss ? numUnrollSteps + 1: 1); k++) {
            final int kFinal = k;
            List<NDArray> o = batch.stream()
                .map(sample -> {
                    ObservationModelInput observation = sample.getObservations().get(kFinal);
                    return observation.getNDArray(ndManager);
                })
                .collect(Collectors.toList());

            inputs.add(symmetryEnhancerReturnNDArray(NDArrays.stack(new NDList(o))));
        }
    }

    public void addActionInput(int numUnrollSteps, @NotNull List<Sample> batch, @NotNull NDManager nd, @NotNull List<NDArray> inputs, boolean withSymmetryEnrichment ) {

        for (int k = 0; k <    numUnrollSteps ; k++) {

            List<NDArray> list = new ArrayList<>();
            for (Sample s : batch) {
                NDArray aArray;
                if (s.getActionsList().size() > k) {
                    Action action = config.newAction(s.getActionsList().get(k));
                    aArray = action.encode(nd);
                } else {
                    int a = ThreadLocalRandom.current().nextInt(0, config.getActionSpaceSize());

                    Action action = config.newAction(a);
                    aArray = action.encode(nd);
                }
                list.add(aArray);
            }
            NDArray inputMiniBatch2 = NDArrays.stack(new NDList(list));
            if (withSymmetryEnrichment) {
                inputs.add(symmetryEnhancerReturnNDArray(inputMiniBatch2));
            } else {
                inputs.add(inputMiniBatch2);
            }
        }
    }

    @SuppressWarnings("java:S2095")
    public @NotNull List<NDArray> constructOutput(@NotNull NDManager nd, int numUnrollSteps, @NotNull List<Sample> batch) {
        List<NDArray> outputs = new ArrayList<>();
        int actionSize = config.getActionSpaceSize();
        for (int k = 0; k <= numUnrollSteps; k++) {
            int b = 0;
            float[] valueArray = new float[batch.size()];
            float[] rewardArray = new float[batch.size()];

            float[] policyArray = new float[batch.size() * actionSize];
            float[] legalActionsArray =   new float[batch.size() * actionSize]  ;

            for (Sample s : batch) {
                List<Target> targets = s.getTargetList();
                log.trace("target.size(): {}, k: {}, b: {}", targets.size(), k, b);
                Target target = targets.get(k);

                log.trace("valuetarget: {}", target.getValue());
                double scale = 2.0 / config.getValueSpan();
                valueArray[b] = (float) (target.getValue() * scale);


                System.arraycopy(target.getPolicy(), 0, policyArray, b * actionSize, actionSize);
                log.trace("policytarget: {}", Arrays.toString(target.getPolicy()));


                log.trace("legalactionstarget: {}", target.getLegalActions());
                System.arraycopy(target.getLegalActions(), 0, legalActionsArray, b * actionSize, actionSize);


                if (k>0) {
                    log.trace("rewardtarget: {}", target.getReward());
                    if (target.getReward() > 0f) {
                        int i = 42;
                    }

                    scale = 2.0 / config.getValueSpan();
                    rewardArray[b] = (float) (target.getReward() * scale);
                }

                b++;
            }


            NDArray legalActionsOutput2 = nd.create(legalActionsArray).reshape(new Shape(batch.size(), actionSize));
            outputs.add(symmetryEnhancerPolicy(legalActionsOutput2));

            if (k>0) {
                NDArray rewardOutput2 = nd.create(rewardArray).reshape(new Shape(batch.size(), 1));
                outputs.add(symmetryEnhancerValue(rewardOutput2));
            }

            NDArray policyOutput2 = nd.create(policyArray).reshape(new Shape(batch.size(), actionSize));
            outputs.add(symmetryEnhancerPolicy(policyOutput2));

            NDArray valueOutput2 = nd.create(valueArray).reshape(new Shape(batch.size(), 1));
            outputs.add(symmetryEnhancerValue(valueOutput2));

        }
        return outputs;
    }

}

