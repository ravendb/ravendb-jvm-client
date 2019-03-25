package net.ravendb.client.serverwide.operations.configuration;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.configuration.ClientConfiguration;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

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

    private static class PutServerWideClientConfigurationCommand extends VoidRavenCommand {
        private final ClientConfiguration _configuration;

        public PutServerWideClientConfigurationCommand(DocumentConventions conventions, ClientConfiguration configuration) {
            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }
            if (configuration == null) {
                throw new IllegalArgumentException("Configuration cannot be null");
            }

            _configuration = configuration;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/configuration/client";

            HttpPut request = new HttpPut();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_configuration);
                    generator.writeTree(config);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));
            return request;
        }
    }
}
