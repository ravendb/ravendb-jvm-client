package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.replication.PullReplicationDefinitionAndCurrentConnections;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetPullReplicationHubTasksInfoOperation implements IMaintenanceOperation<PullReplicationDefinitionAndCurrentConnections> {

    private final long _taskId;

    public GetPullReplicationHubTasksInfoOperation(long taskId) {
        _taskId = taskId;
    }

    @Override
    public RavenCommand<PullReplicationDefinitionAndCurrentConnections> getCommand(DocumentConventions conventions) {
        return new GetPullReplicationTasksInfoCommand(_taskId);
    }

    private static class GetPullReplicationTasksInfoCommand extends RavenCommand<PullReplicationDefinitionAndCurrentConnections> {
        private final long _taskId;

        public GetPullReplicationTasksInfoCommand(long taskId) {
            super(PullReplicationDefinitionAndCurrentConnections.class);
            _taskId = taskId;
        }

        public GetPullReplicationTasksInfoCommand(long taskId, String nodeTag) {
            this(taskId);
            selectedNodeTag = nodeTag;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/tasks/pull-replication/hub?key=" + _taskId;

            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response != null) {
                result = mapper.readValue(response, PullReplicationDefinitionAndCurrentConnections.class);
            }
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }
    }
}
