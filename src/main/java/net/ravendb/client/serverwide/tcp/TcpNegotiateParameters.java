package net.ravendb.client.serverwide.tcp;

import java.net.Socket;
import java.util.function.BiFunction;
import java.util.function.Function;

public class TcpNegotiateParameters {

    private TcpConnectionHeaderMessage.OperationTypes operation;
    private TcpConnectionHeaderMessage.AuthorizationInfo authorizeInfo;
    private int version;
    private String database;
    private String sourceNodeTag;
    private String destinationNodeTag;
    private String destinationUrl;
    private String destinationServerId;

    private BiFunction<String, Socket, Integer> readResponseAndGetVersionCallback;

    public TcpConnectionHeaderMessage.OperationTypes getOperation() {
        return operation;
    }

    public void setOperation(TcpConnectionHeaderMessage.OperationTypes operation) {
        this.operation = operation;
    }

    public TcpConnectionHeaderMessage.AuthorizationInfo getAuthorizeInfo() {
        return authorizeInfo;
    }

    public void setAuthorizeInfo(TcpConnectionHeaderMessage.AuthorizationInfo authorizeInfo) {
        this.authorizeInfo = authorizeInfo;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSourceNodeTag() {
        return sourceNodeTag;
    }

    public void setSourceNodeTag(String sourceNodeTag) {
        this.sourceNodeTag = sourceNodeTag;
    }

    public String getDestinationNodeTag() {
        return destinationNodeTag;
    }

    public void setDestinationNodeTag(String destinationNodeTag) {
        this.destinationNodeTag = destinationNodeTag;
    }

    public String getDestinationUrl() {
        return destinationUrl;
    }

    public void setDestinationUrl(String destinationUrl) {
        this.destinationUrl = destinationUrl;
    }

    public String getDestinationServerId() {
        return destinationServerId;
    }

    public void setDestinationServerId(String destinationServerId) {
        this.destinationServerId = destinationServerId;
    }

    public BiFunction<String, Socket, Integer> getReadResponseAndGetVersionCallback() {
        return readResponseAndGetVersionCallback;
    }

    public void setReadResponseAndGetVersionCallback(BiFunction<String, Socket, Integer> readResponseAndGetVersionCallback) {
        this.readResponseAndGetVersionCallback = readResponseAndGetVersionCallback;
    }
}
