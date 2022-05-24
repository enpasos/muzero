package ai.enpasos.muzero.platform.agent.gamebuffer;

import ai.enpasos.muzero.platform.agent.memorize.Game;
import ai.enpasos.muzero.platform.agent.memorize.GameDTO;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBuffer;
import ai.enpasos.muzero.platform.agent.memorize.ReplayBufferDTO;
import ai.enpasos.muzero.platform.agent.memory.protobuf.ReplayBufferProto;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class ReplayBufferDTOTest {

    @Test
    void testProtobuffer() throws Exception {
        ReplayBufferDTO buffer = new ReplayBufferDTO();
        buffer.setGameClassName("some.javaclass");
        GameDTO g = new GameDTO();
        g.setRootValues(List.of(1f, 2f));
        g.setRewards(List.of(3f, 4f));
        g.setActions(List.of(5, 6));
        g.setPolicyTargets(List.of(new float[]{7f, 8f}, new float[]{209f, 210f}));
        g.setTStateA(1);
        g.setTStateB(2);
        g.setNetworkName("networkName");
        buffer.getData().add(g);
        ReplayBufferProto proto = buffer.proto();
        byte[] raw = proto.toByteArray();
        ReplayBufferProto proto2 = ReplayBufferProto.parseFrom(raw);
        assertEquals(proto, proto2);
        ReplayBufferDTO buffer2 = new ReplayBufferDTO();
        buffer2.deproto(proto);
        log.info(Arrays.toString(buffer.getData().get(0).getPolicyTargets().get(0)));
        log.info(Arrays.toString(buffer.getData().get(0).getPolicyTargets().get(1)));
        log.info(Arrays.toString(buffer2.getData().get(0).getPolicyTargets().get(0)));
        log.info(Arrays.toString(buffer2.getData().get(0).getPolicyTargets().get(1)));
        assertTrue(Arrays.equals(buffer.getData().get(0).getPolicyTargets().get(0), buffer2.getData().get(0).getPolicyTargets().get(0)));
        assertTrue(Arrays.equals(buffer.getData().get(0).getPolicyTargets().get(1), buffer2.getData().get(0).getPolicyTargets().get(1)));
        assertEquals( buffer.getData().get(0).getRewards().get(0), buffer2.getData().get(0).getRewards().get(0));
        assertEquals( buffer.getData().get(0).getRewards().get(1), buffer2.getData().get(0).getRewards().get(1));
        assertEquals( buffer.getData().get(0).getActions().get(0), buffer2.getData().get(0).getActions().get(0));
        assertEquals( buffer.getData().get(0).getActions().get(1), buffer2.getData().get(0).getActions().get(1));
        assertTrue( buffer.equals(buffer2) );
    }

}
