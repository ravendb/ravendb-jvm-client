package net.ravendb.client.documents.operations.AI.agents;

import net.ravendb.client.documents.commands.GetAiAgentsCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.serverwide.tcp.TcpConnectionHeaderMessage;

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
}
