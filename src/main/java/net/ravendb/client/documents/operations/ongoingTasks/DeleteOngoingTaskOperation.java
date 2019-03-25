package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.serverwide.operations.ModifyOngoingTaskResult;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class DeleteOngoingTaskOperation implements IMaintenanceOperation<ModifyOngoingTaskResult> {
    private final long _taskId;
    private final OngoingTaskType _taskType;

    public DeleteOngoingTaskOperation(long taskId, OngoingTaskType taskType) {
        _taskId = taskId;
        _taskType = taskType;
    }

    @Override
    public RavenCommand<ModifyOngoingTaskResult> getCommand(DocumentConventions conventions) {
        return new DeleteOngoingTaskCommand(_taskId, _taskType);
    }

    private static class DeleteOngoingTaskCommand extends RavenCommand<ModifyOngoingTaskResult> {
        private final long _taskId;
        private final OngoingTaskType _taskType;

        public DeleteOngoingTaskCommand(long taskId, OngoingTaskType taskType) {
            super(ModifyOngoingTaskResult.class);

            _taskId = taskId;
            _taskType = taskType;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/tasks?id=" + _taskId + "&type=" + SharpEnum.value(_taskType);

            return new HttpDelete();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }
    }
}
