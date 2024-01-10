package net.ravendb.client.documents.operations.ongoingTasks;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.serverwide.operations.ModifyOngoingTaskResult;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class ToggleOngoingTaskStateOperation implements IMaintenanceOperation<ModifyOngoingTaskResult> {

    private final long _taskId;
    private final String _taskName;
    private final OngoingTaskType _type;
    private final boolean _disable;

    public ToggleOngoingTaskStateOperation(String taskName, OngoingTaskType type, boolean disable) {
        if (StringUtils.isEmpty(taskName)) {
            throw new IllegalStateException("TaskName id must have a non empty value");
        }
        _taskName = taskName;
        _taskId = 0;
        _type = type;
        _disable = disable;
    }

    public ToggleOngoingTaskStateOperation(long taskId, OngoingTaskType type, boolean disable) {
        _taskId = taskId;
        _taskName = null;
        _type = type;
        _disable = disable;
    }

    @Override
    public RavenCommand<ModifyOngoingTaskResult> getCommand(DocumentConventions conventions) {
        return new ToggleTaskStateCommand(_taskId, _taskName, _type, _disable);
    }

    private static class ToggleTaskStateCommand extends RavenCommand<ModifyOngoingTaskResult> implements IRaftCommand {
        private final long _taskId;
        private final String _taskName;
        private final OngoingTaskType _type;
        private final boolean _disable;

        public ToggleTaskStateCommand(long taskId, String taskName, OngoingTaskType type, boolean disable) {
            super(ModifyOngoingTaskResult.class);

            _taskId = taskId;
            _taskName = taskName;
            _type = type;
            _disable = disable;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/"
                    + node.getDatabase() + "/admin/tasks/state?key="
                    + _taskId + "&type=" + SharpEnum.value(_type)
                    + "&disable=" + (_disable ? "true": "false");

            if (_taskName != null) {
                url += "&taskName=" + urlEncode(_taskName);
            }

            return new HttpPost(url);
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

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
