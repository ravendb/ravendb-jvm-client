package net.ravendb.client.documents.queries.vectorSearch.fields;

import net.ravendb.client.documents.queries.vectorSearch.VectorEmbeddingType;
import net.ravendb.client.documents.session.IVectorEmbeddingFieldFactoryAccessor;
import net.ravendb.client.documents.session.IVectorField;

/**
 * Vector field implementation
 * @param <T> The type of the field
 */
public class VectorField<T> implements IVectorField, IVectorEmbeddingFieldFactoryAccessor {
    private String fieldName;
    private VectorEmbeddingType sourceQuantizationType;
    private VectorEmbeddingType destinationQuantizationType;
    private boolean isBase64Encoded;
    private String embeddingsGenerationTaskIdentifier = "";

    public VectorField(T fieldName) {
        this.fieldName = fieldName.toString();
    }

    public VectorField(T fieldName, VectorEmbeddingType sourceQuantizationType, VectorEmbeddingType destinationQuantizationType, String embeddingsGenerationTaskIdentifier) {
        this.fieldName = fieldName.toString();
        this.sourceQuantizationType = sourceQuantizationType;
        this.destinationQuantizationType = destinationQuantizationType;
        this.embeddingsGenerationTaskIdentifier = embeddingsGenerationTaskIdentifier;
    }

    public void setSourceQuantizationType (VectorEmbeddingType sourceQuantizationType) {
        this.sourceQuantizationType = sourceQuantizationType;
    }

    @Override
    public VectorEmbeddingType getSourceQuantizationType () {
        return this.sourceQuantizationType;
    }

    public void setDestinationQuantizationType (VectorEmbeddingType destinationQuantizationType) {
        this.destinationQuantizationType = destinationQuantizationType;
    }

    @Override
    public VectorEmbeddingType getDestinationQuantizationType () {
        return this.destinationQuantizationType;
    }

    public void setIsBase64Encoded (boolean isBase64Encoded) {
        this.isBase64Encoded = isBase64Encoded;
    }

    @Override
    public boolean isBase64Encoded () {
        return this.isBase64Encoded;
    }

    @Override
    public String getFieldName() {
        return this.fieldName;
    }

    @Override
    public String getEmbeddingsGenerationTaskIdentifier() {
        return this.embeddingsGenerationTaskIdentifier;
    }

    public void setEmbeddingsGenerationTaskIdentifier(String embeddingsGenerationTaskIdentifier) {
        this.embeddingsGenerationTaskIdentifier = embeddingsGenerationTaskIdentifier;
    }
}