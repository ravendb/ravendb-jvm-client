package net.ravendb.client.serverwide.operations.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.configuration.ClientConfiguration;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class PutServerWideClientConfigurationOperation implements IVoidServerOperation {
    private final ClientConfiguration _configuration;

    public PutServerWideClientConfigurationOperation(ClientConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        _configuration = configuration;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new PutServerWideClientConfigurationCommand(conventions, _configuration);
    }

    private static class PutServerWideClientConfigurationCommand extends VoidRavenCommand implements IRaftCommand {
        private final ClientConfiguration _configuration;
        private final DocumentConventions _conventions;

        public PutServerWideClientConfigurationCommand(DocumentConventions conventions, ClientConfiguration configuration) {
            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }
            if (configuration == null) {
                throw new IllegalArgumentException("Configuration cannot be null");
            }

            _conventions = conventions;
            _configuration = configuration;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/configuration/client";

            HttpPut request = new HttpPut(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_configuration);
                    generator.writeTree(config);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON, _conventions));
            return request;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
