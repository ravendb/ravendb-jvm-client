package net.ravendb.client.serverwide.commands;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetNodeInfoCommand extends RavenCommand<NodeInfo> {

    public GetNodeInfoCommand() {
        super(NodeInfo.class);
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/cluster/node-info";

        return new HttpGet();
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
