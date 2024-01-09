package net.ravendb.client.serverwide.operations.ongoingTasks;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class PutServerWideExternalReplicationOperation implements IServerOperation<ServerWideExternalReplicationResponse> {
    private final ServerWideExternalReplication _configuration;

    public PutServerWideExternalReplicationOperation(ServerWideExternalReplication configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        _configuration = configuration;
    }

    @Override
    public RavenCommand<ServerWideExternalReplicationResponse> getCommand(DocumentConventions conventions) {
        return new PutServerWideExternalReplicationCommand(conventions, _configuration);
    }

    private static class PutServerWideExternalReplicationCommand extends RavenCommand<ServerWideExternalReplicationResponse> implements IRaftCommand {
        private final DocumentConventions _conventions;
        private final ObjectNode _configuration;

        public PutServerWideExternalReplicationCommand(DocumentConventions conventions, ServerWideExternalReplication configuration) {
            super(ServerWideExternalReplicationResponse.class);

            if (configuration == null) {
                throw new IllegalArgumentException("Configuration cannot be null");
            }
            _conventions = conventions;
            _configuration = mapper.valueToTree(configuration);
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
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/configuration/server-wide/external-replication";

            HttpPut request = new HttpPut();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeObject(_configuration);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = mapper.readValue(response, ServerWideExternalReplicationResponse.class);
        }
    }
}
