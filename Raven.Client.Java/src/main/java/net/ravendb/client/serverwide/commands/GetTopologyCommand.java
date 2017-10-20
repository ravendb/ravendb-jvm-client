package net.ravendb.client.serverwide.commands;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.Topology;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.io.InputStream;

public class GetTopologyCommand extends RavenCommand<Topology> {

    private final String forcedUrl;

    public GetTopologyCommand() {
        this(null);
    }

    public GetTopologyCommand(String forcedUrl) {
        super(Topology.class);
        this.forcedUrl = forcedUrl;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/topology?name=" + node.getDatabase();
        if (StringUtils.isNotEmpty(forcedUrl)) {
            url.value += "&url=" + forcedUrl;
        }

        return new HttpGet();
    }

    @Override
    public void setResponse(InputStream response, boolean fromCache) throws IOException {
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
