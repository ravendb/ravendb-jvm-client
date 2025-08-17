package net.ravendb.client.documents.queries.vectorSearch;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class VectorQuantizierTest {
    @Test
    public void shouldCorrectlyQuantizeVectorElementsAndStoreScaleFactor() {
        float[] input = {0.1f, 0.2f};
        int[] result = VectorQuantizer.toInt8(input);
        int[] expected = {64, 127, -51, -52, 76, 62};

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i]);
        }
    }

    @Test
    public void shouldMaintainExpectedByteValuesInQuantizedOutput() {
        float[] input = {0.1f, 0.2f};
        int[] result = VectorQuantizer.toInt8(input);
        int[] expected = {64, 127, -51, -52, 76, 62};

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i]);
        }
    }

    @Test
    public void shouldHandleZeroVectorInputCorrectly() {
        float[] input = {0, 0, 0, 0};
        int[] result = VectorQuantizer.toInt8(input);
        int[] expected = new int[8];

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i]);
        }
    }

    @Test
    public void shouldCorrectlyQuantizeFloatsWithPositiveAndNegativeValues() {
        float[] input = {0.5f, -1.5f, 2.5f, -3.5f};
        int[] result = VectorQuantizer.toInt8(input);
        int[] expected = {18, -54, 91, -127, 0, 0, 96, 64};

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i]);
        }
    }

    @Test
    public void shouldCorrectlyConvertVectorToBinaryBitRepresentation() {
        float[] input = {1, -2, 3, -4, 5, -6, 7, -8, 9};
        int[] result = VectorQuantizer.toInt1(input);
        int[] expected = {0xAA, 0x80};

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i]);
        }
    }

    @Test
    public void shouldRepresentZeroValuesAsPositiveBits() {
        float[] input = {0, 0, 0, 0, 0, 0, 0, 0};
        int[] result = VectorQuantizer.toInt1(input);
        int[] expected = {0xFF};

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i]);
        }
    }

    @Test
    public void shouldProperlyPadVectorsNotDivisibleBy8() {
        float[] input = {1, 2, 3, 4, 5};
        int[] result = VectorQuantizer.toInt1(input);
        int[] expected = {0xF8};

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i]);
        }
    }

    @Test
    public void shouldCorrectlyConvertAlternatingSignsToBitPattern() {
        float[] input = {-1, 2, -3, 4, -5, 6, -7, 8};
        int[] result = VectorQuantizer.toInt1(input);
        int[] expected = {0x55};

        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], result[i]);
        }
    }
}
