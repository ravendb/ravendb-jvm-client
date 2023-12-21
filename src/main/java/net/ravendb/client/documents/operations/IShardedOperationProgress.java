package net.ravendb.client.documents.operations;

public interface IShardedOperationProgress extends IOperationProgress {
    int getShardNumber();

    void setShardNumber(int shardNumber);

    String getNodeTag();

    void setNodeTag(String nodeTag);

}
