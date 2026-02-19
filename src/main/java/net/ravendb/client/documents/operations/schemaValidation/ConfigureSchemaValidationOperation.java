package net.ravendb.client.documents.operations.schemaValidation;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class ConfigureSchemaValidationOperation
        implements IMaintenanceOperation<ConfigureSchemaValidationOperationResult> {

    private final SchemaValidationConfiguration configuration;

    /**
     * Creates a new operation that configures schema validation.
     *
     * @param configuration the schema validation configuration to apply
     * @throws IllegalArgumentException if configuration is null
     */
    public ConfigureSchemaValidationOperation(SchemaValidationConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }
        this.configuration = configuration;
    }

    @Override
    public RavenCommand<ConfigureSchemaValidationOperationResult> getCommand(
            DocumentConventions conventions) {
        return new ConfigureSchemaValidationCommand(conventions, configuration);
    }

    private static class ConfigureSchemaValidationCommand
            extends RavenCommand<ConfigureSchemaValidationOperationResult>
            implements IRaftCommand {

        private final DocumentConventions conventions;
        private final SchemaValidationConfiguration configuration;

        public ConfigureSchemaValidationCommand(
                DocumentConventions conventions,
                SchemaValidationConfiguration configuration) {
            super(ConfigureSchemaValidationOperationResult.class);

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
            String url = node.getUrl()
                    + "/databases/" + node.getDatabase()
                    + "/admin/schema-validation/config";

            HttpPost request = new HttpPost(url);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode json = mapper.valueToTree(configuration);
                    generator.writeTree(json);
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
            return RaftIdGenerator.newId();
        }
    }
}
