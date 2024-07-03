package ai.enpasos.muzero.platform.agent.d_model.djl;

import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MyCompositeLossTest {

    @Test
    void evaluateWhatToTrain() {

//        SomeSerialization.saveNDList(labels, "labels.dat");
//        SomeSerialization.saveNDList(predictions, "predictions.dat");
//        SomeSerialization.saveBooleanArray(bOK, "bOK.dat");
//        SomeSerialization.saveIntArray(from, "from.dat");

        String baseDir = "E:\\public\\muzero\\";

        NDManager ndManager =  NDManager.newBaseManager();
        NDList labels = SomeSerialization.loadNDList( baseDir + "labels.dat", ndManager);
        NDList predictions  = SomeSerialization.loadNDList( baseDir + "predictions.dat", ndManager);
        boolean[][][] bOK = SomeSerialization.loadBooleanArray(baseDir + "bOK.dat");
        int[] from = SomeSerialization.loadIntArray(baseDir + "from.dat");


        int i = 342;

    }
}
