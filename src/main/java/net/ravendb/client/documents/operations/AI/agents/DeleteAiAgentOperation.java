package net.ravendb.client.documents.operations.AI.agents;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.serverwide.tcp.TcpConnectionHeaderMessage;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import java.io.IOException;

public class DeleteAiAgentOperation implements IMaintenanceOperation<AiAgentConfigurationResult> {
    private final String identifier;

    public DeleteAiAgentOperation(String identifier) {
        if(identifier == null || identifier.trim().isEmpty()) {
            throw new IllegalArgumentException("Identifier cannot be null or empty");
        }
        this.identifier = identifier;
    }

    public TcpConnectionHeaderMessage.OperationResultType getResultType() {
        return TcpConnectionHeaderMessage.OperationResultType.CommandResult;
    }

    @Override
    public RavenCommand<AiAgentConfigurationResult> getCommand(DocumentConventions conventions) {
        return new DeleteAiAgentCommand(this.identifier, conventions);
    }

    class DeleteAiAgentCommand extends RavenCommand<AiAgentConfigurationResult> {
        private final String identifier;
        private final DocumentConventions conventions;

        public DeleteAiAgentCommand(String identifier, DocumentConventions conventions) {
            super(AiAgentConfigurationResult.class);
            this.identifier = identifier;
            this.conventions = conventions;
        }

        @Override
        public boolean isReadRequest() {return false;}

        @Override
        public HttpUriRequestBase createRequest(ServerNode node){
            String uri = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/ai/agent"
                    + "?agentId=" + UrlUtils.escapeDataString(identifier);
            return new HttpDelete(uri);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = mapper.readValue(response, AiAgentConfigurationResult.class);
        }
    }
}
