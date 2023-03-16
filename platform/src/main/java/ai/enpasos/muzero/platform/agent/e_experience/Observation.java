package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import com.google.protobuf.ByteString;
import lombok.Builder;
import lombok.Data;

import java.util.BitSet;

@Data
@Builder
public class Observation {

    private int partSize;
    private BitSet partA;
    private BitSet partB;

    public int addTo(BitSet rawResult, int index, OneOfTwoPlayer currentPlayer) {
        BitSet a = partA;
        BitSet b = partB;
        boolean flip = currentPlayer != OneOfTwoPlayer.PLAYER_A;
        for (int i = 0; i < a.size(); i++) {
            rawResult.set(index + i, flip ? !b.get(i) : a.get(i));
        }
        index += partSize;
        for (int i = 0; i < b.size(); i++) {
            rawResult.set(index + i, flip ? !a.get(i) : b.get(i));
        }
        index += partSize;
        return index;
    }

    public Observation clone() {
        return Observation.builder()
                .partSize(partSize)
                .partA((BitSet) partA.clone())
                .partB((BitSet) partB.clone())
                .build();
    }


    public static float[] bitSetToFloatArray(int size, BitSet input) {
        float[] result = new float[size];
        for (int i = 0; i < size; i++) {
            result[i] = input.get(i) ? 1f : 0f;
        }
        return result;
    }

    public ByteString toByteString() {
         return ByteString.copyFrom(partA.toByteArray()).concat(ByteString.copyFrom(partB.toByteArray()));
    }
    public static Observation fromByteStringAndPartSize(ByteString input, int partSize) {
        int n = input.size() / 2;
        return Observation.builder()
                .partSize(partSize)
                .partA(BitSet.valueOf(input.substring(0, n).toByteArray()))
                .partB(BitSet.valueOf(input.substring(n).toByteArray()))
                .build();
    }
}
