package ai.enpasos.muzero.go.selfcritical;


import ai.djl.basicdataset.cv.classification.Mnist;
import ai.djl.engine.Engine;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.training.dataset.ArrayDataset;
import ai.djl.util.Progress;
import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

@Slf4j
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

        float[] labels = new float[length * (maxFullMoves+1)]; // + 1, because v can be wrong for all moves
        float[] data = new float[length * 2 * (maxFullMoves+1)];

        for (int i = 0; i < length; i++) {

            SelfCriticalGame game = this.inputData.getData().get(i);
            int fullMove = 0;
            int move = 0;
            for (Map.Entry<SelfCriticalPosition, Float> entry : game.normalizedEntropyValues.entrySet()) {
                SelfCriticalPosition pos = entry.getKey();
                float entropy = entry.getValue();

                if (pos.getPlayer() == OneOfTwoPlayer.PLAYER_A) {
                    data[i * 2 * (maxFullMoves + 1) +                      fullMove ] = entropy;
                } else {
                    data[i * 2 * (maxFullMoves + 1) + (maxFullMoves + 1) + fullMove] = entropy;
                    fullMove++;
                }

                move++;
            }
            labels[i * (maxFullMoves+1) + game.firstReliableFullMove] = 1f;

        }


        int count = 0;
        for(int i = 0; i < labels.length; i++) {
            if (labels[i] != 0) count++;
        }
        log.info("labels expected 1 sum: " + length + ", measured: " + count);



        manager = Engine.getInstance().newBaseManager();

        this.labels = new NDArray[]{manager.create(labels, new Shape(length, maxFullMoves +1))};
        this.data = new NDArray[]{manager.create(data, new Shape(length, 1, 2, maxFullMoves +1))};

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
