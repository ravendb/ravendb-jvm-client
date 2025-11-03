package net.ravendb.client.documents.operations.AI;

import java.util.List;

/**
 * Base class for all AI provider settings.
 */
public abstract class AbstractAiSettings {

    /**
     * Maximum number of query embedding batches that can be processed concurrently.
     * Allows users to override the database global value.
     */
    private Integer embeddingsMaxConcurrentBatches;

    public Integer getEmbeddingsMaxConcurrentBatches() {
        return embeddingsMaxConcurrentBatches;
    }

    public void setEmbeddingsMaxConcurrentBatches(Integer embeddingsMaxConcurrentBatches) {
        this.embeddingsMaxConcurrentBatches = embeddingsMaxConcurrentBatches;
    }

    /**
     * Validates the settings fields and adds any errors to the provided list.
     *
     * @param errors List to collect validation error messages.
     */
    public abstract void validate(List<String> errors);

    /**
     * Compares this settings instance with another to detect differences.
     *
     * @param other The other settings instance to compare with.
     * @return Flags indicating which settings differ.
     */
    public abstract AiSettingsCompareDifferences compare(AbstractAiSettings other);
}
