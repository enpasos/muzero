/*
 *  Copyright (c) 2021 enpasos GmbH
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package ai.enpasos.muzero.tictactoe;

import ai.enpasos.muzero.platform.agent.e_experience.Game;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.memory.EpisodeMemory;
import ai.enpasos.muzero.platform.agent.e_experience.memory.EpisodeMemoryImpl;
import ai.enpasos.muzero.platform.agent.e_experience.memory.ObservationStateNode;
import ai.enpasos.muzero.platform.agent.e_experience.memory.StateNode;
import ai.enpasos.muzero.platform.config.MuZeroConfig;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;


@ActiveProfiles("test")
@ExtendWith(SpringExtension.class)
@SpringBootTest
class MemoryTest {




    @Autowired
    MuZeroConfig config;

    @Test
    void createMemoryTest() {
        EpisodeMemory episodeMemory = new EpisodeMemoryImpl(10000);

        Game game1 = gameFromActions( 1, 5, 0, 4, 7, 2, 8, 6 );
        Game game2 = gameFromActions( 1, 5, 0, 4, 7, 2, 8, 3 );
        episodeMemory.add(game1);
        assertArrayEquals(new boolean[]{true, true, true, true, true, true, true, true, true},
                episodeMemory.getLegalNotDeeplyVisitedActions(game1, 0));
        assertArrayEquals(new boolean[]{false, false, false, true, false, false, false, false, false},
                episodeMemory.getLegalNotDeeplyVisitedActions(game1, 7));

        episodeMemory.add(game2);
        assertEquals(10, episodeMemory.getNumberOfStateNodes());
        assertEquals(2, episodeMemory.getNumberOfEpisodes());

        assertEquals(new ObservationStateNode(game1,0), new ObservationStateNode(game2, 0));
        assertEquals(new ObservationStateNode(game1,1), new ObservationStateNode(game2, 1));
        assertEquals(new ObservationStateNode(game1,2), new ObservationStateNode(game2, 2));
        assertEquals(new ObservationStateNode(game1,3), new ObservationStateNode(game2, 3));
        assertEquals(new ObservationStateNode(game1,4), new ObservationStateNode(game2, 4));
        assertNotEquals(new ObservationStateNode(game1,4), new ObservationStateNode(game2, 5));
        assertEquals(new ObservationStateNode(game1,5), new ObservationStateNode(game2, 5));
        assertEquals(new ObservationStateNode(game1,6), new ObservationStateNode(game2, 6));
        assertEquals(new ObservationStateNode(game1,7), new ObservationStateNode(game2, 7));
        assertNotEquals(new ObservationStateNode(game1,8), new ObservationStateNode(game2, 8));
        assertEquals(new ObservationStateNode(game1,2).hashCode(), new ObservationStateNode(game2,2).hashCode());


        assertArrayEquals(new boolean[]{false, false, false, false, false, false, false, false, false},
                episodeMemory.getLegalNotDeeplyVisitedActions(game2, 7));


        assertArrayEquals(new boolean[]{false, false, false, false, false, false, false, false, false},
                episodeMemory.getLegalNotDeeplyVisitedActions(game1, 7));


        assertFalse(episodeMemory.visitsUnvisitedAction(game1));
        Game game3 = gameFromActions( 1, 5, 7, 4, 0, 2, 8, 3 );
        assertTrue(episodeMemory.visitsUnvisitedAction(game3));
        episodeMemory.add(game3);
        assertFalse(episodeMemory.visitsUnvisitedAction(game3));

        episodeMemory.add(gameFromActions( 1, 5, 0, 4, 2));
        episodeMemory.add(gameFromActions( 0, 5, 4, 3, 8));
        episodeMemory.add(gameFromActions( 2, 1, 4, 3, 6));
        episodeMemory.add(gameFromActions( 1, 2, 4, 3, 7));
        episodeMemory.add(gameFromActions( 3, 1, 4, 0, 5));
        episodeMemory.add(gameFromActions( 6, 1, 7, 0, 8));
        assertEquals(9, episodeMemory.getNumberOfEpisodes());
        episodeMemory.setCapacity(5);
        episodeMemory.add(gameFromActions( 3, 1, 4, 0, 5));
        assertEquals(5, episodeMemory.getNumberOfEpisodes());
        int i = 42;

    }


    private Game gameFromActions(int...   actions) {
        Game game = config.newGame(true,true);
        for (int i = 0; i < actions.length; i++) {
            int a = actions[i];
            Objects.requireNonNull(game).apply(config.newAction(a));
        }
        return game;
    }
}
