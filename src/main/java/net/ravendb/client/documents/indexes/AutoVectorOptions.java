package net.ravendb.client.documents.indexes;

import java.util.Objects;

/**
 * Vector options of an auto-index field (C# Raven.Client.Documents.Indexes.Vector.AutoVectorOptions)
 */
public class AutoVectorOptions extends FieldVectorOptions {

    private String sourceFieldName;
    private String embeddingsGenerationTaskIdentifier;

    public AutoVectorOptions() {
    }

    public AutoVectorOptions(AutoVectorOptions options) {
        super(options);
        sourceFieldName = options.getSourceFieldName();
        embeddingsGenerationTaskIdentifier = options.getEmbeddingsGenerationTaskIdentifier();
    }

    public String getSourceFieldName() {
        return sourceFieldName;
    }

    public void setSourceFieldName(String sourceFieldName) {
        this.sourceFieldName = sourceFieldName;
    }

    public String getEmbeddingsGenerationTaskIdentifier() {
        return embeddingsGenerationTaskIdentifier;
    }

    public void setEmbeddingsGenerationTaskIdentifier(String embeddingsGenerationTaskIdentifier) {
        this.embeddingsGenerationTaskIdentifier = embeddingsGenerationTaskIdentifier;
    }

    @Override
    void validate() {
        super.validate();
        if (sourceFieldName == null) {
            throw new IllegalStateException("Source field name cannot be null.");
        }
        if (embeddingsGenerationTaskIdentifier == null) {
            throw new IllegalStateException("EmbeddingsGenerationTaskIdentifier cannot be null.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!super.equals(o)) return false;
        AutoVectorOptions that = (AutoVectorOptions) o;
        return Objects.equals(sourceFieldName, that.sourceFieldName)
                && Objects.equals(embeddingsGenerationTaskIdentifier, that.embeddingsGenerationTaskIdentifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), sourceFieldName, embeddingsGenerationTaskIdentifier);
    }
}
