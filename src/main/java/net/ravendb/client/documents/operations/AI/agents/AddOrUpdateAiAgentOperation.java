package net.ravendb.client.documents.operations.AI.agents;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.tcp.TcpConnectionHeaderMessage;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.commons.lang3.StringUtils;
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

        this.configuration = configuration;
        this.sampleObject = sampleObject;
    }

    public static <T> AddOrUpdateAiAgentOperation create(AiAgentConfiguration configuration, T outputType) {
        return new AddOrUpdateAiAgentOperation(configuration, outputType);
    }

    public TcpConnectionHeaderMessage.OperationResultType getResultType() {
        return TcpConnectionHeaderMessage.OperationResultType.CommandResult;
    }

    @Override
    public RavenCommand<AiAgentConfigurationResult> getCommand(DocumentConventions conventions) {
        if (sampleObject != null && hasNoSampleObjectAndScheme(configuration)) {
            try {
                configuration.setSampleObject(conventions.getEntityMapper().writeValueAsString(sampleObject));
            } catch (JsonProcessingException e) {
                throw new RuntimeException("Failed to serialize sample object to JSON", e);
            }
        }

        return new AddOrUpdateAiAgentCommand(configuration, conventions);
    }

    public static boolean hasNoSampleObjectAndScheme(AiAgentConfiguration configuration) {
        return StringUtils.isBlank(configuration.getOutputSchema()) && StringUtils.isBlank(configuration.getSampleObject());
    }

    private static class AddOrUpdateAiAgentCommand extends RavenCommand<AiAgentConfigurationResult> implements IRaftCommand {
        private final AiAgentConfiguration configuration;
        private final DocumentConventions conventions;

        public AddOrUpdateAiAgentCommand(AiAgentConfiguration configuration, DocumentConventions conventions) {
            super(AiAgentConfigurationResult.class);
            if (hasNoSampleObjectAndScheme(configuration)) {
                // message mirrors the C# client (which interpolates the OutputSchema value)
                throw new IllegalArgumentException("Please provide a non-empty value for either "
                        + StringUtils.defaultString(configuration.getOutputSchema()) + " or SampleObject is required.");
            }
            this.configuration = configuration;
            this.conventions = conventions;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String uri = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/ai/agent";

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
            if (response == null) {
                throwInvalidResponse();
            }
            result = mapper.readValue(response, AiAgentConfigurationResult.class);
        }
    }
}
