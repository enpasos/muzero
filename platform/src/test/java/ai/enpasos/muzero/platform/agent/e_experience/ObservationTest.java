package ai.enpasos.muzero.platform.agent.e_experience;

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

        Observation observation2 = ObservationTwoPlayers.fromByteStringAndPartSize(observation.toByteString(), partSize);

        assertEquals(observation, observation2);
    }
}