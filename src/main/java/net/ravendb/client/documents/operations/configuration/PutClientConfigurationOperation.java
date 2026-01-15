package net.ravendb.client.documents.operations.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.ExceptionsUtils;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

/**
 * Sets or modifies the client configuration on the server using the {@link PutClientConfigurationOperation}.
 * The client configuration consists of various options that control client-server communication.
 * <p>
 * The initial configuration is set when creating the Document Store, but it can be dynamically modified
 * by a database administrator using this operation.
 * </p>
 * <p>
 * <strong>Note:</strong> The client will update its configuration the next time it sends a request to the database.
 * </p>
 */
public class PutClientConfigurationOperation implements IVoidMaintenanceOperation {
    private final ClientConfiguration configuration;

    /**
     * @see PutClientConfigurationOperation
     * @param configuration The {@link ClientConfiguration} object containing the configuration settings to be applied on the server.
     */
    public PutClientConfigurationOperation(ClientConfiguration configuration) {

        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        this.configuration = configuration;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new PutClientConfigurationCommand(conventions, this.configuration);
    }

    private static class PutClientConfigurationCommand extends VoidRavenCommand implements IRaftCommand {
        private final String configuration;

        public PutClientConfigurationCommand(DocumentConventions conventions, ClientConfiguration configuration) {
            if (conventions == null) {
                throw new IllegalArgumentException("conventions cannot be null");
            }

            if (configuration == null) {
                throw new IllegalArgumentException("configuration cannot be null");
            }

            try {
                this.configuration = mapper.writeValueAsString(configuration);
            } catch (JsonProcessingException e) {
                throw ExceptionsUtils.unwrapException(e);
            }
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/configuration/client";

            HttpPut httpPut = new HttpPut(url);
            httpPut.setEntity(new StringEntity(this.configuration, ContentType.APPLICATION_JSON));
            return httpPut;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
