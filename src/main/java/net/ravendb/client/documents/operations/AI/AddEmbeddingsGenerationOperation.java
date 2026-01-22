package net.ravendb.client.documents.operations.AI;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;
import java.io.IOException;

public class AddEmbeddingsGenerationOperation implements IMaintenanceOperation<AddEmbeddingsGenerationOperationResult> {

    private final EmbeddingsGenerationConfiguration configuration;

    public AddEmbeddingsGenerationOperation(EmbeddingsGenerationConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }
        this.configuration = configuration;
    }

    @Override
    public RavenCommand<AddEmbeddingsGenerationOperationResult> getCommand(DocumentConventions conventions) {
        return new AddEmbeddingsGenerationCommand(conventions, configuration);
    }

    class AddEmbeddingsGenerationCommand extends RavenCommand<AddEmbeddingsGenerationOperationResult>
            implements IRaftCommand {

        private final DocumentConventions conventions;
        private final EmbeddingsGenerationConfiguration configuration;
        private final String raftUniqueRequestId = RaftIdGenerator.newId();

        public AddEmbeddingsGenerationCommand(DocumentConventions conventions, EmbeddingsGenerationConfiguration configuration) {
            super(AddEmbeddingsGenerationOperationResult.class);

            if (conventions == null) {
                throw new IllegalArgumentException("conventions cannot be null");
            }
            if (configuration == null) {
                throw new IllegalArgumentException("configuration cannot be null");
            }

            this.conventions = conventions;
            this.configuration = configuration;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/etl";

            HttpPut request = new HttpPut(url);
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

            result = mapper.readValue(response, resultClass);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return raftUniqueRequestId;
        }
    }
}
