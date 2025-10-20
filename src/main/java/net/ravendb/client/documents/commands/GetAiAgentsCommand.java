package net.ravendb.client.documents.commands;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.AI.agents.GetAiAgentsResponse;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;
import java.io.InputStream;

public class GetAiAgentsCommand extends RavenCommand<GetAiAgentsResponse> {
    private final String agentId;
    private final DocumentConventions conventions;

    public GetAiAgentsCommand(String agentId, DocumentConventions conventions) {
        super(GetAiAgentsResponse.class);
        this.agentId = agentId;
        this.conventions = conventions;
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        StringBuilder uri = new StringBuilder(node.getUrl())
                .append("/databases/")
                .append(node.getDatabase())
                .append("/admin/ai/agent");

        if (agentId != null && !agentId.isEmpty()) {
            uri.append("?agentId=").append(UrlUtils.escapeDataString(agentId));
        }

        return new HttpGet(uri.toString());
    }

    public void setResponse(InputStream responseStream, boolean fromCache) throws IOException {
        if (responseStream == null) {
            throwInvalidResponse();
        }

        try (InputStream stream = responseStream) {
            this.result = JsonExtensions.getDefaultMapper().readValue(stream, GetAiAgentsResponse.class);
        }
    }
}

