package ai.enpasos.muzero.platform.agent.e_experience;

import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.*;

class ObservationTwoPlayersTest {

    @Test
    void byteStringRoundtripTest() {

        BitSet rawA = new BitSet(12);
        rawA.set(0, 2, true);

        BitSet rawB = new BitSet(12);
        rawB.set(3, 9, true);

        ObservationTwoPlayers observation = new ObservationTwoPlayers(12, rawA, rawB);

        ByteString byteStringA = observation.toByteStringA();
        ByteString byteStringB = observation.toByteStringB();
        assertEquals(observation, ObservationTwoPlayers.fromByteStringAndPartSize(byteStringA, byteStringB, 12));
    }

    @Test
    void byteStringRoundtrip2Test() {
        int size = 12;
        BitSet rawA = new BitSet(size);

        BitSet rawB = new BitSet(size);

        ObservationTwoPlayers observation = new ObservationTwoPlayers(size, rawA, rawB);

        ByteString byteStringA = observation.toByteStringA();
        ByteString byteStringB = observation.toByteStringB();
        assertEquals(observation, ObservationTwoPlayers.fromByteStringAndPartSize(byteStringA,  byteStringB, size));
    }


}