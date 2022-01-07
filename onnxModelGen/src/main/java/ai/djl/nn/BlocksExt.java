package ai.djl.nn;

import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;

import static ai.djl.nn.LambdaBlockExt.Type.BATCH_FLATTEN;
import static ai.djl.nn.LambdaBlockExt.Type.IDENTITY;

/**
 * Utility class that provides some useful blocks.
 */
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
     * Inflates the {@link ai.djl.ndarray.NDArray} provided as input to a 2-D {@link
     * ai.djl.ndarray.NDArray} of shape (batch, size).
     *
     * @param array a array to be flattened
     * @param size  the input size
     * @return a {@link NDList} that contains the inflated {@link ai.djl.ndarray.NDArray}
     * @throws IndexOutOfBoundsException if the input {@link NDList} has more than one {@link
     *                                   ai.djl.ndarray.NDArray}
     */
    public static NDArray batchFlatten(NDArray array, long size) {
        return array.reshape(-1, size);
    }

    /**
     * Creates a {@link Block} whose forward function applies the {@link #batchFlatten(NDArray)
     * batchFlatten} method.
     *
     * @return a {@link Block} whose forward function applies the {@link #batchFlatten(NDArray)
     * batchFlatten} method
     */
    public static Block batchFlattenBlock() {
        return LambdaBlockExt.singleton(BATCH_FLATTEN, BlocksExt::batchFlatten);
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
        return LambdaBlockExt.singleton(BATCH_FLATTEN, array -> batchFlatten(array, size));
    }

    /**
     * Creates a {@link LambdaBlockExt} that performs the identity function.
     *
     * @return an identity {@link Block}
     */
    public static Block identityBlock() {
        return new LambdaBlockExt(IDENTITY, x -> x);
    }
}
