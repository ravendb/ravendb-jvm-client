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
     IVectorEmbeddingTextField withText(T fieldName);

    /**
     * Creates a vector field from embedding
     * @param fieldName The field name
     * @param storedEmbeddingQuantization The stored embedding quantization (optional)
     * @return The vector embedding field
     */
     IVectorEmbeddingField withEmbedding(T fieldName, VectorEmbeddingType storedEmbeddingQuantization);

    /**
     * Creates a vector field from base64 encoded embedding
     * @param fieldName The field name
     * @param storedEmbeddingQuantization The stored embedding quantization (optional)
     * @return The vector embedding field
     */
      IVectorEmbeddingField withBase64(T fieldName, VectorEmbeddingType storedEmbeddingQuantization);

    /**
     * Creates a vector field
     * @param fieldName The field name
     * @return The vector field
     */
       IVectorField withField(T fieldName);
}
