package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.AI.agents.AddOrUpdateAiAgentOperation;
import net.ravendb.client.documents.operations.AI.agents.AiAgentConfigurationResult;
import net.ravendb.client.documents.operations.AI.agents.config.AiAgentConfiguration;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.HttpReset;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

public class AddOrUpdateAiAgentCommand extends RavenCommand<AiAgentConfigurationResult> implements IRaftCommand {
    private final AiAgentConfiguration configuration;
    private final DocumentConventions conventions;
    private final Object sampleSchema;

    public AddOrUpdateAiAgentCommand(AiAgentConfiguration configuration, Object sampleSchema, DocumentConventions conventions) {
        super(AiAgentConfigurationResult.class);
        if(AddOrUpdateAiAgentOperation.hasNoSampleObjectAndScheme(configuration))
            throw new IllegalArgumentException("Please provide a non-empty value for either outputSchema or sampleSchema");
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
}
