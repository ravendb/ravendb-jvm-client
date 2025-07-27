package net.ravendb.client.documents.queries.vectorSearch.fields;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;
import net.ravendb.client.documents.queries.vectorSearch.common.VectorFieldBase;
import net.ravendb.client.documents.session.IVectorEmbeddingFieldFactoryAccessor;
import net.ravendb.client.documents.session.IVectorEmbeddingTextField;

/**
 * Vector embedding text field implementation
 * @param <T> The type of the field
 */
public class VectorEmbeddingTextField<T> extends VectorFieldBase<T> implements 
        IVectorEmbeddingTextField, 
        IVectorEmbeddingFieldFactoryAccessor<T> {

    private VectorEmbeddingType sourceQuantizationType = VectorEmbeddingType.TEXT;
    private VectorEmbeddingType destinationQuantizationType = VectorEmbeddingType.SINGLE;
    private boolean isBase64Encoded = false;
    private String embeddingsGenerationTaskIdentifier = "";

    /**
     * Creates a new instance of VectorEmbeddingTextField
     * @param fieldName The field name
     */
    public VectorEmbeddingTextField(T fieldName) {
        super(fieldName);
        updateFieldName();
    }

    private void updateFieldName() {
        setFieldName(getFormattedFieldName(
                getRawFieldName(),
                sourceQuantizationType,
                destinationQuantizationType,
                embeddingsGenerationTaskIdentifier
        ));
    }

    @Override
    public IVectorEmbeddingTextField targetQuantization(VectorEmbeddingType targetEmbeddingQuantization) {
        if (targetEmbeddingQuantization == VectorEmbeddingType.TEXT) {
            throw new IllegalArgumentException("Cannot quantize the embedding to Text. This option is only available for sourceQuantizationType.");
        }

        this.destinationQuantizationType = targetEmbeddingQuantization;
        updateFieldName();
        return this;
    }

    @Override
    public IVectorEmbeddingTextField usingTask(String embeddingsGenerationTaskIdentifier) {
        if (this.sourceQuantizationType != VectorEmbeddingType.TEXT) {
            throw new IllegalArgumentException("The usingTask method can only be used with text embeddings (withText)");
        }

        this.embeddingsGenerationTaskIdentifier = embeddingsGenerationTaskIdentifier;
        updateFieldName();
        return this;
    }

    @Override
    public VectorEmbeddingType getSourceQuantizationType() {
        return sourceQuantizationType;
    }

    @Override
    public VectorEmbeddingType getDestinationQuantizationType() {
        return destinationQuantizationType;
    }

    @Override
    public boolean isBase64Encoded() {
        return isBase64Encoded;
    }

    @Override
    public String getEmbeddingsGenerationTaskIdentifier() {
        return embeddingsGenerationTaskIdentifier;
    }
}