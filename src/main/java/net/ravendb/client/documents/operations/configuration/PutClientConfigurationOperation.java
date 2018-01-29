package net.ravendb.client.documents.operations.configuration;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.ExceptionsUtils;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;

public class PutClientConfigurationOperation implements IVoidMaintenanceOperation {
    private final ClientConfiguration configuration;

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

    private static class PutClientConfigurationCommand extends VoidRavenCommand {
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
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/configuration/client";

            HttpPut httpPut = new HttpPut();
            httpPut.setEntity(new StringEntity(this.configuration, ContentType.APPLICATION_JSON));
            return httpPut;
        }
    }
}
