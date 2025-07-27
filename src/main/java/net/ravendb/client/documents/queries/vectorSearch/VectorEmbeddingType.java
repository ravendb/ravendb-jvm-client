package net.ravendb.client.documents.queries.vectorSearch;

/**
 * Represents the type of vector embedding.
 */
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