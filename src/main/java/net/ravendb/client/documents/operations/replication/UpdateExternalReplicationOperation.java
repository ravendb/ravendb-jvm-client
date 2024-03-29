package net.ravendb.client.documents.operations.replication;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.operations.ModifyOngoingTaskResult;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class UpdateExternalReplicationOperation implements IMaintenanceOperation<ModifyOngoingTaskResult> {

    private final ExternalReplication _newWatcher;

    public UpdateExternalReplicationOperation(ExternalReplication newWatcher) {
        if (newWatcher == null) {
            throw new IllegalArgumentException("NewWatcher cannot be null");
        }
        _newWatcher = newWatcher;
    }

    @Override
    public RavenCommand<ModifyOngoingTaskResult> getCommand(DocumentConventions conventions) {
        return new UpdateExternalReplication(conventions, _newWatcher);
    }

    private static class UpdateExternalReplication extends RavenCommand<ModifyOngoingTaskResult> implements IRaftCommand {
        private final ExternalReplication _newWatcher;
        private final DocumentConventions _conventions;

        public UpdateExternalReplication(DocumentConventions conventions, ExternalReplication newWatcher) {
            super(ModifyOngoingTaskResult.class);

            _conventions = conventions;
            _newWatcher = newWatcher;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/tasks/external-replication";

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeFieldName("Watcher");

                    ObjectNode tree = mapper.valueToTree(_newWatcher);

                    generator.writeTree(tree);

                    generator.writeEndObject();
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public boolean isReadRequest() {
            return false;
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
