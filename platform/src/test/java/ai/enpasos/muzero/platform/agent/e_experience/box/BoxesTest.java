package ai.enpasos.muzero.platform.agent.e_experience.box;

import org.junit.jupiter.api.Test;

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
        int[] input = {0, 0, 0, 0, 0};
        Boxes.toUOk(input, 1, false, true);
        assertArrayEquals(new int[]{1, 0, 0, 0, 0}, input);
    }

    @Test
    void toUOk2() {
        int[] input = {0, 0, 0, 0, 0};
        Boxes.toUOk(input, 2, false, true);
        assertArrayEquals(new int[]{1, 1, 0, 0, 0}, input);
    }

    @Test
    void toUOk3() {
        int[] input = {0, 0, 0, 0, 0};
        Boxes.toUOk(input, 3, false, true);
        assertArrayEquals(new int[]{1, 1, 1, 0, 0}, input);
    }

    @Test
    void toUOk4() {
        int[] input = {2, 1, 1, 1, 0};
        Boxes.toUOk(input, 2, false, true);
        assertArrayEquals(new int[]{3, 2, 0, 0, 0}, input);
    }


    @Test
    void toUOk5() {
        int[] input = {2, 1, 1, 1, 0};
        Boxes.toUOk(input, -1, false, true);
        assertArrayEquals(new int[]{0, 0, 0, 0, 0}, input);
    }
}
