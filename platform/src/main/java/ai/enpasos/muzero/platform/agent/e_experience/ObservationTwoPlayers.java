package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.episode.Player;
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



    @Override
    public int addTo(Player player, BitSet rawResult, int index) {
        BitSet a = partA;
        BitSet b = partB;
        OneOfTwoPlayer currentPlayer = (OneOfTwoPlayer) player;
        boolean flip = currentPlayer != null && currentPlayer != OneOfTwoPlayer.PLAYER_A;
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
    public ByteString toByteStringA() {
        return  ByteString.copyFrom(partA.toByteArray());
    }
    @Override
    public ByteString toByteStringB() {
        return  ByteString.copyFrom(partB.toByteArray());
    }

    static Observation fromByteStringAndPartSize(ByteString inputA, ByteString inputB, int partSize) {

        return ObservationTwoPlayers.builder()
                .partSize(partSize)
                .partA(BitSet.valueOf(inputA.toByteArray()))
                .partB(BitSet.valueOf(inputB.toByteArray()))
                .build();
    }

    // implement equals method
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ObservationTwoPlayers that = (ObservationTwoPlayers) o;
        return partSize == that.partSize &&
                partA.equals(that.partA) &&
                partB.equals(that.partB);
    }


}
