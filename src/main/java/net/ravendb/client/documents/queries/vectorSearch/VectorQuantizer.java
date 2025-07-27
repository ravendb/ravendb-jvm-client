package net.ravendb.client.documents.queries.vectorSearch;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Utility class for quantizing vectors
 */
public class VectorQuantizer {
    /**
     * Converts a float array to an int8 array.
     * Finds the maximum absolute value and scales all values to fit in int8 range (-127 to 127).
     * Appends the maximum absolute value as a float at the end.
     *
     * @param rawEmbedding The float array to convert
     * @return A new array with the quantized values
     */
    public static int[] toInt8(float[] rawEmbedding) {
        int length = rawEmbedding.length;
        int[] result = new int[length + 4]; // +4 for the float at the end

        float maxAbsValue = 0;
        for (int i = 0; i < length; i++) {
            maxAbsValue = Math.max(maxAbsValue, Math.abs(rawEmbedding[i]));
        }

        float scaleFactor = maxAbsValue == 0 ? 1 : 127 / maxAbsValue;

        for (int i = 0; i < length; i++) {
            result[i] = Math.round(rawEmbedding[i] * scaleFactor);
        }

        // Convert the maxAbsValue float to bytes and append to the result
        ByteBuffer buffer = ByteBuffer.allocate(4);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putFloat(maxAbsValue);
        byte[] bytes = buffer.array();
        
        for (int i = 0; i < 4; i++) {
            result[length + i] = bytes[i];
        }

        return result;
    }

    /**
     * Converts a float array to a binary representation where each value is represented by 1 bit.
     * 1 if the value is non-negative, 0 if negative. Packs 8 values per byte.
     *
     * @param rawEmbedding The float array to convert
     * @return A new array with the binary-packed values
     */
    public static int[] toInt1(float[] rawEmbedding) {
        int length = rawEmbedding.length;
        int outputLength = (int) Math.ceil(length / 8.0);
        int[] result = new int[outputLength];

        for (int i = 0; i < length; i++) {
            int byteIndex = i / 8;
            int bitPosition = 7 - (i % 8);

            if (rawEmbedding[i] >= 0) {
                result[byteIndex] |= (1 << bitPosition);
            }
        }

        return result;
    }
}