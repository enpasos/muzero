package ai.enpasos.muzero.platform.agent.e_experience.box;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BoxesTest {
//
//    @Test
//    void encode() {
//        Integer[] boxes = {1, 2, 3};
//        String encoded = Boxes.encode(boxes);
//        assertEquals("1,2,3", encoded);
//    }
//
//    @Test
//    void decode() {
//        String encoded = "1,2,3";
//        Integer[] decoded = Boxes.decode(encoded);
//        assertArrayEquals(new Integer[]{1, 2, 3}, decoded);
//    }
//
//    @Test
//    void decodeEmpty() {
//        String encoded = "";
//        Integer[] decoded = Boxes.decode(encoded);
//        assertArrayEquals(new Integer[]{}, decoded);
//    }
//
//    @Test
//    void encodeDecode() {
//        Integer[] boxes = {1, 2, 3};
//        String encoded = Boxes.encode(boxes);
//        Integer[] decoded = Boxes.decode(encoded);
//        assertArrayEquals(boxes, decoded);
//    }

    @Test
    void toUOk() {
        Integer[] input = {0, 0, 0, 0, 0};
        Boxes.toUOk(input, 1, false, true);
        assertArrayEquals(new Integer[]{1, 0, 0, 0, 0}, input);
    }

    @Test
    void toUOk2() {
        Integer[] input = {0, 0, 0, 0, 0};
        Boxes.toUOk(input, 2, false, true);
        assertArrayEquals(new Integer[]{1, 1, 0, 0, 0}, input);
    }

    @Test
    void toUOk3() {
        Integer[] input = {0, 0, 0, 0, 0};
        Boxes.toUOk(input, 3, false, true);
        assertArrayEquals(new Integer[]{1, 1, 1, 0, 0}, input);
    }

    @Test
    void toUOk4() {
        Integer[] input = {2, 1, 1, 1, 0};
        Boxes.toUOk(input, 2, false, true);
        assertArrayEquals(new Integer[]{3, 2, 0, 0, 0}, input);
    }


    @Test
    void toUOk5() {
        Integer[] input = {2, 1, 1, 1, 0};
        Boxes.toUOk(input, -1, false, true);
        assertArrayEquals(new Integer[]{0, 0, 0, 0, 0}, input);
    }
}
