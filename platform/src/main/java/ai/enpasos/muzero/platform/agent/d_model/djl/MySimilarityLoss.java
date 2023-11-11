package ai.enpasos.muzero.platform.agent.d_model.djl;


import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.training.loss.Loss;


public class MySimilarityLoss extends Loss {

    public static final float NULL_VALUE = 1234567f;

    private final float weight;


    public MySimilarityLoss() {
        this("SimilarityLoss");
    }


    public MySimilarityLoss(String name) {
        this(name, 2.f);
    }


    public MySimilarityLoss(String name, float weight) {
        super(name);
        this.weight = weight;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public NDArray evaluate(NDList label, NDList prediction) {
        NDArray pred = prediction.singletonOrThrow();
        NDArray lab = label.singletonOrThrow();

        int[] axis = new int[]{1};
        double epsilon = 1e-8;
        NDArray labNorm = lab.norm(axis, true);
        NDArray predNorm = pred.norm(axis, true);

        labNorm = labNorm.maximum(epsilon);
        predNorm = predNorm.maximum(epsilon);

        NDArray normProd = labNorm.mul(predNorm);
        NDArray loss = lab.mul(pred).sum(axis, true).div(normProd).sub(1).mul(-weight);

       //     NDArray lossMean = loss.mean();
      //  System.out.println("similarityLossMean: " + lossMean.toFloatArray()[0]);
        return loss;
    }
}
