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
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class UpdateQueueSinkOperation<T extends ConnectionString> implements IMaintenanceOperation<UpdateQueueSinkOperation.UpdateQueueSinkOperationResult> {

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
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/queue-sink?id=" + _taskId;

            HttpPut request = new HttpPut();

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_configuration);
                    generator.writeTree(config);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

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

    public static class UpdateQueueSinkOperationResult {
        private long raftCommandIndex;
        private long taskId;

        public long getRaftCommandIndex() {
            return raftCommandIndex;
        }

        public void setRaftCommandIndex(long raftCommandIndex) {
            this.raftCommandIndex = raftCommandIndex;
        }

        public long getTaskId() {
            return taskId;
        }

        public void setTaskId(long taskId) {
            this.taskId = taskId;
        }
    }
}
