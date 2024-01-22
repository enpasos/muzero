package ai.enpasos.mnist.blocks.ext;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.nn.Block;
import ai.djl.nn.Blocks;

import static ai.enpasos.mnist.blocks.ext.LambdaBlockExt.Type.*;

/**
 * Utility class that provides some useful blocks.
 */
@SuppressWarnings("all")
public final class BlocksExt {

    private BlocksExt() {
    }

    /**
     * Inflates the {@link ai.djl.ndarray.NDArray} provided as input to a 2-D {@link
     * ai.djl.ndarray.NDArray} of shape (batch, size).
     *
     * @param array a array to be flattened
     * @return a {@link NDList} that contains the inflated {@link ai.djl.ndarray.NDArray}
     */
    public static NDArray batchFlatten(NDArray array) {
        long batch = array.size(0);
        return array.reshape(batch, -1);
    }


    /**
     * Creates a {@link Block} whose forward function applies the {@link #batchFlatten(NDArray)
     * batchFlatten} method.
     *
     * @return a {@link Block} whose forward function applies the {@link #batchFlatten(NDArray)
     * batchFlatten} method
     */
    public static Block batchFlattenBlock() {
        return LambdaBlockExt.singleton(BATCH_FLATTEN, Blocks::batchFlatten);
    }

    /**
     * Creates a {@link Block} whose forward function applies the {@link #batchFlatten(NDArray)
     * batchFlatten} method. The size of input to the block returned must be batch_size * size.
     *
     * @param size the expected size of each input
     * @return a {@link Block} whose forward function applies the {@link #batchFlatten(NDArray)
     * batchFlatten} method
     */
    public static Block batchFlattenBlock(long size) {
        return LambdaBlockExt.singleton(BATCH_FLATTEN, array -> Blocks.batchFlatten(array, size));
    }

    /**
     * Creates a {@link LambdaBlockExt} that performs the identity function.
     *
     * @return an identity {@link Block}
     */
    public static Block identityBlock() {
        return new LambdaBlockExt(IDENTITY, x -> x);
    }


    public static Block identityOnLastInput() {
        return new LambdaBlockExt(IDENTITY_ON_LAST_INPUT, x -> new NDList(x.get(x.size()-1)));
    }

    public static Block identityOnFirstInput() {
        return new LambdaBlockExt(IDENTITY_ON_FIRST_INPUT, x -> new NDList(x.get(0)));
    }
}
