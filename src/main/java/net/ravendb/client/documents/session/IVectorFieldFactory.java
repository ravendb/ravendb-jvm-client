package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;
import net.ravendb.client.util.SerializableFunction;

/**
 * Interface for vector field factory
 * @param <T> The type of the field
 */
public interface IVectorFieldFactory<T> extends IVectorField {
    /**
     * Creates a vector field from text
     * @param fieldName The field name
     * @return The vector embedding text field
     */
     IVectorEmbeddingTextField withText(String fieldName);

    /**
     * Creates a vector field from text
     * @param propertySelector The field selector
     * @return The vector embedding text field
     */
    IVectorEmbeddingTextField withText(SerializableFunction<T, ?> propertySelector);

    /**
     * Creates a vector field from embedding (stored as Single)
     * @param propertySelector The field selector
     * @return The vector embedding field
     */
    IVectorEmbeddingField withEmbedding(SerializableFunction<T, ?> propertySelector);

    /**
     * Creates a vector field from embedding
     * @param propertySelector The field name
     * @param storedEmbeddingQuantization The stored embedding quantization (optional)
     * @return The vector embedding field
     */
    IVectorEmbeddingField withEmbedding(SerializableFunction<T, ?> propertySelector, VectorEmbeddingType storedEmbeddingQuantization);

    /**
     * Creates a vector field from embedding
     * @param fieldName The field name
     * @param storedEmbeddingQuantization The stored embedding quantization (optional)
     * @return The vector embedding field
     */
    IVectorEmbeddingField withEmbedding(String fieldName, VectorEmbeddingType storedEmbeddingQuantization);

    /**
     * Creates a vector field from embedding
     * @param fieldName The field name
     * @return The vector embedding field
     */
    IVectorEmbeddingField withEmbedding(String fieldName);

    /**
     * Creates a vector field from base64 encoded embedding
     * @param fieldName The field name
     * @param storedEmbeddingQuantization The stored embedding quantization (optional)
     * @return The vector embedding field
     */
      IVectorEmbeddingField withBase64(String fieldName, VectorEmbeddingType storedEmbeddingQuantization);

    /**
     * Creates a vector field from base64 encoded embedding (stored as Single)
     * @param fieldName The field name
     * @return The vector embedding field
     */
    IVectorEmbeddingField withBase64(String fieldName);

    /**
     * Creates a vector field from base64 encoded embedding
     * @param propertySelector The field selector
     * @param storedEmbeddingQuantization The stored embedding quantization
     * @return The vector embedding field
     */
    IVectorEmbeddingField withBase64(SerializableFunction<T, ?> propertySelector, VectorEmbeddingType storedEmbeddingQuantization);

    /**
     * Creates a vector field from base64 encoded embedding (stored as Single)
     * @param propertySelector The field selector
     * @return The vector embedding field
     */
    IVectorEmbeddingField withBase64(SerializableFunction<T, ?> propertySelector);

    /**
     * Creates a vector field
     * @param fieldName The field name
     * @return The vector field
     */
       IVectorField withField(String fieldName);

    /**
     * Creates a vector field
     * @param propertySelector The field name
     * @return The vector field
     */
    IVectorField withField(SerializableFunction<T, ?> propertySelector);
}
