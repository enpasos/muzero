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

package ai.enpasos.muzero.platform.agent.fast.model;


import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.platform.MuZeroConfig;
import ai.enpasos.muzero.platform.agent.gamebuffer.Target;
import ai.enpasos.muzero.platform.agent.slow.play.Action;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class InputOutputConstruction {


    public static @NotNull List<NDArray> constructInput(@NotNull MuZeroConfig config, @NotNull NDManager nd, int numUnrollSteps, Observation observation, List<Integer> actionsList, boolean withSymmetryEnrichment) {

        List<NDArray> inputs = new ArrayList<>();


        Sample sample = Sample.builder()
                .observation(observation)
                .actionsList(actionsList)
                .build();
        List<Sample> batch = new ArrayList<>();
        batch.add(sample);

        addHiddenStateInput(nd, batch, inputs);
        addActionInput(config, numUnrollSteps, batch, nd, inputs, withSymmetryEnrichment);

        return inputs;

    }

    public static @NotNull List<NDArray> constructInput(@NotNull MuZeroConfig config, @NotNull NDManager ndManager, int numUnrollSteps, @NotNull List<Sample> batch, boolean withSymmetryEnrichment) {

        List<NDArray> inputs = new ArrayList<>();

        addHiddenStateInput(ndManager, batch, inputs);
        addActionInput(config, numUnrollSteps, batch, ndManager, inputs, withSymmetryEnrichment);

        return inputs;

    }

    public static NDArray symmetryEnhancerReturnNDArray(@NotNull NDArray a) {
        List<NDArray> list = symmetryEnhancer(a);
        return NDArrays.concat(new NDList( list));
    }

    @NotNull
    private static List<NDArray> symmetryEnhancer(@NotNull NDArray a) {
        NDArray a2 = a.rotate90(1, new int[]{2, 3});
        NDArray a3 = a.rotate90(2, new int[]{2, 3});
        NDArray a4 = a.rotate90(3, new int[]{2, 3});
        NDArray a5 = a.transpose(0, 1, 3, 2);

        NDArray a6 = a5.rotate90(1, new int[]{2, 3});
        NDArray a7 = a5.rotate90(2, new int[]{2, 3});
        NDArray a8 = a5.rotate90(3, new int[]{2, 3});
        List<NDArray> list = List.of(a, a2, a3, a4, a5, a6, a7, a8);
        return list;
    }

    public static NDArray symmetryEnhancerValue(@NotNull NDArray a) {
        NDArray a2 = a.duplicate();
        NDArray a3 = a.duplicate();
        NDArray a4 = a.duplicate();
        NDArray a5 = a.duplicate();
        NDArray a6 = a.duplicate();
        NDArray a7 = a.duplicate();
        NDArray a8 = a.duplicate();
        return NDArrays.concat(new NDList(a, a2, a3, a4, a5, a6, a7, a8));
    }

    public static NDArray symmetryEnhancerPolicy(MuZeroConfig config, @NotNull NDArray a) {
        // TODO generalize
        Shape oldShape = a.getShape();

        int boardsize = config.getBoardHeight() * config.getBoardWidth();
        NDList splitted = a.split(new long[] {boardsize},1);
        NDArray a_ = splitted.get(0);
        Shape oldShape_ = a_.getShape();

        NDArray policyOutput3 = a_.reshape(new Shape(oldShape_.get(0), 1, config.getBoardHeight(), config.getBoardWidth()));


        List<NDArray> ndArrayList = symmetryEnhancer(policyOutput3);
        List<NDArray> ndArrayList2 = ndArrayList.stream().map(
                transformedA -> {
                    NDArray a0 = transformedA.reshape(new Shape(transformedA.getShape().get(0), oldShape_.get(1)));
                    if (splitted.size() > 0) {
                        NDArray a1 = splitted.get(1);
                        a0 = a0.concat(a1, 1);
                    }
                    return a0;
                }
        ).collect(Collectors.toList());


        NDArray policyOutput4 = NDArrays.concat(new NDList( ndArrayList2));


        return policyOutput4.reshape(new Shape(policyOutput4.getShape().get(0), oldShape.get(1)));

    }


    private static void addHiddenStateInput(@NotNull NDManager ndManager, @NotNull List<Sample> batch, @NotNull List<NDArray> inputs) {
        List<NDArray> o = batch.stream()
                .map(sample -> sample.getObservation().getNDArray(ndManager))
                .collect(Collectors.toList());

        inputs.add(symmetryEnhancerReturnNDArray(NDArrays.stack(new NDList(o))));
    }

    public static void addActionInput(@NotNull MuZeroConfig config, int numUnrollSteps, @NotNull List<Sample> batch, @NotNull NDManager nd, @NotNull List<NDArray> inputs, boolean withSymmetryEnrichment) {

        // System.out.println("*** numUnrollSteps = " + numUnrollSteps + " ***");
        for (int k = 0; k < numUnrollSteps; k++) {

            List<NDArray> list = new ArrayList<>();
            int b = 0;
            for (Sample s : batch) {
                //  System.out.println("k: " + k + ", b: " + b++);
                NDArray aArray;
                if (s.getActionsList().size() > k) {
                    Action action = new Action(config, s.getActionsList().get(k));
                    aArray = action.encode(nd);
                } else {
                    aArray = Action.encodeEmptyNDArray(config, nd);
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


    public static @NotNull List<NDArray> constructOutput(MuZeroConfig config, @NotNull NDManager nd, int numUnrollSteps, @NotNull List<Sample> batch) {

        //  log.debug("constructOutput Start");
        List<NDArray> outputs = new ArrayList<>();


        Sample sample = batch.get(0);
        for (int k = 0; k <= numUnrollSteps; k++) {

            List<NDArray> policyList = new ArrayList<>();

            List<NDArray> valueList = new ArrayList<>();
            int b = 0;
            for (Sample s : batch) {

                List<Target> targets = s.getTargetList();
                log.trace("target.size(): {}, k: {}, b: {}", targets.size(), k, b);
                Target target = targets.get(k);


                log.trace("valuetarget: {}", target.value);
                NDArray valueOutput = nd.zeros(new Shape(1));
                valueOutput.setScalar(new NDIndex(0), target.value);
                valueList.add(valueOutput);
                log.trace("policytarget: {}", Arrays.toString(target.policy));
                NDArray c = nd.create(target.policy);
                policyList.add(c);

                b++;
            }
            NDArray policyOutput2 = NDArrays.stack(new NDList(policyList));

            NDArray valueOutput2 = NDArrays.stack(new NDList(valueList));


            outputs.add(symmetryEnhancerPolicy(config, policyOutput2));
            outputs.add(symmetryEnhancerValue(valueOutput2));

        }


        return outputs;

    }


}

