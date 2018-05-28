package net.ravendb.client.documents.operations.replication;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.ModifyOngoingTaskResult;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class UpdateExternalReplicationOperation implements IMaintenanceOperation<ModifyOngoingTaskResult> {

    private final ExternalReplication _newWatcher;

    public UpdateExternalReplicationOperation(ExternalReplication newWatcher) {
        _newWatcher = newWatcher;
    }

    @Override
    public RavenCommand<ModifyOngoingTaskResult> getCommand(DocumentConventions conventions) {
        return new UpdateExternalReplication(_newWatcher);
    }

    private static class UpdateExternalReplication extends RavenCommand<ModifyOngoingTaskResult> {
        private final ExternalReplication _newWatcher;

        public UpdateExternalReplication(ExternalReplication newWatcher) {
            super(ModifyOngoingTaskResult.class);

            _newWatcher = newWatcher;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/tasks/external-replication";

            HttpPost request = new HttpPost();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeFieldName("Watcher");

                    ObjectNode tree = mapper.valueToTree(_newWatcher);

                    generator.writeTree(tree);

                    generator.writeEndObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

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
    }
}
