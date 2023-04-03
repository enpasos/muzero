package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.episode.Player;
import com.google.protobuf.ByteString;

import java.util.BitSet;

public interface Observation {

    int getPartSize();
    static float[] bitSetToFloatArray(int size, BitSet input) {
        float[] result = new float[size];
        for (int i = 0; i < size; i++) {
            result[i] = input.get(i) ? 1f : 0f;
        }
        return result;
    }



    int addTo(Player player, BitSet rawResult, int index);

    Observation clone();

    ByteString toByteStringA();
    ByteString toByteStringB();
}
