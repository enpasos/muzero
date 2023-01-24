package ai.enpasos.muzero.platform.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ValueConverterTest {

    @Test
    void valueToClassIndex() {
        int[] confValues = {-1, 0, 1};
        assertEquals(0, ValueConverter.valueToClassIndex(confValues, -1));
        assertEquals(1, ValueConverter.valueToClassIndex(confValues, 0));
        assertEquals(2, ValueConverter.valueToClassIndex(confValues, 1));
    }


}
