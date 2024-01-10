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

public class AddQueueSinkOperation<T extends ConnectionString> implements IMaintenanceOperation<AddQueueSinkOperation.AddQueueSinkOperationResult> {

    private final QueueSinkConfiguration _configuration;

    public AddQueueSinkOperation(QueueSinkConfiguration configuration) {
        _configuration = configuration;
    }

    @Override
    public RavenCommand<AddQueueSinkOperationResult> getCommand(DocumentConventions conventions) {
        return new AddQueueSinkCommand(conventions, _configuration);
    }

    private static class AddQueueSinkCommand extends RavenCommand<AddQueueSinkOperationResult> implements IRaftCommand {
        private final QueueSinkConfiguration _configuration;
        private final DocumentConventions _conventions;

        public AddQueueSinkCommand(DocumentConventions conventions, QueueSinkConfiguration configuration) {
            super(AddQueueSinkOperationResult.class);

            _conventions = conventions;
            _configuration = configuration;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/queue-sink";

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

    public static class AddQueueSinkOperationResult {
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
