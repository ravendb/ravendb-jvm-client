package net.ravendb.client.documents.queries.vectorSearch;

import net.ravendb.client.primitives.UseSharpEnum;

/**
 * Represents the type of vector embedding.
 */
@UseSharpEnum
public enum VectorEmbeddingType {
    /**
     * Single precision floating point (32-bit) vector
     */
    SINGLE,

    /**
     * 8-bit integer vector (quantized from floating point)
     */
    INT8,

    /**
     * Binary vector (1 bit per dimension)
     */
    BINARY,

    /**
     * Text that will be converted to vector embedding
     */
    TEXT
}