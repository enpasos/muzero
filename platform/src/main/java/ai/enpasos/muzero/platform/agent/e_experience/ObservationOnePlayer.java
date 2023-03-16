package ai.enpasos.muzero.platform.agent.e_experience;

import ai.enpasos.muzero.platform.environment.OneOfTwoPlayer;
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
    public int addTo(BitSet rawResult, int index) {

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


    @Override
    public ByteString toByteString() {
         return ByteString.copyFrom(part.toByteArray());
    }
}
