package ai.enpasos.muzero.platform.agent.d_model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class BoxingTest {

    @Test
    void isUsedTest() {
        assertTrue(Boxing.isUsed(0, 0));
        assertTrue(Boxing.isUsed(0, 1));
        assertTrue(Boxing.isUsed(0, 2));
        assertTrue(Boxing.isUsed(0, 3));
        assertTrue(Boxing.isUsed(0, 4));
        assertTrue(Boxing.isUsed(0, 5));
        assertTrue(Boxing.isUsed(0, 6));
        assertTrue(Boxing.isUsed(0, 7));
        assertTrue(Boxing.isUsed(0, 8));
        assertTrue(Boxing.isUsed(0, 9));
        assertTrue(Boxing.isUsed(0, 10));

        assertFalse(Boxing.isUsed(1, 1));
        assertTrue(Boxing.isUsed(1, 2));
        assertFalse(Boxing.isUsed(1, 3));
        assertTrue(Boxing.isUsed(1, 4));
        assertFalse(Boxing.isUsed(1, 5));
        assertTrue(Boxing.isUsed(1, 6));
        assertFalse(Boxing.isUsed(1, 7));

        assertFalse(Boxing.isUsed(2, 1));
        assertFalse(Boxing.isUsed(2, 2));
        assertFalse(Boxing.isUsed(2, 3));
        assertTrue(Boxing.isUsed(2, 4));
        assertFalse(Boxing.isUsed(2, 5));
        assertFalse(Boxing.isUsed(2, 6));
        assertFalse(Boxing.isUsed(2, 7));
        assertTrue(Boxing.isUsed(2, 8));

        assertFalse(Boxing.isUsed(3, 1));
        assertFalse(Boxing.isUsed(3, 2));
        assertFalse(Boxing.isUsed(3, 3));
        assertFalse(Boxing.isUsed(3, 4));
        assertFalse(Boxing.isUsed(3, 5));
        assertFalse(Boxing.isUsed(3, 6));
        assertFalse(Boxing.isUsed(3, 7));
        assertTrue(Boxing.isUsed(3, 8));
        assertFalse(Boxing.isUsed(3, 9));

    }

    @Test
    void intervallTest() {
        assertEquals(1, Boxing.intervall(0));
        assertEquals(2, Boxing.intervall(1));
        assertEquals(4, Boxing.intervall(2));
        assertEquals(8, Boxing.intervall(3));
    }

    @Test
    void boxesRelevantTest() {

        assertArrayEquals(new Integer[]{0}, Boxing.boxesRelevant(1, 10).toArray());
        assertArrayEquals(new Integer[]{0,1,2,3}, Boxing.boxesRelevant(8, 10).toArray());



    }


    @Test
    void boxesRelevantAndOccupied() {
        assertArrayEquals(new Integer[]{0,1,2,3}, Boxing.boxesRelevantAndOccupied(List.of(0, 2, 3, 4, 5, 6, 7, 8, 9, 10),1358, 2).toArray());
    }
}
