package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.operations.etl.queue.QueueEtlConfiguration;

public class OngoingTaskQueueEtlDetails extends OngoingTask {

    private QueueEtlConfiguration configuration;

    public QueueEtlConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(QueueEtlConfiguration configuration) {
        this.configuration = configuration;
    }
}
