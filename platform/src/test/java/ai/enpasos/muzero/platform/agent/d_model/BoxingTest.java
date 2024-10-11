package ai.enpasos.muzero.platform.agent.d_model;

import ai.enpasos.muzero.platform.agent.e_experience.box.Boxing;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoxingTest {

    @Test
    void isUsedTest() {
        assertTrue(Boxing.isUsed(1, 1));
        assertTrue(Boxing.isUsed(1, 2));
        assertTrue(Boxing.isUsed(1, 3));
        assertTrue(Boxing.isUsed(1, 4));
        assertTrue(Boxing.isUsed(1, 5));
        assertTrue(Boxing.isUsed(1, 6));
        assertTrue(Boxing.isUsed(1, 7));
        assertTrue(Boxing.isUsed(1, 8));
        assertTrue(Boxing.isUsed(1, 9));
        assertTrue(Boxing.isUsed(1, 10));

        assertFalse(Boxing.isUsed(2, 1));
        assertTrue(Boxing.isUsed(2, 2));
        assertFalse(Boxing.isUsed(2, 3));
        assertTrue(Boxing.isUsed(2, 4));
        assertFalse(Boxing.isUsed(2, 5));
        assertTrue(Boxing.isUsed(2, 6));
        assertFalse(Boxing.isUsed(2, 7));

        assertFalse(Boxing.isUsed(3, 1));
        assertFalse(Boxing.isUsed(3, 2));
        assertFalse(Boxing.isUsed(3, 3));
        assertTrue(Boxing.isUsed(3, 4));
        assertFalse(Boxing.isUsed(3, 5));
        assertFalse(Boxing.isUsed(3, 6));
        assertFalse(Boxing.isUsed(3, 7));
        assertTrue(Boxing.isUsed(3, 8));

        assertFalse(Boxing.isUsed(4, 1));
        assertFalse(Boxing.isUsed(4, 2));
        assertFalse(Boxing.isUsed(4, 3));
        assertFalse(Boxing.isUsed(4, 4));
        assertFalse(Boxing.isUsed(4, 5));
        assertFalse(Boxing.isUsed(4, 6));
        assertFalse(Boxing.isUsed(4, 7));
        assertTrue(Boxing.isUsed(4, 8));
        assertFalse(Boxing.isUsed(4, 9));

    }

    @Test
    void intervallTest() {
        assertEquals(1, Boxing.intervall(1));
        assertEquals(2, Boxing.intervall(2));
        assertEquals(4, Boxing.intervall(3));
        assertEquals(8, Boxing.intervall(4));
    }

    @Test
    void boxesRelevantTest() {

        assertArrayEquals(integerArray(1), Boxing.boxesRelevant(1 ).toArray());

        assertArrayEquals(integerArray(2), Boxing.boxesRelevant(2 ).toArray());
        assertArrayEquals(integerArray(1), Boxing.boxesRelevant(3 ).toArray());
        assertArrayEquals(integerArray(3), Boxing.boxesRelevant(4 ).toArray());
        assertArrayEquals(integerArray(1), Boxing.boxesRelevant(5 ).toArray());
        assertArrayEquals(integerArray(2), Boxing.boxesRelevant(6 ).toArray());
        assertArrayEquals(integerArray(1), Boxing.boxesRelevant(7 ).toArray());
        assertArrayEquals(integerArray(4), Boxing.boxesRelevant(8 ).toArray());
        assertArrayEquals(integerArray(5), Boxing.boxesRelevant(16 ).toArray());
        assertArrayEquals(integerArray(5), Boxing.boxesRelevant(32 ).toArray());
        assertArrayEquals(integerArray(4), Boxing.boxesRelevant(8 ).toArray());


    }


    private Integer[] integerArray(int maxValue) {
        Integer[] result = new Integer[maxValue];
        for (int i = 1; i <= maxValue; i++) {
            result[i-1] = i;
        }
        return result;
    }


//    @Test
//    @Disabled
//    void boxesRelevantAndOccupied() {
//        assertArrayEquals(new Integer[]{0,1,2,3}, Boxing.boxesRelevantAndOccupied(List.of(0, 2, 3, 4, 5, 6, 7, 8, 9, 10),1358, 2).toArray());
//    }
}
