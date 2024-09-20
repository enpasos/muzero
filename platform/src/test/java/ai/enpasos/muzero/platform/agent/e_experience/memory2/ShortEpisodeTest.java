package ai.enpasos.muzero.platform.agent.e_experience.memory2;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShortEpisodeTest {


    @Test
    void someTestsA() {
        ShortEpisode shortEpisode = getShortEpisodeA();
        assertEquals(8, shortEpisode.getMaxT());
        assertEquals(2, shortEpisode.getUnrollSteps());
    }

    @Test
    void someTestsB() {
        ShortEpisode shortEpisode = getShortEpisodeB();
        assertEquals(8, shortEpisode.getMaxT());
        assertEquals(1, shortEpisode.getUnrollSteps());
    }

    @Test
    void someTestsC() {
        ShortEpisode shortEpisode = getShortEpisodeC();
        assertEquals(8, shortEpisode.getMaxT());
        assertEquals(8, shortEpisode.getUnrollSteps());
    }

    private static ShortEpisode getShortEpisodeA() {
        // build an episode with 9 timesteps, the last two are uokclosed, uok = 1 everywhere but on t=3 it is uok = -1 and on the last uok = 0
        return ShortEpisode.builder()
                .id(1L)
                .shortTimesteps(List.of(
                        ShortTimestep.builder().id(1000L).t(0).uOk(1).build(),
                        ShortTimestep.builder().id(1001L).t(1).uOk(1).build(),
                        ShortTimestep.builder().id(1002L).t(2).uOk(1).build(),
                        ShortTimestep.builder().id(1003L).t(3).uOk(-1).build(),
                        ShortTimestep.builder().id(1004L).t(4).uOk(1).build(),
                        ShortTimestep.builder().id(1005L).t(5).uOk(1).build(),
                        ShortTimestep.builder().id(1006L).t(6).uOk(1).build(),
                        ShortTimestep.builder().id(1007L).t(7).uOk(1).uOkClosed(true).build(),
                        ShortTimestep.builder().id(1008L).t(8).uOk(0).uOkClosed(true).build()
                )).build();
    }
    private static ShortEpisode getShortEpisodeB() {
        // build an episode with 9 timesteps, the last two are uokclosed, uok = 1 everywhere but on t=3 it is uok = -1 and on the last uok = 0
        return ShortEpisode.builder()
                .id(1L)
                .shortTimesteps(List.of(
                        ShortTimestep.builder().id(1000L).t(0).uOk(-1).build(),
                        ShortTimestep.builder().id(1001L).t(1).uOk(-1).build(),
                        ShortTimestep.builder().id(1002L).t(2).uOk(-1).build(),
                        ShortTimestep.builder().id(1003L).t(3).uOk(-1).build(),
                        ShortTimestep.builder().id(1004L).t(4).uOk(-1).build(),
                        ShortTimestep.builder().id(1005L).t(5).uOk(-1).build(),
                        ShortTimestep.builder().id(1006L).t(6).uOk(-1).build(),
                        ShortTimestep.builder().id(1007L).t(7).uOk(-1).build(),
                        ShortTimestep.builder().id(1008L).t(8).uOk(-1).build()
                )).build();
    }

    private static ShortEpisode getShortEpisodeC() {

        return ShortEpisode.builder()
                .id(1L)
                .shortTimesteps(List.of(
                        ShortTimestep.builder().id(1000L).t(0).uOk(1).build(),
                        ShortTimestep.builder().id(1001L).t(1).uOk(7).uOkClosed(true).build(),
                        ShortTimestep.builder().id(1002L).t(2).uOk(5).build(),
                        ShortTimestep.builder().id(1003L).t(3).uOk(6).build(),
                        ShortTimestep.builder().id(1004L).t(4).uOk(3).build(),
                        ShortTimestep.builder().id(1005L).t(5).uOk(2).build(),
                        ShortTimestep.builder().id(1006L).t(6).uOk(2).build(),
                        ShortTimestep.builder().id(1007L).t(7).uOk(1).uOkClosed(true).build(),
                        ShortTimestep.builder().id(1008L).t(8).uOk(0).uOkClosed(true).build()
                )).build();
    }
}
