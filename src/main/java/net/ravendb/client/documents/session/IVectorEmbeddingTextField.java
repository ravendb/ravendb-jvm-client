package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;

/**
 * Interface for vector embedding text field
 */
public interface IVectorEmbeddingTextField {
    /**
     * Sets the target quantization
     * @param targetEmbeddingQuantization The target embedding quantization
     * @return The vector embedding text field
     */
    IVectorEmbeddingTextField targetQuantization(VectorEmbeddingType targetEmbeddingQuantization);

    /**
     * Sets the task to use for generating embeddings
     * @param embeddingsGenerationTaskIdentifier The embeddings generation task identifier
     * @return The vector embedding text field
     */
    IVectorEmbeddingTextField usingTask(String embeddingsGenerationTaskIdentifier);
}