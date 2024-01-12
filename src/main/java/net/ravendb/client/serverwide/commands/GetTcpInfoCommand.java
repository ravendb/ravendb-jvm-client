package net.ravendb.client.serverwide.commands;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;
import java.time.Duration;

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
        this.timeout = Duration.ofSeconds(15);
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url;
        if (StringUtils.isEmpty(dbName)) {
            url = node.getUrl() + "/info/tcp?tcp=" + tag;
        } else {
            url = node.getUrl() + "/databases/" + dbName + "/info/tcp?tag=" + tag;
        }

        requestedNode = node;
        return new HttpGet(url);
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
