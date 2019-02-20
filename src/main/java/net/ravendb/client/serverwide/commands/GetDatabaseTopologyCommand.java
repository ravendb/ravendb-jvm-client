package net.ravendb.client.serverwide.commands;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.Topology;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetDatabaseTopologyCommand extends RavenCommand<Topology> {

    private final String _debugTag;

    public GetDatabaseTopologyCommand() {
        this(null);
    }

    public GetDatabaseTopologyCommand(String debugTag) {
        super(Topology.class);
        _debugTag = debugTag;
        canCacheAggressively = false;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/topology?name=" + node.getDatabase();

        if (_debugTag != null) {
            url.value += "&" + _debugTag;
        }

        if (node.getUrl().toLowerCase().contains(".fiddler")) {
            // we want to keep the '.fiddler' stuff there so we'll keep tracking request
            // so we are going to ask the server to respect it
            url.value += "&localUrl=" + UrlUtils.escapeDataString(node.getUrl());
        }

        return new HttpGet();
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            return;
        }

        result = mapper.readValue(response, resultClass);
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
