package net.ravendb.client.documents.operations.AI.agents;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.serverwide.tcp.TcpConnectionHeaderMessage;
import net.ravendb.client.documents.commands.AddOrUpdateAiAgentCommand;

public class AddOrUpdateAiAgentOperation implements IMaintenanceOperation<AiAgentConfigurationResult> {
    private final AiAgentConfiguration configuration;
    private final Object sampleObject;

    public AddOrUpdateAiAgentOperation(AiAgentConfiguration configuration) {
        this(configuration, null);
    }

    public AddOrUpdateAiAgentOperation(AiAgentConfiguration configuration, Object sampleObject) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        if (hasNoSampleObjectAndScheme(configuration) && sampleObject == null) {
            throw new IllegalArgumentException("Please provide a non-empty value for either outputSchema or sampleObject.");
        }
        this.configuration = configuration;
        this.sampleObject = sampleObject;
    }

    public TcpConnectionHeaderMessage.OperationResultType getResultType() {
        return TcpConnectionHeaderMessage.OperationResultType.CommandResult;
    }

    @Override
    public RavenCommand<AiAgentConfigurationResult> getCommand(DocumentConventions conventions) {
        String json = toJson(sampleObject);
        configuration.setSampleObject(json);
        return new AddOrUpdateAiAgentCommand(this.configuration, this.sampleObject, conventions);
    }

    public static boolean hasNoSampleObjectAndScheme(AiAgentConfiguration configuration) {
        return (configuration.getOutputSchema() == null || configuration.getOutputSchema().trim().isEmpty())
                && (configuration.getSampleObject() == null || configuration.getSampleObject().trim().isEmpty());
    }

    private String toJson(Object obj){
        try {
            return new ObjectMapper().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize object to JSON", e);
        }
    }
}
