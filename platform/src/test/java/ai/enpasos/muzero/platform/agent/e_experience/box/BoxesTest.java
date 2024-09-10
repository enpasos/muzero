package ai.enpasos.muzero.platform.agent.e_experience.box;

import ai.djl.util.Pair;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoxesTest {
//
//    @Test
//    void encode() {
//        int[] boxes = {1, 2, 3};
//        String encoded = Boxes.encode(boxes);
//        assertEquals("1,2,3", encoded);
//    }
//
//    @Test
//    void decode() {
//        String encoded = "1,2,3";
//        int[] decoded = Boxes.decode(encoded);
//        assertArrayEquals(new int[]{1, 2, 3}, decoded);
//    }
//
//    @Test
//    void decodeEmpty() {
//        String encoded = "";
//        int[] decoded = Boxes.decode(encoded);
//        assertArrayEquals(new int[]{}, decoded);
//    }
//
//    @Test
//    void encodeDecode() {
//        int[] boxes = {1, 2, 3};
//        String encoded = Boxes.encode(boxes);
//        int[] decoded = Boxes.decode(encoded);
//        assertArrayEquals(boxes, decoded);
//    }

    @Test
    void toUOk() {
        int[] array = {0, 0, 0, 0, 0};
        array = Boxes.toUOk(array, 1, false, true).getValue();
        assertArrayEquals(new int[]{1, 0}, array);
    }

    @Test
    void toUOk2() {
        int[] array  = {5, 4, 3, 2, 1};
        array = Boxes.toUOk(array, 2, false, true).getValue();
        assertArrayEquals(new int[]{6, 5, 0}, array);
    }

    @Test
    void toUOk3() {
        int[] array = {0, 0, 0, 0, 0};
        array = Boxes.toUOk(array, 3, false, true).getValue();
        assertArrayEquals(new int[]{1, 1, 1, 0}, array);
    }

    @Test
    void toUOk4() {
        int[] array = {2, 1, 1, 1, 0};
        array = Boxes.toUOk(array, 2, false, true).getValue();
        assertArrayEquals(new int[]{3, 2, 0}, array);
    }


    @Test
    void toUOk5() {
        int[] array = {2, 1, 1, 1, 0};
        array = Boxes.toUOk(array, -1, false, true).getValue();
        assertArrayEquals(new int[]{0}, array);
    }

    @Test
    void toUOk6() {
        int[] array = {0};
        array = Boxes.toUOk(array, 0, false, true).getValue();
        assertArrayEquals(new int[]{0}, array);
    }

    @Test
    void toUOk7() {
        int[] array = {5, 4, 3, 2, 1};
        Pair<Boolean, int[]> result = Boxes.toUOk(array, 2, false, false) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{5, 4, 0}, result.getValue());
    }

    @Test
    void toUOk8() {
        int[] array = {5, 4, 3, 2, 0};
        Pair<Boolean, int[]> result = Boxes.toUOk(array, 4, false, false) ;
        assertFalse(result.getKey());
        assertArrayEquals(new int[]{5, 4, 3, 2, 0}, result.getValue());
    }

    @Test
    void toUOk9() {
        int[] array = {5, 4};
        Pair<Boolean, int[]> result = Boxes.toUOk(array, 2, true, false) ;
        assertFalse(result.getKey());
        assertArrayEquals(new int[]{5, 4}, result.getValue());
    }

    @Test
    void toUOk10() {
        int[] array = {5, 4};
        Pair<Boolean, int[]> result = Boxes.toUOk(array, 2, true, true) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{6, 5}, result.getValue());
    }

    @Test
    void toUOk11() {
        int[] array = {5, 4};
        Pair<Boolean, int[]> result = Boxes.toUOk(array, 2, false, true) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{6, 5, 0}, result.getValue());
    }

    @Test
    void hasRelevantBox() {
        assertTrue(Boxes.hasRelevantBox(List.of(1,2), new int[]{3, 2, 1, 0}));
    }


}
