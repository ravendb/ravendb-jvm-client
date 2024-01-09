package net.ravendb.client.documents.operations.replication;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.ModifyOngoingTaskResult;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class UpdatePullReplicationAsSinkOperation implements IMaintenanceOperation<ModifyOngoingTaskResult> {
    private final PullReplicationAsSink _pullReplication;

    public UpdatePullReplicationAsSinkOperation(PullReplicationAsSink pullReplication) {
        _pullReplication = pullReplication;
    }

    @Override
    public RavenCommand<ModifyOngoingTaskResult> getCommand(DocumentConventions conventions) {
        return new UpdatePullEdgeReplication(conventions, _pullReplication);
    }

    private static class UpdatePullEdgeReplication extends RavenCommand<ModifyOngoingTaskResult> implements IRaftCommand {
        private final PullReplicationAsSink _pullReplication;
        private final DocumentConventions _conventions;

        public UpdatePullEdgeReplication(DocumentConventions conventions, PullReplicationAsSink pullReplication) {
            super(ModifyOngoingTaskResult.class);
            if (pullReplication == null) {
                throw new IllegalArgumentException("PullReplication cannot be null");
            }
            _pullReplication = pullReplication;
            _conventions = conventions;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/tasks/sink-pull-replication";

            HttpPost request = new HttpPost();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeFieldName("PullReplicationAsSink");
                    generator.getCodec().writeValue(generator, _pullReplication);
                    generator.writeEndObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, ModifyOngoingTaskResult.class);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
