package net.ravendb.client.documents.operations.AI.agents;

import net.ravendb.client.documents.commands.RunConversationCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.AI.AiStreamCallback;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.serverwide.tcp.TcpConnectionHeaderMessage;
import java.util.List;

public class RunConversationOperation<TAnswer> implements IMaintenanceOperation<ConversationResult<TAnswer>> {
    private final String agentId;
    private final String conversationId;
    private final String userPrompt;
    private final List<AiAgentActionResponse> actionResponses;
    private final AiConversationCreationOptions options;
    private final String changeVector;
    private final String streamPropertyPath;
    private final AiStreamCallback streamCallback;

    public RunConversationOperation(
            String agentId,
            String conversationId,
            String userPrompt,
            List<AiAgentActionResponse> actionResponses,
            AiConversationCreationOptions options,
            String changeVector,
            String streamPropertyPath,
            AiStreamCallback streamCallback
    ) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("agentId cannot be null or empty.");
        }
        if (conversationId == null || conversationId.trim().isEmpty()) {
            throw new IllegalArgumentException("conversationId cannot be null or empty.");
        }

        if ((streamPropertyPath != null) != (streamCallback != null)) {
            throw new IllegalStateException("Both streamPropertyPath and streamCallback must be specified together or neither.");
        }

        this.agentId = agentId;
        this.conversationId = conversationId;
        this.userPrompt = userPrompt;
        this.actionResponses = actionResponses;
        this.options = options;
        this.changeVector = changeVector;
        this.streamPropertyPath = streamPropertyPath;
        this.streamCallback = streamCallback;
    }

    public TcpConnectionHeaderMessage.OperationResultType getResultType() {
        return TcpConnectionHeaderMessage.OperationResultType.CommandResult;
    }

    @Override
    public RavenCommand<ConversationResult<TAnswer>> getCommand(DocumentConventions conventions) {
        return new RunConversationCommand<TAnswer>(
                conversationId,
                agentId,
                userPrompt,
                actionResponses,
                options,
                changeVector,
                conventions,
                streamPropertyPath,
                streamCallback
        );
    }
}
