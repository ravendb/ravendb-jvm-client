package net.ravendb.client.documents.queries.vectorSearch.fields;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;
import net.ravendb.client.documents.queries.vectorSearch.common.VectorFieldBase;
import net.ravendb.client.documents.session.IVectorEmbeddingField;
import net.ravendb.client.documents.session.IVectorEmbeddingFieldFactoryAccessor;

/**
 * Vector embedding field implementation
 * @param <T> The type of the field
 */
public class VectorEmbeddingField<T> extends VectorFieldBase<T> implements 
        IVectorEmbeddingField, 
        IVectorEmbeddingFieldFactoryAccessor<T> {

    private VectorEmbeddingType sourceQuantizationType;
    private VectorEmbeddingType destinationQuantizationType;
    private boolean isBase64Encoded;
    private String embeddingsGenerationTaskIdentifier = "";

    /**
     * Creates a new instance of VectorEmbeddingField
     * @param fieldName The field name
     * @param sourceQuantizationType The source quantization type (default: SINGLE)
     * @param isBase64Encoded Whether the embedding is base64 encoded (default: false)
     */
    public VectorEmbeddingField(T fieldName, 
                               VectorEmbeddingType sourceQuantizationType, 
                               boolean isBase64Encoded) {
        super(fieldName);
        this.sourceQuantizationType = sourceQuantizationType != null ? sourceQuantizationType : VectorEmbeddingType.SINGLE;
        this.destinationQuantizationType = this.sourceQuantizationType;
        this.isBase64Encoded = isBase64Encoded;
        updateFieldName();
    }

    /**
     * Creates a new instance of VectorEmbeddingField with default values
     * @param fieldName The field name
     */
    public VectorEmbeddingField(T fieldName) {
        this(fieldName, VectorEmbeddingType.SINGLE, false);
    }

    private void updateFieldName() {
        setFieldName(getFormattedFieldName(
                getRawFieldName(),
                sourceQuantizationType,
                destinationQuantizationType
        ));
    }

    @Override
    public IVectorEmbeddingField targetQuantization(VectorEmbeddingType targetEmbeddingQuantization) {
        if (targetEmbeddingQuantization == VectorEmbeddingType.TEXT) {
            throw new IllegalArgumentException("Cannot quantize the embedding to Text. This option is only available for sourceQuantizationType.");
        }

        this.destinationQuantizationType = targetEmbeddingQuantization;

        if ((this.sourceQuantizationType == VectorEmbeddingType.INT8 ||
             this.sourceQuantizationType == VectorEmbeddingType.BINARY) &&
             this.destinationQuantizationType != this.sourceQuantizationType) {
            throw new IllegalArgumentException(
                    String.format("Cannot quantize already quantized embeddings. Source VectorEmbeddingType is %s; however the destination is %s.",
                            this.sourceQuantizationType, this.destinationQuantizationType));
        }

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