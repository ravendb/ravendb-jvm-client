package net.ravendb.client.documents.operations.queueSink;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.connectionStrings.ConnectionString;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class UpdateQueueSinkOperation<T extends ConnectionString> implements IMaintenanceOperation<UpdateQueueSinkOperationResult> {

    private final long _taskId;
    private final QueueSinkConfiguration _configuration;

    public UpdateQueueSinkOperation(long taskId, QueueSinkConfiguration configuration) {
        _taskId = taskId;
        _configuration = configuration;
    }

    @Override
    public RavenCommand<UpdateQueueSinkOperationResult> getCommand(DocumentConventions conventions) {
        return new UpdateQueueSinkCommand(conventions, _taskId, _configuration);
    }

    private static class UpdateQueueSinkCommand extends RavenCommand<UpdateQueueSinkOperationResult> implements IRaftCommand {
        private final long _taskId;
        private final QueueSinkConfiguration _configuration;
        private final DocumentConventions _conventions;

        public UpdateQueueSinkCommand(DocumentConventions conventions, long taskId, QueueSinkConfiguration configuration) {
            super(UpdateQueueSinkOperationResult.class);
            _taskId = taskId;
            if (configuration == null) {
                throw new IllegalArgumentException("Configuration is null");
            }
            _configuration = configuration;
            _conventions = conventions;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/queue-sink?id=" + _taskId;

            HttpPut request = new HttpPut(url);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_configuration);
                    generator.writeTree(config);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

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
