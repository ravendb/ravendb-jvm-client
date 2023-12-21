package net.ravendb.client.documents.operations.backups.sharding;

public class SingleShardRestoreSetting {
    private int shardNumber;
    private String nodeTag;
    private String folderName;
    private String lastFileNameToRestore;

    public int getShardNumber() {
        return shardNumber;
    }

    public void setShardNumber(int shardNumber) {
        this.shardNumber = shardNumber;
    }

    public String getNodeTag() {
        return nodeTag;
    }

    public void setNodeTag(String nodeTag) {
        this.nodeTag = nodeTag;
    }

    public String getFolderName() {
        return folderName;
    }

    public void setFolderName(String folderName) {
        this.folderName = folderName;
    }

    public String getLastFileNameToRestore() {
        return lastFileNameToRestore;
    }

    public void setLastFileNameToRestore(String lastFileNameToRestore) {
        this.lastFileNameToRestore = lastFileNameToRestore;
    }
}
