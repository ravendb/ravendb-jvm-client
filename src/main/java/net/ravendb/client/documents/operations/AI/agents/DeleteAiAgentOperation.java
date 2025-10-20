package net.ravendb.client.documents.operations.AI.agents;

import net.ravendb.client.documents.commands.DeleteAiAgentCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.serverwide.tcp.TcpConnectionHeaderMessage;

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
}
