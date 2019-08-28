package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.serverwide.operations.ModifyOngoingTaskResult;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class ToggleOngoingTaskStateOperation implements IMaintenanceOperation<ModifyOngoingTaskResult> {

    private final long _taskId;
    private final OngoingTaskType _type;
    private final boolean _disable;

    public ToggleOngoingTaskStateOperation(long taskId, OngoingTaskType type, boolean disable) {
        _taskId = taskId;
        _type = type;
        _disable = disable;
    }

    @Override
    public RavenCommand<ModifyOngoingTaskResult> getCommand(DocumentConventions conventions) {
        return new ToggleTaskStateCommand(_taskId, _type, _disable);
    }

    private static class ToggleTaskStateCommand extends RavenCommand<ModifyOngoingTaskResult> {
        private final long _taskId;
        private final OngoingTaskType _type;
        private final boolean _disable;

        public ToggleTaskStateCommand(long taskId, OngoingTaskType type, boolean disable) {
            super(ModifyOngoingTaskResult.class);

            _taskId = taskId;
            _type = type;
            _disable = disable;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/tasks/state?key="
                    + _taskId + "&type=" + SharpEnum.value(_type) + "&disable=" + (_disable ? "true": "false");

            return new HttpPost();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response != null) {
                result = mapper.readValue(response, ModifyOngoingTaskResult.class);
            }
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }
    }
}
