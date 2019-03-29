package net.ravendb.client.documents.operations.ongoingTasks;

public class OngoingTaskSubscription extends OngoingTask {
    public OngoingTaskSubscription() {
        setTaskType(OngoingTaskType.SUBSCRIPTION);
    }

    private String query;

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
