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

package ai.enpasos.muzero.network;


import ai.djl.Model;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDArrays;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.index.NDIndex;
import ai.djl.ndarray.types.Shape;
import ai.enpasos.muzero.MuZeroConfig;
import ai.enpasos.muzero.gamebuffer.Target;
import ai.enpasos.muzero.play.Action;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class InputOutputConstruction {
    public static List<NDArray> constructInputForDynamics(MuZeroConfig config, NDArray hiddenState, Action action, Model model, boolean withEmptyState, boolean withSymmetryEnrichment) {
        NDManager nd = NDManager.newBaseManager(config.getInferenceDevice());
        List<NDArray> inputs = new ArrayList<>();

        inputs.add(hiddenState);

        Sample sample = Sample.builder()
                .actionsList(List.of(action.getIndex()))
                .build();
        List<Sample> batch = new ArrayList<>();
        batch.add(sample);

        addActionInput(config, 1, batch, nd, inputs, withSymmetryEnrichment);

        return inputs;

    }


    public static List<NDArray> constructInputForDynamics(MuZeroConfig config, NDArray hiddenState, List<Action> actionList, Model model, boolean withEmptyState, boolean withSymmetryEnrichment) {


        NDManager nd = NDManager.newBaseManager(config.getInferenceDevice());
        List<NDArray> inputs = new ArrayList<>();

        inputs.add(hiddenState);

        Sample sample = Sample.builder()
                .actionsList(List.of(actionList.get(0).getIndex()))
                .build();
        List<Sample> batch = new ArrayList<>();
        batch.add(sample);

        addActionInput(config, 1, batch, nd, inputs, withSymmetryEnrichment);

        return inputs;

    }

    public static List<NDArray> constructInput(MuZeroConfig config, NDManager nd, int numUnrollSteps, Observation observation, List<Integer> actionsList, boolean withEmptyState, boolean withSymmetryEnrichment) {

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

    public static List<NDArray> constructInput(MuZeroConfig config, NDManager ndManager, int numUnrollSteps, List<Sample> batch, boolean withSymmetryEnrichment) {

        List<NDArray> inputs = new ArrayList<>();

        addHiddenStateInput(ndManager, batch, inputs);
        addActionInput(config, numUnrollSteps, batch, ndManager, inputs, withSymmetryEnrichment);

        return inputs;

    }

    public static NDArray symmetryEnhancer(NDArray a) {
        NDArray a1 = a;
        NDArray a2 = a.rotate90(1, new int[]{2, 3});
        NDArray a3 = a.rotate90(2, new int[]{2, 3});
        NDArray a4 = a.rotate90(3, new int[]{2, 3});
        NDArray a5 = a.transpose(0, 1, 3, 2);

        NDArray a6 = a5.rotate90(1, new int[]{2, 3});
        NDArray a7 = a5.rotate90(2, new int[]{2, 3});
        NDArray a8 = a5.rotate90(3, new int[]{2, 3});

        return NDArrays.concat(new NDList(a1, a2, a3, a4, a5, a6, a7, a8));
    }

    public static NDArray symmetryEnhancerValue(NDArray a) {
        NDArray a1 = a;
        NDArray a2 = a.duplicate();
        NDArray a3 = a.duplicate();
        NDArray a4 = a.duplicate();
        NDArray a5 = a.duplicate();
        NDArray a6 = a.duplicate();
        NDArray a7 = a.duplicate();
        NDArray a8 = a.duplicate();
        return NDArrays.concat(new NDList(a1, a2, a3, a4, a5, a6, a7, a8));
    }

    public static NDArray symmetryEnhancerPolicy(NDArray a) {
        // TODO generalize
        Shape oldShape = a.getShape();
        NDArray policyOutput3 = a.reshape(new Shape(oldShape.get(0), 1, 3, 3));
        NDArray policyOutput4 = symmetryEnhancer(policyOutput3);
        NDArray policyOutput5 = policyOutput4.reshape(new Shape(policyOutput4.getShape().get(0), oldShape.get(1)));

        return policyOutput5;
    }


    private static void addHiddenStateInput(NDManager ndManager, List<Sample> batch, List<NDArray> inputs) {
        List<NDArray> o = batch.stream()
                .map(sample -> sample.getObservation().getNDArray(ndManager))
                .collect(Collectors.toList());

        inputs.add(symmetryEnhancer(NDArrays.stack(new NDList(o))));
    }

    public static void addActionInput(MuZeroConfig config, int numUnrollSteps, List<Sample> batch, NDManager nd, List<NDArray> inputs, boolean withSymmetryEnrichment) {

        // System.out.println("*** numUnrollSteps = " + numUnrollSteps + " ***");
        for (int k = 0; k < numUnrollSteps; k++) {

            List<NDArray> list = new ArrayList<>();
            int b = 0;
            for (Sample s : batch) {
                //  System.out.println("k: " + k + ", b: " + b++);
                NDArray aArray = null;
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
                inputs.add(symmetryEnhancer(inputMiniBatch2));
            } else {
                inputs.add(inputMiniBatch2);
            }
        }
    }


    public static List<NDArray> constructOutput(NDManager nd, int numUnrollSteps, List<Sample> batch) {

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


            outputs.add(symmetryEnhancerPolicy(policyOutput2));
            outputs.add(symmetryEnhancerValue(valueOutput2));

        }


        return outputs;

    }


}

