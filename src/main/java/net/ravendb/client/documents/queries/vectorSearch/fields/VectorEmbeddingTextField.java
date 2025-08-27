package net.ravendb.client.documents.queries.vectorSearch.fields;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;
import net.ravendb.client.documents.queries.vectorSearch.common.VectorFieldBase;
import net.ravendb.client.documents.session.IVectorEmbeddingFieldFactoryAccessor;
import net.ravendb.client.documents.session.IVectorEmbeddingTextField;

/**
 * Vector embedding text field implementation
 * @param <T> The type of the field
 */
public class VectorEmbeddingTextField<T> extends VectorField implements IVectorEmbeddingTextField {
        /**
     * Creates a new instance of VectorEmbeddingTextField
     * @param fieldName The field name
     */
    public VectorEmbeddingTextField(T fieldName) {
        super(fieldName);
        this.setDestinationQuantizationType(VectorEmbeddingType.SINGLE);
        this.setSourceQuantizationType(VectorEmbeddingType.TEXT);
        this.setIsBase64Encoded(false);
        this.setEmbeddingsGenerationTaskIdentifier("");
    }

    @Override
    public IVectorEmbeddingTextField targetQuantization(VectorEmbeddingType targetEmbeddingQuantization) {
        if (targetEmbeddingQuantization == VectorEmbeddingType.TEXT) {
            throw new IllegalArgumentException("Cannot quantize the embedding to Text. This option is only available for sourceQuantizationType.");
        }

        this.setDestinationQuantizationType(targetEmbeddingQuantization);
        return this;
    }

    @Override
    public IVectorEmbeddingTextField usingTask(String embeddingsGenerationTaskIdentifier) {
        if (this.getSourceQuantizationType() != VectorEmbeddingType.TEXT) {
            throw new IllegalArgumentException("The usingTask method can only be used with text embeddings (withText)");
        }

        this.setEmbeddingsGenerationTaskIdentifier(embeddingsGenerationTaskIdentifier);
        return this;
    }
}