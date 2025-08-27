package net.ravendb.client.documents.session;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;

/**
 * Interface for vector embedding field factory accessor
 * @param <T> The type of the field
 */
public interface IVectorEmbeddingFieldFactoryAccessor<T> extends IVectorField{
    /**
     * Gets the field name
     * @return The field name
     */
    T getFieldName();

    /**
     * Gets the source quantization type
     * @return The source quantization type
     */
    VectorEmbeddingType getSourceQuantizationType();

    /**
     * Gets the destination quantization type
     * @return The destination quantization type
     */
    VectorEmbeddingType getDestinationQuantizationType();

    /**
     * Gets whether the embedding is base64 encoded
     * @return Whether the embedding is base64 encoded
     */
    boolean isBase64Encoded();

    /**
     * Gets the embeddings generation task identifier
     * @return The embeddings generation task identifier
     */
    String getEmbeddingsGenerationTaskIdentifier();
}