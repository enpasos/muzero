package ai.enpasos.muzero.platform.agent.d_model.djl;
import ai.djl.ndarray.*;
import ai.djl.engine.Engine;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import static ai.enpasos.muzero.platform.agent.d_model.djl.SomeSerialization.*;
import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;



class NDListSerializationTest {


    private static final Logger logger = Logger.getLogger(NDListSerializationTest.class.getName());

    private NDManager manager;
    private Path tempFile;

    @BeforeEach
    void setUp() throws IOException {
        manager = Engine.getInstance().newBaseManager();
        tempFile = Files.createTempFile("ndlist", ".dat");
        logger.log(Level.INFO, "Temporary file created: " + tempFile.toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        if (manager != null) {
            manager.close();
            logger.log(Level.INFO, "NDManager closed.");
        }
        Files.deleteIfExists(tempFile);
        logger.log(Level.INFO, "Temporary file deleted: " + tempFile.toString());
    }

    @Test
    void testNDListSerialization() {
        // Construct an NDList
        NDArray array1 = manager.create(new float[]{1.0f, 2.0f, 3.0f});
        NDArray array2 = manager.create(new float[][]{{1.0f, 2.0f}, {3.0f, 4.0f}});
        NDList originalList = new NDList(array1, array2);
        logger.log(Level.INFO, "Original NDList created.");

        // Write to file
        saveNDList(originalList, tempFile.toString());
        logger.log(Level.INFO, "NDList saved to file.");

        // Read from file
        NDList loadedList = loadNDList(tempFile.toString(), manager);
        logger.log(Level.INFO, "NDList loaded from file.");

        // Assert equality
        assertEquals(originalList.size(), loadedList.size(), "Size of NDLists must be the same");
        for (int i = 0; i < originalList.size(); i++) {
            NDArray original = originalList.get(i);
            NDArray loaded = loadedList.get(i);
            assertTrue(original.contentEquals(loaded), "NDArrays must be equal at index " + i);
        }
        logger.log(Level.INFO, "NDList serialization test passed.");
    }

    @Test
    void testEmptyNDListSerialization() {
        NDList emptyList = new NDList();
        logger.log(Level.INFO, "Empty NDList created.");

        // Write to file
        saveNDList(emptyList, tempFile.toString());
        logger.log(Level.INFO, "Empty NDList saved to file.");

        // Read from file
        NDList loadedList = loadNDList(tempFile.toString(), manager);
        logger.log(Level.INFO, "Empty NDList loaded from file.");

        // Assert equality
        assertEquals(emptyList.size(), loadedList.size(), "Size of empty NDLists must be the same");
        logger.log(Level.INFO, "Empty NDList serialization test passed.");
    }

    @Test
    void testSingleElementNDListSerialization() {
        NDArray array = manager.create(new float[]{42.0f});
        NDList singleList = new NDList(array);
        logger.log(Level.INFO, "Single element NDList created.");

        // Write to file
        saveNDList(singleList, tempFile.toString());
        logger.log(Level.INFO, "Single element NDList saved to file.");

        // Read from file
        NDList loadedList = loadNDList(tempFile.toString(), manager);
        logger.log(Level.INFO, "Single element NDList loaded from file.");

        // Assert equality
        assertEquals(singleList.size(), loadedList.size(), "Size of single element NDLists must be the same");
        assertTrue(singleList.get(0).contentEquals(loadedList.get(0)), "Single element NDArrays must be equal");
        logger.log(Level.INFO, "Single element NDList serialization test passed.");
    }


    @Test
    void testIntArray()  throws IOException {

        int[] expectedData = new int[]{1, 5, 9, 2};
        Path file = Files.createTempFile("intArray", ".dat");
         saveIntArray(expectedData, file.toString());
        int[] loadedData = loadIntArray(file.toString());

        assertArrayEquals(expectedData, loadedData);
    }


    @Test
    void testBooleanArray() throws IOException  {

        boolean[][][] expectedData = new boolean[][][]{
                {{true, false}, {false, true}},
                {{true, true, true}, {false, false, false}, {true, true, true}}
        };

        Path file = Files.createTempFile("booleanArray", ".dat");
        saveBooleanArray(expectedData, file.toString());
        boolean[][][] loadedData = loadBooleanArray(file.toString());

        assertArrayEquals(expectedData, loadedData);
    }
}
