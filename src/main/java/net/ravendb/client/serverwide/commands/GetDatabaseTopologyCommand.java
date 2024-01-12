package net.ravendb.client.serverwide.commands;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.Topology;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

public class GetDatabaseTopologyCommand extends RavenCommand<Topology> {

    private final UUID _applicationIdentifier;
    private final String _debugTag;

    public GetDatabaseTopologyCommand() {
        this(null, null);
    }

    public GetDatabaseTopologyCommand(String debugTag, UUID applicationIdentifier) {
        super(Topology.class);
        _debugTag = debugTag;
        timeout = Duration.ofSeconds(15);
        _applicationIdentifier = applicationIdentifier;

        timeout = Duration.ofSeconds(15);
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/topology?name=" + node.getDatabase();

        if (_debugTag != null) {
            url += "&" + _debugTag;
        }

        if (_applicationIdentifier != null) {
            url += "&applicationIdentifier=" + urlEncode(_applicationIdentifier.toString());
        }

        if (node.getUrl().toLowerCase().contains(".fiddler")) {
            // we want to keep the '.fiddler' stuff there so we'll keep tracking request
            // so we are going to ask the server to respect it
            url += "&localUrl=" + UrlUtils.escapeDataString(node.getUrl());
        }

        return new HttpGet(url);
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
