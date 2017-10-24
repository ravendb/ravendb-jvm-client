package net.ravendb.client.serverwide.commands;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetTcpInfoCommand extends RavenCommand<TcpConnectionInfo> {

    private final String tag;
    private final String dbName;
    private ServerNode requestedNode;

    public GetTcpInfoCommand(String tag) {
        this(tag, null);
    }

    public GetTcpInfoCommand(String tag, String dbName) {
        super(TcpConnectionInfo.class);
        this.tag = tag;
        this.dbName = dbName;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        if (StringUtils.isEmpty(dbName)) {
            url.value = node.getUrl() + "/info/tcp?tcp=" + tag;
        } else {
            url.value = node.getUrl() + "/databases/" + dbName + "/info/tcp?tag=" + tag;
        }

        requestedNode = node;
        return new HttpGet();
    }

    public ServerNode getRequestedNode() {
        return requestedNode;
    }

    public void setRequestedNode(ServerNode requestedNode) {
        this.requestedNode = requestedNode;
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            throwInvalidResponse();
        }

        result = mapper.readValue(response, TcpConnectionInfo.class);
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
