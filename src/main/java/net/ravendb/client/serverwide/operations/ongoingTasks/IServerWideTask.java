package net.ravendb.client.serverwide.operations.ongoingTasks;

public interface IServerWideTask {
    String[] getExcludedDatabases();
    void setExcludedDatabases(String[] databases);
}
