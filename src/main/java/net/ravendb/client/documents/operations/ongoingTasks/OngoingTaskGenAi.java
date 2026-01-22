package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.AI.GenAiConfiguration;

public final class OngoingTaskGenAi extends OngoingTask {

    private String connectionStringName;
    private GenAiConfiguration configuration;
    private String changeVector;

    public OngoingTaskGenAi() {
        setTaskType(OngoingTaskType.GEN_AI);
    }

    public String getConnectionStringName() {
        return connectionStringName;
    }

    public void setConnectionStringName(String connectionStringName) {
        this.connectionStringName = connectionStringName;
    }

    public GenAiConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(GenAiConfiguration configuration) {
        this.configuration = configuration;
    }

    public String getChangeVector() {
        return changeVector;
    }

    public void setChangeVector(String changeVector) {
        this.changeVector = changeVector;
    }
}
