package net.ravendb.client.documents.queries.vectorSearch.fields;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;
import net.ravendb.client.documents.session.IVectorEmbeddingField;
import net.ravendb.client.documents.session.IVectorEmbeddingFieldFactoryAccessor;

/**
 * Vector embedding field implementation
 * @param <T> The type of the field
 */
public class VectorEmbeddingField<T> extends VectorField implements
        IVectorEmbeddingField{

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
        this.setSourceQuantizationType(sourceQuantizationType != null ? sourceQuantizationType : VectorEmbeddingType.SINGLE);
        this.setDestinationQuantizationType(this.getSourceQuantizationType());
        this.setIsBase64Encoded(isBase64Encoded);
    }

    /**
     * Creates a new instance of VectorEmbeddingField with default values
     * @param fieldName The field name
     */
    public VectorEmbeddingField(T fieldName) {
        this(fieldName, VectorEmbeddingType.SINGLE, false);
    }

    @Override
    public IVectorEmbeddingField targetQuantization(VectorEmbeddingType targetEmbeddingQuantization) {
        if (targetEmbeddingQuantization == VectorEmbeddingType.TEXT) {
            throw new IllegalArgumentException("Cannot quantize the embedding to Text. This option is only available for sourceQuantizationType.");
        }

        this.setDestinationQuantizationType(targetEmbeddingQuantization);

        if ((this.getSourceQuantizationType() == VectorEmbeddingType.INT8 ||
             this.getSourceQuantizationType() == VectorEmbeddingType.BINARY) &&
             this.getDestinationQuantizationType() != this.getSourceQuantizationType()) {
            throw new IllegalArgumentException(
                    String.format("Cannot quantize already quantized embeddings. Source VectorEmbeddingType is %s; however the destination is %s.",
                            this.getSourceQuantizationType(), this.getDestinationQuantizationType()));
        }
        return this;
    }
}