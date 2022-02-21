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

package ai.enpasos.muzero.platform.agent.intuitive;


import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.platform.agent.memorize.Target;
import ai.enpasos.muzero.platform.agent.rational.Action;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class InputOutputConstruction {

    @Autowired
    MuZeroConfig config;

    public List<NDArray> constructInput(@NotNull NDManager nd, int numUnrollSteps, Observation observation, List<Integer> actionsList, boolean withSymmetryEnrichment) {

        List<NDArray> inputs = new ArrayList<>();


        Sample sample = Sample.builder()
            .observation(observation)
            .actionsList(actionsList)
            .build();
        List<Sample> batch = new ArrayList<>();
        batch.add(sample);

        addHiddenStateInput(nd, batch, inputs);
        addActionInput(numUnrollSteps, batch, nd, inputs, withSymmetryEnrichment);

        return inputs;

    }

    public List<NDArray> constructInput(@NotNull NDManager ndManager, int numUnrollSteps, @NotNull List<Sample> batch, boolean withSymmetryEnrichment) {

        List<NDArray> inputs = new ArrayList<>();

        addHiddenStateInput(ndManager, batch, inputs);
        addActionInput(numUnrollSteps, batch, ndManager, inputs, withSymmetryEnrichment);

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


    private void addHiddenStateInput(@NotNull NDManager ndManager, @NotNull List<Sample> batch, @NotNull List<NDArray> inputs) {
        List<NDArray> o = batch.stream()
            .map(sample -> sample.getObservation().getNDArray(ndManager))
            .collect(Collectors.toList());

        inputs.add(symmetryEnhancerReturnNDArray(NDArrays.stack(new NDList(o))));
    }

    public void addActionInput(int numUnrollSteps, @NotNull List<Sample> batch, @NotNull NDManager nd, @NotNull List<NDArray> inputs, boolean withSymmetryEnrichment) {

        for (int k = 0; k < numUnrollSteps; k++) {

            List<NDArray> list = new ArrayList<>();
            for (Sample s : batch) {
                NDArray aArray;
                if (s.getActionsList().size() > k) {
                    Action action = config.newAction(s.getActionsList().get(k));
                    aArray = action.encode(nd);
                } else {
                    aArray = nd.zeros(new Shape(1, config.getBoardHeight(), config.getBoardWidth()));
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

    public @NotNull List<NDArray> constructOutput(@NotNull NDManager nd, int numUnrollSteps, @NotNull List<Sample> batch) {
        List<NDArray> outputs = new ArrayList<>();
        for (int k = 0; k <= numUnrollSteps; k++) {
            List<NDArray> policyList = new ArrayList<>();
            List<NDArray> valueList = new ArrayList<>();
            int b = 0;
            for (Sample s : batch) {
                List<Target> targets = s.getTargetList();
                log.trace("target.size(): {}, k: {}, b: {}", targets.size(), k, b);
                Target target = targets.get(k);
                log.trace("valuetarget: {}", target.getValue());
                NDArray valueOutput = nd.zeros(new Shape(1));
                valueOutput.setScalar(new NDIndex(0), target.getValue());
                valueList.add(valueOutput);
                log.trace("policytarget: {}", Arrays.toString(target.getPolicy()));
                NDArray c = nd.create(target.getPolicy());
                policyList.add(c);

                b++;
            }
            NDArray policyOutput2 = NDArrays.stack(new NDList(policyList));
            NDArray valueOutput2 = NDArrays.stack(new NDList(valueList));
            outputs.add(symmetryEnhancerPolicy(policyOutput2));
            outputs.add(symmetryEnhancerValue(valueOutput2));
        }
        return outputs;
    }

}

