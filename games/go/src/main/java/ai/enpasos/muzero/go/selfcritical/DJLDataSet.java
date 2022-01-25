package ai.enpasos.muzero.go.selfcritical;


import ai.djl.basicdataset.cv.classification.Mnist;
import ai.djl.engine.Engine;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.dataset.ArrayDataset;
import ai.djl.util.Progress;

import java.io.IOException;


public  class DJLDataSet extends ArrayDataset {

    private NDManager manager;
    private Usage usage;
    private SelfCriticalDataSet inputData;
    private boolean prepared;

    private DJLDataSet( Builder builder) {
             super(builder);
             this.manager = builder.manager;
             this.manager.setName("mnist");
             this.usage = builder.usage;
             this.inputData = builder.inputData;
         }


    public static  Builder builder() {
        return new  Builder();
    }


    @Override
    public void prepare(Progress progress) throws IOException {
        if (prepared) {
            return;
        }


        int length = this.inputData.getFeatures().size();

        float[] labels = new float[length * 2];
        float[] data = new float[length * 3];
     //   float[] data = new float[length];
        for (int i = 0; i < length; i++) {
            SelfCriticalLabeledFeature feature = this.inputData.getFeatures().get(i);
            labels[2 * i + 0] = feature.correctAndNoMindChange ? 1f : 0f;
            labels[2 * i + 1] = feature.correctAndNoMindChange ? 0f : 1f;

            //data[i ] = (float) (feature.entropy);

            data[3 * i + 0] = (float) (feature.entropy);
            data[3 * i + 1] = (float) feature.normalizedNumberOfMovesPlayedSofar;
            data[3 * i + 2] = (float) (feature.toPlayNormalized);

        }

        manager = Engine.getInstance().newBaseManager();

        this.labels = new NDArray[]{manager.create(labels, new Shape(length, 2))};
        this.data = new NDArray[]{manager.create(data, new Shape(length, 3))};

        prepared = true;
    }
//
//    public NDManager getManager() {
//        return manager;
//    }
//
//    public void setManager(NDManager manager) {
//        this.manager = manager;
//    }
//
//    public Usage getUsage() {
//        return usage;
//    }
//
//    public void setUsage(Usage usage) {
//        this.usage = usage;
//    }
//
//    public SelfCriticalDataSet getInputData() {
//        return inputData;
//    }
//
//    public void setInputData(SelfCriticalDataSet inputData) {
//        this.inputData = inputData;
//    }
//
//    public boolean isPrepared() {
//        return prepared;
//    }
//
//    public void setPrepared(boolean prepared) {
//        this.prepared = prepared;
//    }



    public static final class Builder extends BaseBuilder<Builder> {

        private NDManager manager;

        private Usage usage;
        private SelfCriticalDataSet inputData;


        public Builder() {
            usage = Usage.TRAIN;
            manager = Engine.getInstance().newBaseManager();
           // pipeline = new Pipeline(new ToTensor());
        }

        /**
         * Creates a builder to build a {@link Mnist}.
         *
         * @return a new builder
         */
        public static  Builder builder() {
            return new Builder();
        }

        /** {@inheritDoc} */
        @Override
        protected  Builder self() {
            return this;
        }

        public Builder optUsage(Usage usage) {
            this.usage = usage;
            return this;
        }

        public Builder inputData(SelfCriticalDataSet inputData) {
            this.inputData = inputData;
            return this;
        }

        public DJLDataSet build() {
            return new DJLDataSet(this);
        }

    }

}
