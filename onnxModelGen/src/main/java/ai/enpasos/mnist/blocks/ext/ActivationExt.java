package ai.enpasos.mnist.blocks.ext;


import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.nn.Block;
import ai.djl.nn.core.Prelu;
import ai.enpasos.mnist.blocks.ext.LambdaBlockExt;

import static ai.enpasos.mnist.blocks.ext.LambdaBlockExt.Type.*;

/**
 * Utility class that provides activation functions and blocks.
 *
 * <p>Many networks make use of the {@link ai.djl.nn.core.Linear} block and other similar linear
 * transformations. However, any number of linear transformations that are composed will only result
 * in a different linear transformation (\($f(x) = W_2(W_1x) = (W_2W_1)x = W_{combined}x\)). In
 * order to represent non-linear data, non-linear functions called activation functions are
 * interspersed between the linear transformations. This allows the network to represent non-linear
 * functions of increasing complexity.
 *
 * <p>See <a href="https://en.wikipedia.org/wiki/Activation_function">wikipedia</a> for more
 * details.
 */
public final class ActivationExt {

    private ActivationExt() {
    }



    public static Block reluBlock() {
        return new LambdaBlockExt(RELU, ai.djl.nn.Activation::relu);
    }

    public static Block sigmoidBlock() {
        return new LambdaBlockExt(SIGMOID, ai.djl.nn.Activation::sigmoid);
    }

    public static Block tanhBlock() {
        return new LambdaBlockExt(TANH, ai.djl.nn.Activation::tanh);
    }


    public static Block softPlusBlock() {
        return new LambdaBlockExt(NOT_IMPLEMENTED_YET, ai.djl.nn.Activation::softPlus);
    }


    public static Block softSignBlock() {
        return new LambdaBlockExt(NOT_IMPLEMENTED_YET, ai.djl.nn.Activation::softSign);
    }


    public static Block leakyReluBlock(float alpha) {
        return new LambdaBlockExt(NOT_IMPLEMENTED_YET, arrays -> ai.djl.nn.Activation.leakyRelu(arrays, alpha));
    }


    public static Block eluBlock(float alpha) {
        return new LambdaBlockExt(NOT_IMPLEMENTED_YET, arrays -> ai.djl.nn.Activation.elu(arrays, alpha));
    }


    public static Block seluBlock() {
        return new LambdaBlockExt(NOT_IMPLEMENTED_YET, ai.djl.nn.Activation::selu);
    }


    public static Block geluBlock() {
        return new LambdaBlockExt(NOT_IMPLEMENTED_YET, ai.djl.nn.Activation::gelu);
    }


    public static Block swishBlock(float beta) {
        return new LambdaBlockExt(NOT_IMPLEMENTED_YET, arrays -> ai.djl.nn.Activation.swish(arrays, beta));
    }


    public static Block mishBlock() {
        return new LambdaBlockExt(NOT_IMPLEMENTED_YET, ai.djl.nn.Activation::mish);
    }


}
