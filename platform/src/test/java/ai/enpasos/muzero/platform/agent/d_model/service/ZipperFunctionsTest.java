package ai.enpasos.muzero.platform.agent.d_model.service;

import ai.enpasos.muzero.platform.agent.e_experience.db.domain.EpisodeDO;
import ai.enpasos.muzero.platform.agent.e_experience.db.domain.TimeStepDO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static ai.enpasos.muzero.platform.agent.d_model.service.ZipperFunctions.*;
import static org.junit.jupiter.api.Assertions.*;

class ZipperFunctionsTest {

    @Test
    void bOKRoundtrip3() {
        boolean[][][] bOk = {{
                {false, false, false},
                {false, true, true},
                {false, false, true}
        }, {
                {false, false },
                {false, true }
        }
        };
        boolean[][][] bOk_Expected_From_S = bOk;

        List<EpisodeDO > episodeDOList = new ArrayList<>();
        List<TimeStepDO > tsList = new ArrayList<>();
        tsList.add(TimeStepDO.builder().t(0).action(1).build());
        tsList.add(TimeStepDO.builder().t(1).action(2).build());
        tsList.add(TimeStepDO.builder().t(2). build());
        episodeDOList.add(EpisodeDO.builder().timeSteps(tsList).build());

        tsList = new ArrayList<>();
        tsList.add(TimeStepDO.builder().t(0).action(3).build());
        tsList.add(TimeStepDO.builder().t(1). build());
        episodeDOList.add(EpisodeDO.builder().timeSteps(tsList).build());

        uOK_in_Episodes_From_b_OK(bOk, episodeDOList);
        assertEquals(-1, episodeDOList.get(0).getTimeStep(0).getUOk());
        assertEquals(1, episodeDOList.get(0).getTimeStep(1).getUOk());
        assertEquals(0, episodeDOList.get(0).getTimeStep(2).getUOk());

        assertEquals(-1, episodeDOList.get(1).getTimeStep(0).getUOk());
        assertEquals(0, episodeDOList.get(1).getTimeStep(1).getUOk());


        boolean[][][] bOk2 =  b_OK_From_UOk_in_Episodes(episodeDOList);

        bOk = bOk_Expected_From_S;
        assertEquals(bOk.length, bOk2.length);
        assertEquals(bOk[0].length, bOk2[0].length);
        assertEquals(bOk[0][0].length, bOk2[0][0].length);
        for (int e = 0; e < bOk.length; e++) {
            for (int i = 0; i < bOk[e].length; i++) {
                assertArrayEquals(bOk[e][i], bOk2[e][i]);
            }
        }
    }


    @Test
    void testTransferB_OK_to_Episodes() {
        boolean[][][] bOk = {{
                {false, false, true},
                {false, true, true},
                {false, false, true}
        }, {
                {false, false },
                {false, true }
        }
        };
        boolean[][][] bOk_Expected_From_S = bOk;

        List<EpisodeDO > episodeDOList = new ArrayList<>();
        List<TimeStepDO > tsList = new ArrayList<>();
        tsList.add(TimeStepDO.builder().t(0).action(1).build());
        tsList.add(TimeStepDO.builder().t(1).action(2).build());
        tsList.add(TimeStepDO.builder().t(2).build());
        episodeDOList.add(EpisodeDO.builder().timeSteps(tsList).build());

        tsList = new ArrayList<>();
        tsList.add(TimeStepDO.builder().t(0).action(3).build());
        tsList.add(TimeStepDO.builder().t(1).build());
        episodeDOList.add(EpisodeDO.builder().timeSteps(tsList).build());

        s_in_Episodes_From_b_OK(bOk, episodeDOList);
        assertEquals(0, episodeDOList.get(0).getTimeStep(0).getS());
        assertEquals(1, episodeDOList.get(0).getTimeStep(1).getS());
        assertEquals(3, episodeDOList.get(0).getTimeStep(2).getS());

        assertEquals(0, episodeDOList.get(1).getTimeStep(0).getS());
        assertEquals(1, episodeDOList.get(1).getTimeStep(1).getS());


        assertEquals(-1, episodeDOList.get(0).getTimeStep(0).getUOk());
        assertEquals(1, episodeDOList.get(0).getTimeStep(1).getUOk());
        assertEquals(0, episodeDOList.get(0).getTimeStep(2).getUOk());

        assertEquals(-1, episodeDOList.get(1).getTimeStep(0).getUOk());
        assertEquals(0, episodeDOList.get(1).getTimeStep(1).getUOk());

        assertFalse( episodeDOList.get(0).getTimeStep(0).isUOkClosed());
        assertTrue( episodeDOList.get(0).getTimeStep(1).isUOkClosed());
        assertTrue( episodeDOList.get(0).getTimeStep(2).isUOkClosed());

        assertFalse( episodeDOList.get(1).getTimeStep(0).isUOkClosed());
        assertTrue( episodeDOList.get(1).getTimeStep(1).isUOkClosed());

    }
    @Test
    void bOKRoundtrip2() {
        boolean[][][] bOk = {{
                {false, false, true},
                {false, true, true},
                {false, false, true}
        }, {
                {false, false },
                {false, true }
        }
        };
        boolean[][][] bOk_Expected_From_S = bOk;

        List<EpisodeDO > episodeDOList = new ArrayList<>();
        List<TimeStepDO > tsList = new ArrayList<>();
        tsList.add(TimeStepDO.builder().t(0).action(1).build());
        tsList.add(TimeStepDO.builder().t(1).action(2).build());
        tsList.add(TimeStepDO.builder().t(2). build());
        episodeDOList.add(EpisodeDO.builder().timeSteps(tsList).build());

         tsList = new ArrayList<>();
        tsList.add(TimeStepDO.builder().t(0).action(3).build());
        tsList.add(TimeStepDO.builder().t(1). build());
        episodeDOList.add(EpisodeDO.builder().timeSteps(tsList).build());

        s_in_Episodes_From_b_OK(bOk, episodeDOList);
        assertEquals(0, episodeDOList.get(0).getTimeStep(0).getS());
        assertEquals(1, episodeDOList.get(0).getTimeStep(1).getS());
        assertEquals(3, episodeDOList.get(0).getTimeStep(2).getS());

        assertEquals(0, episodeDOList.get(1).getTimeStep(0).getS());
        assertEquals(1, episodeDOList.get(1).getTimeStep(1).getS());


        boolean[][][] bOk2 =  b_OK_From_S_in_Episodes(episodeDOList);

        bOk = bOk_Expected_From_S;
        assertEquals(bOk.length, bOk2.length);
        assertEquals(bOk[0].length, bOk2[0].length);
        assertEquals(bOk[0][0].length, bOk2[0][0].length);
        for (int e = 0; e < bOk.length; e++) {
            for (int i = 0; i < bOk[e].length; i++) {
                assertArrayEquals(bOk[e][i], bOk2[e][i]);
            }
        }
    }


    @Test
    void bOKRoundtrip() {
        boolean[][][] bOk = {{
                {false, false, true},
                {false, true, false},
                {false, false, true}
        }, {
                {false, false },
                {false, true }
        }
        };
        boolean[][][] bOk_Expected_From_S = {{
                {false, false, false},
                {false, true, false},
                {false, false, true}
        }, {
                {false, false },
                {false, true }
        }
        };
        List<EpisodeDO > episodeDOList = new ArrayList<>();
        List<TimeStepDO > tsList = new ArrayList<>();
        tsList.add(TimeStepDO.builder().t(0).action(1).build());
        tsList.add(TimeStepDO.builder().t(1).action(2).build());
        tsList.add(TimeStepDO.builder().t(2).build());
        episodeDOList.add(EpisodeDO.builder().timeSteps(tsList).build());

        tsList = new ArrayList<>();
        tsList.add(TimeStepDO.builder().t(0).action(3).build());
        tsList.add(TimeStepDO.builder().t(1).build());
        episodeDOList.add(EpisodeDO.builder().timeSteps(tsList).build());

        s_in_Episodes_From_b_OK(bOk, episodeDOList);
        assertEquals(0, episodeDOList.get(0).getTimeStep(0).getS());
        assertEquals(1, episodeDOList.get(0).getTimeStep(1).getS());
        assertEquals(1, episodeDOList.get(0).getTimeStep(2).getS());

        assertEquals(0, episodeDOList.get(1).getTimeStep(0).getS());
        assertEquals(1, episodeDOList.get(1).getTimeStep(1).getS());


        boolean[][][] bOk2 =  b_OK_From_S_in_Episodes(episodeDOList);

        bOk = bOk_Expected_From_S;
        assertEquals(bOk.length, bOk2.length);
        assertEquals(bOk[0].length, bOk2[0].length);
        assertEquals(bOk[0][0].length, bOk2[0][0].length);
        for (int e = 0; e < bOk.length; e++) {
            for (int i = 0; i < bOk[e].length; i++) {
                assertArrayEquals(bOk[e][i], bOk2[e][i]);
            }
        }
    }



    @Test
    void unrollStepsA() {
        boolean[][][] input = {{
                {false, false, false},
                {false, false, false},
                {false, false, false}
        }, {
                {false, false },
                {false, false }
        }
        };
        assertEquals(0, ZipperFunctions.unrollSteps(input));
    }

    @Test
    void unrollStepsB() {
        boolean[][][] input = {{
                {true, false, true},
                {false, true, false},
                {false, false, true}
        }, {
                {false, false },
                {false, false }
        }
        };
        assertEquals(0, ZipperFunctions.unrollSteps(input));
    }
    @Test
    void unrollStepsC() {
        boolean[][][] input = {{
                {true, false, true},
                {false, true, false},
                {false, false, true}
        }, {
                {true, false },
                {false, true }
        }
        };
        assertEquals(1, ZipperFunctions.unrollSteps(input));
    }
    @Test
    void unrollStepsD() {
        boolean[][][] input = {{
                {true, true, true},
                {false, true, true},
                {false, false, true}
        }, {
                {true, true },
                {false, true }
        }
        };
        assertEquals(3, ZipperFunctions.unrollSteps(input));
    }

    @Test
    void determineUnrollSteps() {

        boolean[][][] trainingNeeded = {{
                {true, true, false},
                {false, true, true},
                {false, false, true}
        }, {
                {true, false},
                {false, true}
        }
        };

        int k = 2;  // going for row 0 in the first game, and for the non existing row -1 in the second game
        int[] expected = {1, -1};
        int[] actual = ZipperFunctions.determineUnrollSteps(trainingNeeded,  k);
        assertArrayEquals(expected, actual);

        k = 1;  // going for row 1 in the first game, and for row 0 in the second game
        expected = new int[] {1, 0};
        actual = ZipperFunctions.determineUnrollSteps(trainingNeeded,  k);
        assertArrayEquals(expected, actual);

        k = 0;  // going for row 2 in the first game, and for row 1 in the second game
        expected = new int[] {0, 0};
        actual = ZipperFunctions.determineUnrollSteps(trainingNeeded,  k);
        assertArrayEquals(expected, actual);
    }


    @Test
    void trainingNeededTest() {
        boolean[][][] input = {{
                {true, false, true},
                {false, true, false},
                {false, false, true}
        }, {
                {false, false },
                {false, false }
        }
        };
        boolean[][][] expected = {{
                {true, true, false},
                {false, true, true},
                {false, false, true}
        }, {
                {true, false },
                {false, true }
        }
        };
        boolean[][][] output = ZipperFunctions.trainingNeeded(input);
        assertArrayEquals(expected[0], output[0]);
        assertArrayEquals(expected[1], output[1]);
    }

    @Test
    void trainingNeededFloatTest() {
        boolean[][][] input = {{
                {true, false, true},
                {false, true, false},
                {false, false, true}
        }, {
                {false, false },
                {false, false }
        }
        };
        float[][][] expected = {{
                {0.1f, 1f, 0f},
                {0f, 0.1f, 1f},
                {0f, 0f, 0.1f}
        }, {
                {1f, 0f },
                {0f, 1f }
        }
        };
        float[][][] output = ZipperFunctions.trainingNeededFloat(input, 0.1f, false);
        assertArrayEquals(expected[0], output[0]);
        assertArrayEquals(expected[1], output[1]);
    }
    @Test
    void trainingNeededTestA() {
        boolean[][][] input = {{
                {true, true, true},
                {false, true, true},
                {false, false, true}
        }};
        boolean[][][] expected = {{
                {true, true, true},
                {false, true, true},
                {false, false, true}
        }};
        boolean[][][] output = ZipperFunctions.trainingNeeded(input);
        assertArrayEquals(expected[0], output[0]);
    }

    @Test
    void trainingNeededTestB() {
        boolean[][][] input = {{
                {false, false, false},
                {false, false, false},
                {false, false, false}
        }};
        boolean[][][] expected = {{
                {true, false, false},
                {false, true, false},
                {false, false, true}
        }};
        boolean[][][] output = ZipperFunctions.trainingNeeded(input);
        assertArrayEquals(expected[0], output[0]);
    }
    @Test
    void trainingNeededTestC() {
        boolean[][][] input = {{
                {false, false, false},
                {false, false, false},
                {false, false, true}
        }};
        boolean[][][] expected = {{
                {true, false, false},
                {false, true, true},
                {false, false, true}
        }};
        boolean[][][] output = ZipperFunctions.trainingNeeded(input);
        assertArrayEquals(expected[0], output[0]);
    }

    @Test
    void trainingNeededTestD() {
        boolean[][][] input = {{
                {false, false, true},
                {false, false, false},
                {false, false, true}
        }};
        boolean[][][] expected = {{
                {true, false, false},
                {false, true, true},
                {false, false, true}
        }};
        boolean[][][] output = ZipperFunctions.trainingNeeded(input);
        assertArrayEquals(expected[0], output[0]);
    }

    @Test
    void trainingNeededTestE() {
        boolean[][][] input = {{
                {false, true, true},
                {false, true, false},
                {false, false, true}
        }};
        boolean[][][] expected = {{
                {true, true, false},
                {false, true, true},
                {false, false, true}
        }};
        boolean[][][] output = ZipperFunctions.trainingNeeded(input);
        assertArrayEquals(expected[0], output[0]);
    }

    @Test
    void sortedAndFilteredIndicesA() {
        int[] input = {3, 1, 2};
        int[] expected = {1, 2, 0};
        int[] actual = ZipperFunctions.sortedAndFilteredIndices(input);
        assertArrayEquals(expected, actual);
    }

    @Test
    void sortedAndFilteredIndicesB() {
        int[] input = {3, -1, -1, 1, 2};
        int[] expected = {3, 4, 0};
        int[] actual = ZipperFunctions.sortedAndFilteredIndices(input);
        assertArrayEquals(expected, actual);
    }


//    @Test
//    void zipperClosedA() {
//        // bOK[from][to] = true for a particular game if the error is small enough from t = from and tau = from-to
//        // only to >= from are relevant
//        // to are the columns
//        // from are the rows
//        boolean[][] bOK = {{true, true, true},
//                {false, true, true},
//                {false, false, true}};
//        // only the upper triangle is relevant
//        // the diagonal is the case where tau = 0
//        // suppose the diagonal is already learned but nothing else
//        bOK = new boolean[][]{{true, false, false},
//                {false, true, false},
//                {false, false, true}};
//
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 0, 0));
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 1, 1));
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 2, 2));
//
//        assertFalse(ZipperFunctions.zipperClosed(bOK, 0, 1));
//        assertFalse(ZipperFunctions.zipperClosed(bOK, 1, 2));
//
//        assertFalse(ZipperFunctions.zipperClosed(bOK, 0, 2));
//    }
//    @Test
//    void zipperClosedB() {
//
//        boolean[][] bOK =  {{true, true, false},
//                              {false, true, true},
//                              {false, false, true}};
//
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 0, 0));
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 1, 1));
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 2, 2));
//
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 0, 1));
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 1, 2));
//
//        assertFalse(ZipperFunctions.zipperClosed(bOK, 0, 2));
//    }
//    @Test
//    void zipperClosedC() {
//
//        boolean[][] bOK =  {{true, true, true},
//                {false, true, true},
//                {false, false, true}};
//
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 0, 0));
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 1, 1));
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 2, 2));
//
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 0, 1));
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 1, 2));
//
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 0, 2));
//    }
//    @Test
//    void zipperClosedD() {
//
//        boolean[][] bOK =  {{true, true, true},
//                              {false, true, false},
//                              {false, false, true}};
//
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 0, 0));
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 1, 1));
//
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 2, 2));
//        assertFalse(ZipperFunctions.zipperClosed(bOK, 1, 2));
//        assertFalse(ZipperFunctions.zipperClosed(bOK, 0, 2));
//
//        assertTrue(ZipperFunctions.zipperClosed(bOK, 0, 1));
//
//
//
//    }
}
