package net.ravendb.client.serverwide.commands;

import net.ravendb.client.http.ClusterTopologyResponse;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetClusterTopologyCommand extends RavenCommand<ClusterTopologyResponse> {

    public GetClusterTopologyCommand() {
        super(ClusterTopologyResponse.class);
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/cluster/topology";

        return new HttpGet();
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            throwInvalidResponse();
        }

        result = mapper.readValue(response, resultClass);
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
