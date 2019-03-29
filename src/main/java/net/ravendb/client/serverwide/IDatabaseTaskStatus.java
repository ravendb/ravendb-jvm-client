package net.ravendb.client.serverwide;

public interface IDatabaseTaskStatus {
    String getNodeTag();

    void setNodeTag(String nodeTag);
}
