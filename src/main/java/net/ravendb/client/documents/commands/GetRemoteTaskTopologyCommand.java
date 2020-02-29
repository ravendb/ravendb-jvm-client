package net.ravendb.client.documents.commands;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetRemoteTaskTopologyCommand extends RavenCommand<String[]> {

    private final String _remoteDatabase;
    private final String _databaseGroupId;
    private final String _remoteTask;

    private ServerNode requestedNode;

    public GetRemoteTaskTopologyCommand(String remoteDatabase, String databaseGroupId, String remoteTask) {
        super(String[].class);

        if (remoteDatabase == null) {
            throw new IllegalArgumentException("RemoteDatabase cannot be null");
        }
        _remoteDatabase = remoteDatabase;

        if (databaseGroupId == null) {
            throw new IllegalArgumentException("DatabaseGroupId cannot be null");
        }

        _databaseGroupId = databaseGroupId;

        if (remoteTask == null) {
            throw new IllegalArgumentException("RemoteTask cannot be null");
        }

        _remoteTask = remoteTask;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/info/remote-task/topology?" +
                "database=" + urlEncode(_remoteDatabase) +
                "&remote=" + urlEncode(_remoteTask) +
                "&groupId=" + urlEncode(_databaseGroupId);

        return new HttpGet();
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            throwInvalidResponse();
        }

        result = mapper.readValue(response, String[].class);
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }

    public ServerNode getRequestedNode() {
        return requestedNode;
    }
}
