package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.AI.EmbeddingsGenerationConfiguration;

public final class OngoingTaskEmbeddingsGeneration extends OngoingTask {

    private String connectionStringName;
    private EmbeddingsGenerationConfiguration configuration;

    public OngoingTaskEmbeddingsGeneration() {
        setTaskType(OngoingTaskType.EMBEDDINGS_GENERATION);
    }

    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    public EmbeddingsGenerationConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(EmbeddingsGenerationConfiguration configuration) {
        this.configuration = configuration;
    }
}
