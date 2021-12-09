package ai.enpasos.muzero.platform.config;

import ai.djl.ndarray.NDArray;

import java.util.List;
import java.util.function.Function;

public enum SymmetryType {
    NONE, SQUARE;

    public int getSymmetryEnhancementFactor() {
        if (this == SQUARE) {
            return 8;
        }
        return 1;
    }

    public Function<NDArray, List<NDArray>> getSymmetryFunction() {
        if (this == SQUARE) {
            return a -> {
                NDArray a2 = a.rotate90(1, new int[]{2, 3});
                NDArray a3 = a.rotate90(2, new int[]{2, 3});
                NDArray a4 = a.rotate90(3, new int[]{2, 3});
                NDArray a5 = a.transpose(0, 1, 3, 2);

                NDArray a6 = a5.rotate90(1, new int[]{2, 3});
                NDArray a7 = a5.rotate90(2, new int[]{2, 3});
                NDArray a8 = a5.rotate90(3, new int[]{2, 3});
                return List.of(a, a2, a3, a4, a5, a6, a7, a8);
            };
        }
        return a -> List.of(a);
    }
}
