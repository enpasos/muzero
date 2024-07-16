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

package ai.enpasos.muzero.platform.agent.d_model.djl;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.dataset.Batch;
import ai.enpasos.muzero.platform.agent.d_model.InputOutputConstruction;
import ai.enpasos.muzero.platform.agent.d_model.ObservationModelInput;
import ai.enpasos.muzero.platform.agent.d_model.Sample;
import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.GameBuffer;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import ai.enpasos.muzero.platform.config.TrainingDatasetType;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ai.enpasos.muzero.platform.agent.e_experience.GameBuffer.convertEpisodeDOsToGames;


@Slf4j
@Component
public class BatchFactory {

    @Autowired
    MuZeroConfig config;

    @Autowired
    GameBuffer gameBuffer;

    @Autowired
    InputOutputConstruction inputOutputConstruction;



//    public Batch getSequentialBatchFromAllExperience(@NotNull NDManager ndManager, boolean withSymmetryEnrichment, int numUnrollSteps, SequentialCursor cursor) {
//
//        List<Sample> sampleList = gameBuffer.sequentialSampleList(numUnrollSteps, cursor);
//        Batch batch = getBatch(ndManager, withSymmetryEnrichment, numUnrollSteps, sampleList);
//
////        sampleList.forEach(s -> s.clear());
////        sampleList.clear();
//        return batch;
//    }

    @NotNull
    private Batch getBatch(@NotNull NDManager ndManager, boolean withSymmetryEnrichment, int numUnrollSteps, List<Sample> sampleList, TrainingDatasetType trainingDatasetType) {
        NDManager nd = ndManager.newSubManager();


        List<NDArray> inputs = inputOutputConstruction.constructInput(nd, numUnrollSteps, sampleList, withSymmetryEnrichment, config.isWithConsistencyLoss(), trainingDatasetType);
        List<NDArray> outputs = inputOutputConstruction.constructOutput(nd, numUnrollSteps, sampleList, trainingDatasetType, withSymmetryEnrichment);

        return new Batch(
                nd,
                new NDList(inputs),
                new NDList(outputs),
                inputs.size() > 0 ? (int) inputs.get(0).getShape().get(0) : 0,
                null,
                null,
                0,
                0);
    }



    private RulesBuffer rulesBuffer;
    public Batch getRulesBatchFromBuffer(List<TimeStepDO> batchTimeSteps, NDManager ndManager, boolean withSymmetryEnrichment, int u ) {

        List<Sample> sampleList =  batchTimeSteps.stream().map( ts -> rulesSampleFromTimeStep(ts, u)).collect(Collectors.toList());


      Batch batch =  getBatch(ndManager, withSymmetryEnrichment, u, sampleList, TrainingDatasetType.RULES_BUFFER);
   //     log.info("batch.base.size: {}, batchTimeSteps.size(): {}, sampleList.size() {}", batch.getData().get(0).getShape().get(0)/8, batchTimeSteps.size(), sampleList.size() );
      return batch;

    }


    public  Sample rulesSampleFromTimeStep(TimeStepDO ts, int s) {
      //  Sample sample = new Sample();
        Game game = config.newGame(false,false);
        game.setEpisodeDO(ts.getEpisode());
       return gameBuffer.sampleFromGame(s, game, ts.getT()  );

    }

    public Batch getBatchFromBuffer(@NotNull NDManager ndManager, boolean withSymmetryEnrichment, int numUnrollSteps, int batchSize, TrainingDatasetType trainingDatasetType) {
        List<Sample> sampleList = null;
        switch(trainingDatasetType) {
            case PLANNING_BUFFER:
                sampleList = gameBuffer.sampleBatchFromPlanningBuffer(numUnrollSteps);
                break;
//            case RULES_BUFFER:
//                sampleList = gameBuffer.sampleBatchFromRulesBuffer(numUnrollSteps);
//                break;
            case REANALYSE_BUFFER:
                sampleList = gameBuffer.sampleBatchFromReanalyseBuffer(numUnrollSteps);
                break;
        }


        return getBatch(ndManager, withSymmetryEnrichment, numUnrollSteps, sampleList, trainingDatasetType);
    }

//    public Batch getBatchFromRulesBuffer(@NotNull NDManager ndManager, boolean withSymmetryEnrichment, int numUnrollSteps, int batchSize) {
//        List<Sample> sampleList = gameBuffer.sampleBatchFromRulesBuffer(config.getNumUnrollSteps());
//        return getBatch(ndManager, withSymmetryEnrichment, numUnrollSteps, sampleList);
//    }
//    public Batch getBatchFromReanalyseBuffer(@NotNull NDManager ndManager, boolean withSymmetryEnrichment, int numUnrollSteps, int batchSize) {
//        List<Sample> sampleList = gameBuffer.sampleBatchFromReanalyseBuffer(config.getNumUnrollSteps());
//        return getBatch(ndManager, withSymmetryEnrichment, numUnrollSteps, sampleList);
//    }

    public Shape @NotNull [] getInputShapes() {
        return getInputShapes(config.getBatchSize() * config.getSymmetryType().getSymmetryEnhancementFactor());
    }

    public Shape @NotNull [] getInputShapes(int batchSize) {
        Shape[] shapes = new Shape[config.getNumUnrollSteps() + 1];
        // for observation input
        shapes[0] = new Shape(batchSize, config.getNumObservationLayers(), config.getBoardHeight(), config.getBoardWidth());
        for (int k = 1; k <= config.getNumUnrollSteps(); k++) {
            shapes[k] = new Shape(batchSize, config.getNumActionLayers(), config.getBoardHeight(), config.getBoardWidth());
        }
        return shapes;
    }

    public Shape @NotNull [] getInputShapesForRules(int s ) {
        return getInputShapesForRules(config.getBatchSize(), s);
    }

    public Shape @NotNull [] getInputShapesForRules(int batchSize, int s ) {
        Shape[] shapes = new Shape[s + 1];
        shapes[0] = new Shape(batchSize, config.getNumObservationLayers(), config.getBoardHeight(), config.getBoardWidth());
        for (int k = 1; k < shapes.length; k++) {
            shapes[k] = new Shape(batchSize, config.getNumActionLayers(), config.getBoardHeight(), config.getBoardWidth());
        }
        return shapes;
    }


    boolean hasMoreRulesBatches = true;  // for testing
    public boolean hasMoreRulesBatches(int s) {
        if (hasMoreRulesBatches) {
            hasMoreRulesBatches = false;
            return true;
        }
        return false;
    }
}

