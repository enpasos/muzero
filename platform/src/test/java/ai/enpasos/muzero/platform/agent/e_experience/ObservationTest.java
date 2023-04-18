package ai.enpasos.muzero.platform.agent.e_experience;

import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.*;

class ObservationTest {




    @Test
    void toByteStringAndFromByteStringB() {
        int partSize = 3;
        BitSet a = new BitSet(partSize);
        a.set(0);
        BitSet b = new BitSet(partSize);
        b.set(1);

        Observation observation = ObservationTwoPlayers.builder()
                .partSize(partSize)
                .partA(a)
                .partB(b)
                .build();

        Observation observation2 = ObservationTwoPlayers.fromByteStringAndPartSize(observation.toByteStringA(), observation.toByteStringB(), partSize);

        assertEquals(observation, observation2);
    }

    @Test
    void toByteStringAndFromByteStringB2() {
        int partSize = 3;
        BitSet a = new BitSet(partSize);
        BitSet b = new BitSet(partSize);
        Observation observation = ObservationTwoPlayers.builder()
                .partSize(partSize)
                .partA(a)
                .partB(b)
                .build();

        ByteString byteStringA = observation.toByteStringA();
        ByteString byteStringB= observation.toByteStringB();
        Observation observation2 = ObservationTwoPlayers.fromByteStringAndPartSize(byteStringA,byteStringB, partSize);

        assertEquals(observation, observation2);
    }
}