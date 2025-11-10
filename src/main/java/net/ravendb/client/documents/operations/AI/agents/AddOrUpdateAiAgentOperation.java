package net.ravendb.client.documents.operations.AI.agents;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.tcp.TcpConnectionHeaderMessage;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;
import java.io.IOException;

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

    class AddOrUpdateAiAgentCommand extends RavenCommand<AiAgentConfigurationResult> implements IRaftCommand {
        private final AiAgentConfiguration configuration;
        private final DocumentConventions conventions;
        private final Object sampleSchema;

        public AddOrUpdateAiAgentCommand(AiAgentConfiguration configuration, Object sampleSchema, DocumentConventions conventions) {
            super(AiAgentConfigurationResult.class);
            if(AddOrUpdateAiAgentOperation.hasNoSampleObjectAndScheme(configuration))
                throw new IllegalArgumentException("Please provide a non-empty value for either outputSchema or sampleObject.");
            this.configuration = configuration;
            this.conventions = conventions;
            this.sampleSchema = sampleSchema;
        }

        @Override
        public boolean isReadRequest() {return false;}

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String uri = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/ai/agent";

            if (this.configuration == null  && sampleSchema != null) {
                this.configuration.setSampleObject(sampleSchema.toString());
            }

            HttpPut request = new HttpPut(uri);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(configuration);
                    generator.writeTree(config);
                }
            }, ContentType.APPLICATION_JSON, conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = mapper.readValue(response, AiAgentConfigurationResult.class);
        }
    }
}
