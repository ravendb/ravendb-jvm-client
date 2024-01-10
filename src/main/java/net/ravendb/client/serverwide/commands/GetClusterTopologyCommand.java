package net.ravendb.client.serverwide.commands;

import net.ravendb.client.http.ClusterTopologyResponse;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetClusterTopologyCommand extends RavenCommand<ClusterTopologyResponse> {

    private final String _debugTag;

    public GetClusterTopologyCommand() {
        super(ClusterTopologyResponse.class);

        _debugTag = null;
    }

    public GetClusterTopologyCommand(String debugTag) {
        super(ClusterTopologyResponse.class);
        _debugTag = debugTag;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/cluster/topology";

        if (_debugTag != null)
            url += "?" + _debugTag;

        return new HttpGet(url);
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
