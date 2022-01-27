package ai.enpasos.muzero.go.selfcritical;


import ai.djl.basicdataset.cv.classification.Mnist;
import ai.djl.engine.Engine;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.dataset.ArrayDataset;
import ai.djl.util.Progress;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;

import java.io.IOException;
import java.util.Map;


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
        int maxFullMoves = this.inputData.maxFullMoves;

        int length = this.inputData.getData().size();

        float[] labels = new float[length * maxFullMoves];
        float[] data = new float[length * 2 * maxFullMoves];

        for (int i = 0; i < length; i++) {

            SelfCriticalGame game = this.inputData.getData().get(i);
            int fullMove = 0;

            for (Map.Entry<SelfCriticalPosition, Float> entry : game.normalizedEntropyValues.entrySet()) {
                SelfCriticalPosition pos = entry.getKey();
                float entropy = entry.getValue();

                data[i * 2 * maxFullMoves + fullMove + 0] = pos.getPlayer() == OneOfTwoPlayer.PLAYER_A ? 0f : 1f;
                data[i * 2 * maxFullMoves + fullMove + 1] = entropy;

                labels[i * maxFullMoves + fullMove] = (game.firstReliableFullMove == fullMove) ? 1f : 0f;

                fullMove++;
            }

        }

        manager = Engine.getInstance().newBaseManager();

        this.labels = new NDArray[]{manager.create(labels, new Shape(length, maxFullMoves))};
        this.data = new NDArray[]{manager.create(data, new Shape(length, 1, 2, maxFullMoves))};

        prepared = true;
    }


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
