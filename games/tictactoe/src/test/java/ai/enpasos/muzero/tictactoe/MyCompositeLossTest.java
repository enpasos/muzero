package ai.enpasos.muzero.tictactoe;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.training.DefaultTrainingConfig;
import ai.enpasos.muzero.platform.agent.d_model.djl.MyCompositeLoss;
import ai.enpasos.muzero.platform.agent.d_model.djl.SomeSerialization;
import ai.enpasos.muzero.platform.agent.d_model.djl.Statistics;
import ai.enpasos.muzero.platform.agent.d_model.djl.TrainingConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
@TestExecutionListeners( { DependencyInjectionTestExecutionListener.class })
@Slf4j
class MyCompositeLossTest {

    @Autowired
    TrainingConfigFactory trainingConfigFactory;

    @Test
    void evaluateWhatToTrain() {

//        SomeSerialization.saveNDList(labels, "labels.dat");
//        SomeSerialization.saveNDList(predictions, "predictions.dat");
//        SomeSerialization.saveBooleanArray(bOK, "bOK.dat");
//        SomeSerialization.saveIntArray(from, "from.dat");

        String baseDir = "src/test/resources/scenario1/";

        NDManager ndManager =  NDManager.newBaseManager();
        NDList labels = SomeSerialization.loadNDList( baseDir + "labels.dat", ndManager);
        NDList predictions  = SomeSerialization.loadNDList( baseDir + "predictions.dat", ndManager);
        boolean[][][] bOK = SomeSerialization.loadBooleanArray(baseDir + "bOK.dat");
        int[] from = SomeSerialization.loadIntArray(baseDir + "from.dat");

int unrollSteps = 3;

        DefaultTrainingConfig djlConfig = trainingConfigFactory.setupTrainingConfig(1, false, true, false, true,  unrollSteps );

        MyCompositeLoss myCompositeLoss = (MyCompositeLoss) djlConfig.getLossFunction();

        NDArray result = myCompositeLoss.evaluateWhatToTrain(labels, predictions, bOK, from, new Statistics());

    }
}
