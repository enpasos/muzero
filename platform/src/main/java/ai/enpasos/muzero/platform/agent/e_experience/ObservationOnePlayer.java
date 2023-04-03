package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.agent.a_loopcontrol.episode.Player;
import com.google.protobuf.ByteString;
import lombok.Builder;
import lombok.Data;

import java.util.BitSet;

@Data
@Builder
public class ObservationOnePlayer implements Observation {

    private int partSize;
    private BitSet part;

    @Override
    public int addTo(Player player, BitSet rawResult, int index) {

        for (int i = 0; i < partSize; i++) {
            rawResult.set(index + i,   part.get(i));
        }

        index += partSize;
        return index;
    }

    @Override
    public Observation clone() {
        return ObservationOnePlayer.builder()
                .partSize(partSize)
                .part((BitSet) part.clone())
                .build();
    }

    // implement equals method
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObservationOnePlayer that = (ObservationOnePlayer) o;

        if (partSize != that.partSize) return false;
        return part.equals(that.part);
    }

    // implement hashCode method
    @Override
    public int hashCode() {
        int result = partSize;
        result = 31 * result + part.hashCode();
        return result;
    }

    static Observation fromByteStringAndPartSize(ByteString input, int partSize) {

        return ObservationOnePlayer.builder()
                .partSize(partSize)
                .part(BitSet.valueOf(input.toByteArray()))
                .build();
    }


    @Override
    public ByteString toByteStringA() {
         return ByteString.copyFrom(part.toByteArray());
    }
    @Override
    public ByteString toByteStringB() {
        throw new RuntimeException("Not implemented");
    }
}
