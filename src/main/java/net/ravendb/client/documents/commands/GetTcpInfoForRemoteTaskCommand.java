package net.ravendb.client.documents.commands;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.commands.TcpConnectionInfo;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.time.Duration;

public class GetTcpInfoForRemoteTaskCommand extends RavenCommand<TcpConnectionInfo> {

    private final String _remoteDatabase;
    private final String _remoteTask;
    private final String _tag;
    private boolean _verifyDatabase;
    private ServerNode _requestedNode;

    public GetTcpInfoForRemoteTaskCommand(String tag, String remoteDatabase, String remoteTask) {
        this(tag, remoteDatabase, remoteTask, false);
    }

    public GetTcpInfoForRemoteTaskCommand(String tag, String remoteDatabase, String remoteTask, boolean verifyDatabase) {
        super(TcpConnectionInfo.class);

        if (remoteDatabase == null) {
            throw new IllegalArgumentException("RemoteDatabase cannot be null");
        }

        _remoteDatabase = remoteDatabase;

        if (remoteTask == null) {
            throw new IllegalArgumentException("RemoteTask cannot be null");
        }

        _remoteTask = remoteTask;
        _tag = tag;
        _verifyDatabase = verifyDatabase;

        setTimeout(Duration.ofSeconds(15));
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/info/remote-task/tcp?" +
                "database=" + urlEncode(_remoteDatabase) +
                "&remote-task=" + urlEncode(_remoteTask) +
                "&tag=" + urlEncode(_tag);

        if (_verifyDatabase) {
            url.value += "&verify-database=true";
        }

        _requestedNode = node;

        return new HttpGet();
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            throwInvalidResponse();
        }

        result = mapper.readValue(response, TcpConnectionInfo.class);
    }

    public ServerNode getRequestedNode() {
        return _requestedNode;
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
