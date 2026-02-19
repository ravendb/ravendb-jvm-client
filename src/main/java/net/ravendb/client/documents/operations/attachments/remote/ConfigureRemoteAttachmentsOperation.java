package net.ravendb.client.documents.operations.attachments.remote;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.attachments.RemoteAttachmentsConfiguration;
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

/**
 * Represents a maintenance operation for configuring the remote attachments
 * feature in RavenDB.
 *
 * <p>
 * This operation enables the setup of automatic uploading of attachments to
 * remote storage destinations.
 * </p>
 */
public final class ConfigureRemoteAttachmentsOperation
        implements IMaintenanceOperation<ConfigureRemoteAttachmentsOperationResult> {

    private final RemoteAttachmentsConfiguration configuration;

    /**
     * Initializes a new instance of the {@code ConfigureRemoteAttachmentsOperation} class.
     *
     * <p>
     * This constructor accepts the configuration settings for the remote attachments
     * feature. The configuration cannot be {@code null}.
     * </p>
     *
     * @param configuration The configuration settings for remote attachments. Must not be {@code null}.
     * @throws NullPointerException If {@code configuration} is {@code null}.
     * @throws IllegalStateException If the configuration validation fails.
     */
    public ConfigureRemoteAttachmentsOperation(RemoteAttachmentsConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }

        configuration.assertConfiguration();
        this.configuration = configuration;
    }

    /**
     * Returns the command that will be executed to configure remote attachments.
     *
     * @param conventions The document conventions to use for the request.
     * @return A {@link RavenCommand} that configures remote attachments and returns the operation result.
     */
    @Override
    public RavenCommand<ConfigureRemoteAttachmentsOperationResult> getCommand(
            DocumentConventions conventions) {
        return new ConfigureAttachmentsRemoteCommand(conventions, configuration);
    }

    private static final class ConfigureAttachmentsRemoteCommand
            extends RavenCommand<ConfigureRemoteAttachmentsOperationResult>
            implements IRaftCommand {

        private final DocumentConventions conventions;
        private final RemoteAttachmentsConfiguration configuration;

        public ConfigureAttachmentsRemoteCommand(
                DocumentConventions conventions,
                RemoteAttachmentsConfiguration configuration) {
            super(ConfigureRemoteAttachmentsOperationResult.class);

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
                    + "/admin/attachments/remote/config";

            HttpPut request = new HttpPut(url);

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

