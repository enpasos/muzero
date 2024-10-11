package ai.enpasos.muzero.platform.agent.e_experience.box;

import ai.djl.util.Pair;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BoxesTest {


    @Test
    void updateBoxes() {
        int[] array = {0, 0, 0, 0, 0};
        array = Boxes.updateBoxes(array, 1, false, true, List.of(0), 3, new TimeStepDO(), 10, 10).getValue();
        assertArrayEquals(new int[]{1, 0}, array);
    }

    @Test
    void updateBoxes2() {
        int[] array  = {5, 4, 3, 2, 1};
        array = Boxes.updateBoxes(array, 2, false, true, List.of(1,2,3,4,5), 3, new TimeStepDO(), 10, 10).getValue();
        assertArrayEquals(new int[]{5, 5, 0}, array);
    }

    @Test
    void updateBoxes3() {
        int[] array = {0, 0, 0, 0, 0};
        array = Boxes.updateBoxes(array, 3, false, true, List.of(0), 3, new TimeStepDO(), 10, 10).getValue();
        assertArrayEquals(new int[]{1, 1, 1, 0}, array);
    }

    @Test
    void updateBoxes4() {
        int[] array = {2, 1, 1, 1, 0};
        array = Boxes.updateBoxes(array, 2, false, true, List.of(0, 1, 2), 3, new TimeStepDO(), 10, 10).getValue();
        assertArrayEquals(new int[]{3, 2, 0}, array);
    }


    @Test
    void updateBoxes5() {
        int[] array = {2, 1, 1, 1, 0};
        array = Boxes.updateBoxes(array, -1, false, true, List.of(0, 1,2), 3, new TimeStepDO(), 10, 10).getValue();
        assertArrayEquals(new int[]{0}, array);
    }

    @Test
    void updateBoxes6() {
        int[] array = {0};
        array = Boxes.updateBoxes(array, 0, false, true, List.of(0), 3, new TimeStepDO(), 10, 10).getValue();
        assertArrayEquals(new int[]{0}, array);
    }

    @Test
    void updateBoxes7() {
        int[] array = {5, 4, 3, 2, 1};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, false, false, List.of(1,2,3,4,5), 3, new TimeStepDO(), 10, 10) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{5, 4, 0}, result.getValue());
    }

    @Test
    void updateBoxes8() {
        int[] array = {5, 4, 3, 2, 0};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 4, false, false, List.of(0,2,3,4,5), 3, new TimeStepDO(), 10, 10) ;
        assertFalse(result.getKey());
        assertArrayEquals(new int[]{5, 4, 3, 2, 0}, result.getValue());
    }

    @Test
    void updateBoxes9() {
        int[] array = {5, 4};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, true, false, List.of(4,5), 3, new TimeStepDO(), 10, 10) ;
        assertFalse(result.getKey());
        assertArrayEquals(new int[]{5, 4}, result.getValue());
    }

    @Test
    void updateBoxes10() {
        int[] array = {5, 4};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, true, true, List.of(4,5), 3, new TimeStepDO(), 10, 10) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{5, 5}, result.getValue());
    }

    @Test
    void updateBoxes11() {
        int[] array = {5, 4};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, false, true, List.of(4,5), 3, new TimeStepDO(), 10, 10) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{5, 5, 0}, result.getValue());
    }

    @Test
    void updateBoxes11B() {
        int[] array = {5, 4};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, false, true, List.of(5), 3, new TimeStepDO(), 10, 10) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{5, 4, 0}, result.getValue());
    }

    @Test
    void updateBoxes11C() {
        int[] array = {5, 4};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, false, true, List.of(4), 3, new TimeStepDO(), 10, 10) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{5, 5, 0}, result.getValue());
    }

    @Test
    void updateBoxes12() {
        int[] array = {5};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 0, true, true, List.of(5), 3, new TimeStepDO(), 10, 10) ;
        assertFalse(result.getKey());
        assertArrayEquals(new int[]{5}, result.getValue());
    }
    @Test
    void updateBoxes13() {
        int[] array = {5,0};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, true, true, List.of(5), 3, new TimeStepDO(), 10, 10) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{5,1}, result.getValue());
    }
    @Test
    void updateBoxes13B() {
        int[] array = {5,0};
        Pair<Boolean, int[]> result = Boxes.updateBoxes(array, 2, false, true, List.of(5), 3, new TimeStepDO(), 10, 10) ;
        assertTrue(result.getKey());
        assertArrayEquals(new int[]{5,1, 0}, result.getValue());
    }

    @Test
    void hasRelevantBox() {
        assertTrue(Boxes.hasRelevantBox(List.of(1,2), new int[]{3, 2, 1, 0}, 3));
    }


    @Test
    void getBox() {
        assertEquals(3, Boxes.getBox(new int[]{3, 0}, 1));
    }

    @Test
    void getBox2() {
        assertEquals(0, Boxes.getBox(new int[]{3, 0}, 2));
    }
}
