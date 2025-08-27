package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;

/**
 * Interface for vector field factory
 * @param <T> The type of the field
 */
public interface IVectorFieldFactory<T> {
    /**
     * Creates a vector field from text
     * @param fieldName The field name
     * @return The vector embedding text field
     */
     IVectorEmbeddingTextField withText(String fieldName);

    //TBD expr should add withText(Expression<Func<T, string>> fieldName);??

    /**
     * Creates a vector field from embedding
     * @param fieldName The field name
     * @param storedEmbeddingQuantization The stored embedding quantization (optional)
     * @return The vector embedding field
     */
     IVectorEmbeddingField withEmbedding(String fieldName, VectorEmbeddingType storedEmbeddingQuantization);

    /**
     * Creates a vector field from base64 encoded embedding
     * @param fieldName The field name
     * @param storedEmbeddingQuantization The stored embedding quantization (optional)
     * @return The vector embedding field
     */
      IVectorEmbeddingField withBase64(String fieldName, VectorEmbeddingType storedEmbeddingQuantization);

    /**
     * Creates a vector field
     * @param fieldName The field name
     * @param storedEmbeddingQuantization The stored embedding quantization (optional)
     * @param destinationEmbeddingQuantization The destination embedding quantization (optional)
     * @param embeddingsGenerationTaskIdentifier The embeddings generation task identifier (optional)
     * @return The vector field
     */
        IVectorField withField(T fieldName, VectorEmbeddingType storedEmbeddingQuantization, VectorEmbeddingType destinationEmbeddingQuantization, String embeddingsGenerationTaskIdentifier);

    /**
     * Creates a vector field
     * @param fieldName The field name
     * @return The vector field
     */
       IVectorField withField(String fieldName);
}
