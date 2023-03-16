package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import com.google.protobuf.ByteString;

import java.util.BitSet;

public interface Observation {
    static float[] bitSetToFloatArray(int size, BitSet input) {
        float[] result = new float[size];
        for (int i = 0; i < size; i++) {
            result[i] = input.get(i) ? 1f : 0f;
        }
        return result;
    }



    int addTo(BitSet rawResult, int index);

    Observation clone();

    ByteString toByteString();
}
