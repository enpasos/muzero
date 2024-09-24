package ai.enpasos.mnist.blocks.ext;

import ai.djl.nn.norm.Dropout;
import ai.enpasos.mnist.blocks.OnnxBlock;
import ai.enpasos.mnist.blocks.OnnxCounter;
import ai.enpasos.mnist.blocks.OnnxIO;
import ai.enpasos.mnist.blocks.OnnxTensor;

import java.util.List;

public class DropoutExt extends Dropout implements OnnxIO {


    public DropoutExt(Builder builder) {
        super(builder);
    }

    @Override
    public OnnxBlock getOnnxBlock(OnnxCounter counter, List<OnnxTensor> input) {
        return null;
    }


    /**
     * Creates a builder to build a {@link Dropout}.
     *
     * @return a new builder
     */
    public static DropoutExt.Builder builder() {
        return new DropoutExt.Builder();
    }

    /** The Builder to construct a {@link Dropout} type of {@link ai.djl.nn.Block}. */
    public static class Builder extends Dropout.Builder {

        protected Builder() {
            super();
        }


        /**
         * Sets the probability or the fraction of the input that gets dropped out during training
         * time. Defaults to 0.5.
         *
         * @param rate fraction of the input that gets dropped out during training
         * @return this Builder
         */
        public DropoutExt.Builder optRate(float rate) {
            super.optRate(rate);
            return this;
        }

        /**
         * Builds a {@link DropoutExt} block.
         *
         * @return the {@link DropoutExt} block
         */
        public DropoutExt build() {
            return new DropoutExt(this);
        }
    }

}
