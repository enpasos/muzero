package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
import com.google.protobuf.ByteString;
import lombok.Builder;
import lombok.Data;

import java.util.BitSet;

@Data
@Builder
public class ObservationTwoPlayers implements Observation {

    private int partSize;
    private BitSet partA;
    private BitSet partB;

    OneOfTwoPlayer currentPlayer;


    @Override
    public int addTo(BitSet rawResult, int index) {
        BitSet a = partA;
        BitSet b = partB;
        boolean flip = currentPlayer != OneOfTwoPlayer.PLAYER_A;
        for (int i = 0; i < partSize; i++) {
            rawResult.set(index + i, flip ? b.get(i) : a.get(i));
        }
        index += partSize;
        for (int i = 0; i < partSize; i++) {
            rawResult.set(index + i, flip ? a.get(i) : b.get(i));
        }
        index += partSize;
        return index;
    }

    @Override
    public Observation clone() {
        return ObservationTwoPlayers.builder()
                .partSize(partSize)
                .partA((BitSet) partA.clone())
                .partB((BitSet) partB.clone())
                .build();
    }


    @Override
    public ByteString toByteString() {
         return ByteString.copyFrom(partA.toByteArray()).concat(ByteString.copyFrom(partB.toByteArray()));
    }

    static Observation fromByteStringAndPartSize(ByteString input, int partSize) {
        int n = input.size() / 2;
        return ObservationTwoPlayers.builder()
                .partSize(partSize)
                .partA(BitSet.valueOf(input.substring(0, n).toByteArray()))
                .partB(BitSet.valueOf(input.substring(n).toByteArray()))
                .build();
    }
}
