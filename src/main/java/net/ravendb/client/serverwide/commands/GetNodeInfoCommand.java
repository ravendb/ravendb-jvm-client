package net.ravendb.client.serverwide.commands;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetNodeInfoCommand extends RavenCommand<NodeInfo> {

    public GetNodeInfoCommand() {
        super(NodeInfo.class);
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/cluster/node-info";

        return new HttpGet(url);
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            return;
        }

        result = mapper.readValue(response, NodeInfo.class);
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
