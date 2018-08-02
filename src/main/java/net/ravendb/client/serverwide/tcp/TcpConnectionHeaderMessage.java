package net.ravendb.client.serverwide.tcp;

import net.ravendb.client.primitives.UseSharpEnum;

public class TcpConnectionHeaderMessage {

    @UseSharpEnum
    public enum OperationTypes {
        NONE,
        DROP,
        SUBSCRIPTION,
        REPLICATION,
        CLUSTER,
        HEARTBEATS,
        PING,
        TEST_CONNECTION
    }

    private String databaseName;
    private String sourceNodeTag;
    private OperationTypes operation;
    private int operationVersion;
    private String info;

    public String getDatabaseName() {
        return databaseName;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    public String getSourceNodeTag() {
        return sourceNodeTag;
    }

    public void setSourceNodeTag(String sourceNodeTag) {
        this.sourceNodeTag = sourceNodeTag;
    }

    public OperationTypes getOperation() {
        return operation;
    }

    public void setOperation(OperationTypes operation) {
        this.operation = operation;
    }

    public int getOperationVersion() {
        return operationVersion;
    }

    public void setOperationVersion(int operationVersion) {
        this.operationVersion = operationVersion;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public static final int NUMBER_OR_RETRIES_FOR_SENDING_TCP_HEADER = 2;
    public static final int SUBSCRIPTION_TCP_VERSION = 40;
}
