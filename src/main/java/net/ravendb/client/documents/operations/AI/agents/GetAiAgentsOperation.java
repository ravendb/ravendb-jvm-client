package net.ravendb.client.documents.operations.AI.agents;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.serverwide.tcp.TcpConnectionHeaderMessage;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import java.io.IOException;

public class GetAiAgentsOperation implements IMaintenanceOperation<GetAiAgentsResponse> {
    private final String agentId;

    public GetAiAgentsOperation(String agentId) {
        this.agentId = agentId;
    }

    public GetAiAgentsOperation() {
        this(null);
    }

    public TcpConnectionHeaderMessage.OperationResultType getResultType() {
        return TcpConnectionHeaderMessage.OperationResultType.CommandResult;
    }

    @Override
    public RavenCommand<GetAiAgentsResponse> getCommand(DocumentConventions conventions) {
        return new GetAiAgentsCommand(agentId, conventions);
    }

    class GetAiAgentsCommand extends RavenCommand<GetAiAgentsResponse> {
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

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = mapper.readValue(response, GetAiAgentsResponse.class);
        }
    }
}
