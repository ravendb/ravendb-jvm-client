package net.ravendb.client.documents.operations.replication;

import net.ravendb.client.documents.operations.ongoingTasks.OngoingTaskPullReplicationAsHub;

import java.util.List;

public class PullReplicationDefinitionAndCurrentConnections {

    private PullReplicationDefinition definition;
    private List<OngoingTaskPullReplicationAsHub> ongoingTasks;

    public PullReplicationDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(PullReplicationDefinition definition) {
        this.definition = definition;
    }

    public List<OngoingTaskPullReplicationAsHub> getOngoingTasks() {
        return ongoingTasks;
    }

    public void setOngoingTasks(List<OngoingTaskPullReplicationAsHub> ongoingTasks) {
        this.ongoingTasks = ongoingTasks;
    }
}
