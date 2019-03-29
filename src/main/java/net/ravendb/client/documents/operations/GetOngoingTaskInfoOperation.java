package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.ongoingTasks.*;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetOngoingTaskInfoOperation implements IMaintenanceOperation<OngoingTask> {

    private final String _taskName;
    private final long _taskId;
    private final OngoingTaskType _type;

    public GetOngoingTaskInfoOperation(long taskId, OngoingTaskType type) {
        _taskId = taskId;
        _type = type;
        _taskName = null;
    }

    public GetOngoingTaskInfoOperation(String taskName, OngoingTaskType type) {
        _taskName = taskName;
        _type = type;
        _taskId = 0;
    }

    @Override
    public RavenCommand<OngoingTask> getCommand(DocumentConventions conventions) {
        if (_taskName != null) {
            return new GetOngoingTaskInfoCommand(_taskName, _type);
        }

        return new GetOngoingTaskInfoCommand(_taskId, _type);
    }

    private static class GetOngoingTaskInfoCommand extends RavenCommand<OngoingTask> {
        private final String _taskName;
        private final long _taskId;
        private final OngoingTaskType _type;

        public GetOngoingTaskInfoCommand(long taskId, OngoingTaskType type) {
            super(OngoingTask.class);

            _taskId = taskId;
            _type = type;
            _taskName = null;
        }

        public GetOngoingTaskInfoCommand(String taskName, OngoingTaskType type) {
            super(OngoingTask.class);

            if (StringUtils.isEmpty(taskName)) {
                throw new IllegalArgumentException("Value cannot be empty");
            }

            _taskName = taskName;
            _type = type;
            _taskId = 0;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = _taskName != null ?
                    node.getUrl() + "/databases/" + node.getDatabase() + "/task?taskName=" + UrlUtils.escapeDataString(_taskName) + "&type=" + SharpEnum.value(_type) :
                    node.getUrl() + "/databases/" + node.getDatabase() + "/task?key=" + _taskId + "&type=" + SharpEnum.value(_type);

            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response != null) {
                switch (_type) {
                    case REPLICATION:
                        result = mapper.readValue(response, OngoingTaskReplication.class);
                        break;
                    case RAVEN_ETL:
                        result = mapper.readValue(response, OngoingTaskRavenEtlDetails.class);
                        break;
                    case SQL_ETL:
                        result = mapper.readValue(response, OngoingTaskSqlEtlDetails.class);
                        break;
                    case BACKUP:
                        result = mapper.readValue(response, OngoingTaskBackup.class);
                        break;
                    case SUBSCRIPTION:
                        result = mapper.readValue(response, OngoingTaskSubscription.class);
                        break;
                    default:
                        throw new IllegalStateException();
                }
            }
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }
    }
}
