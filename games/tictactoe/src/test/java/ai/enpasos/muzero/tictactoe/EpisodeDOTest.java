package ai.enpasos.muzero.tictactoe;

import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.repo.EpisodeRepo;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class EpisodeDOTest {

    @Autowired
    MuZeroConfig config;

    @Autowired
    EpisodeRepo episodeRepo;


    @Test
    void equalityTest() throws Exception {

        // do this "trivial" test because of a hibernate bug
        // https://hibernate.atlassian.net/browse/HHH-5409
        // Hibernates PersistentBag does not implement equals and hashCode correctly

        Game g1 = config.getGame(List.of(0,3,1,4,2));
        g1.setReanalyse(true);
        Game g2 = config.getGame(List.of(0,3,1,4,2));
        g2.setReanalyse(false);
        Game g3 = config.getGame(List.of(3,0,4,1,5));
        assertTrue(g1.equals(g2));
        assertFalse(g1.equals(g3));
        assertTrue(g1.hashCode() == g2.hashCode());
        assertFalse(g1.hashCode() == g3.hashCode());
        Set<Game> gameSet = new HashSet<>();
        gameSet.add(g1);
        gameSet.add(g2);
        gameSet.add(g3);
        assertEquals(2, gameSet.size());
        Set<EpisodeDO> dtoSet = new HashSet<>();
        dtoSet.add(g1.getEpisodeDO());
        dtoSet.add(g2.getEpisodeDO());
        dtoSet.add(g3.getEpisodeDO());
        assertEquals(2, dtoSet.size());

        g1.setEpisodeDO(episodeRepo.save(g1.getEpisodeDO()));
        g2.setEpisodeDO(episodeRepo.save(g2.getEpisodeDO()));
        g3.setEpisodeDO(episodeRepo.save(g3.getEpisodeDO()));


        assertTrue(g1.equals(g2));
        assertFalse(g1.equals(g3));
        assertTrue(g1.hashCode() == g2.hashCode());
        assertFalse(g1.hashCode() == g3.hashCode());

    }

}
