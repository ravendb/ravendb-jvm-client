package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;

/**
 * Interface for vector embedding field
 */
public interface IVectorEmbeddingField {
    /**
     * Sets the target quantization
     * @param targetEmbeddingQuantization The target embedding quantization
     * @return The vector embedding field
     */
    IVectorEmbeddingField targetQuantization(VectorEmbeddingType targetEmbeddingQuantization);
}