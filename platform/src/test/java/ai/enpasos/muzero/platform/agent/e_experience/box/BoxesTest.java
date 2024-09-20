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
    void updateBoxes() {
        int[] array = {0, 0, 0, 0, 0};
        array = Boxes.updateBoxes(array, 1, false, true, List.of(0)).getValue();
        assertArrayEquals(new int[]{1, 0}, array);
    }

    @Test
    void updateBoxes2() {
        int[] array  = {5, 4, 3, 2, 1};
        array = Boxes.updateBoxes(array, 2, false, true, List.of(1,2,3,4,5)).getValue();
        assertArrayEquals(new int[]{6, 5, 0}, array);
    }

    @Test
    void updateBoxes3() {
        int[] array = {0, 0, 0, 0, 0};
        array = Boxes.updateBoxes(array, 3, false, true, List.of(0)).getValue();
        assertArrayEquals(new int[]{1, 1, 1, 0}, array);
    }

    @Test
    void updateBoxes4() {
        int[] array = {2, 1, 1, 1, 0};
        array = Boxes.updateBoxes(array, 2, false, true, List.of(0, 1, 2)).getValue();
        assertArrayEquals(new int[]{3, 2, 0}, array);
    }


    @Test
    void updateBoxes5() {
        int[] array = {2, 1, 1, 1, 0};
        array = Boxes.updateBoxes(array, -1, false, true, List.of(0, 1,2)).getValue();
        assertArrayEquals(new int[]{0}, array);
    }

    @Test
    void updateBoxes6() {
        int[] array = {0};
        array = Boxes.updateBoxes(array, 0, false, true, List.of(0)).getValue();
        assertArrayEquals(new int[]{0}, array);
    }

    @Test
    void updateBoxes7() {
        int[] array = {5, 4, 3, 2, 1};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, false, false, List.of(1,2,3,4,5)) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{5, 4, 0}, result.getValue());
    }

    @Test
    void updateBoxes8() {
        int[] array = {5, 4, 3, 2, 0};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 4, false, false, List.of(0,2,3,4,5)) ;
        assertFalse(result.getKey());
        assertArrayEquals(new int[]{5, 4, 3, 2, 0}, result.getValue());
    }

    @Test
    void updateBoxes9() {
        int[] array = {5, 4};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, true, false, List.of(4,5)) ;
        assertFalse(result.getKey());
        assertArrayEquals(new int[]{5, 4}, result.getValue());
    }

    @Test
    void updateBoxes10() {
        int[] array = {5, 4};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, true, true, List.of(4,5)) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{6, 5}, result.getValue());
    }

    @Test
    void updateBoxes11() {
        int[] array = {5, 4};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, false, true, List.of(4,5)) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{6, 5, 0}, result.getValue());
    }

    @Test
    void updateBoxes11B() {
        int[] array = {5, 4};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, false, true, List.of(5)) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{6, 4, 0}, result.getValue());
    }

    @Test
    void updateBoxes11C() {
        int[] array = {5, 4};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, false, true, List.of(4)) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{5, 5, 0}, result.getValue());
    }

    @Test
    void updateBoxes12() {
        int[] array = {5};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 0, true, true, List.of(5)) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{6}, result.getValue());
    }
    @Test
    void updateBoxes13() {
        int[] array = {7,0};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, true, true, List.of(5)) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{7,1}, result.getValue());
    }
    @Test
    void updateBoxes13B() {
        int[] array = {7,0};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, false, true, List.of(5)) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{7,1, 0}, result.getValue());
    }

    @Test
    void hasRelevantBox() {
        assertTrue(Boxes.hasRelevantBox(List.of(1,2), new int[]{3, 2, 1, 0}, 3));
    }


    @Test
    void getBox() {
        assertEquals(3, Boxes.getBox(new int[]{3, 0}, 1));
    }
}
