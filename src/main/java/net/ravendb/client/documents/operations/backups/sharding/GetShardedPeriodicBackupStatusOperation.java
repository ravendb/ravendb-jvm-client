package net.ravendb.client.documents.operations.backups.sharding;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.backups.AbstractGetPeriodicBackupStatusOperationResult;
import net.ravendb.client.documents.operations.backups.PeriodicBackupStatus;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.util.Map;

public class GetShardedPeriodicBackupStatusOperation implements IMaintenanceOperation<GetShardedPeriodicBackupStatusOperation.GetShardedPeriodicBackupStatusOperationResult> {
    private final long _taskId;

    public GetShardedPeriodicBackupStatusOperation(long taskId) {
        _taskId = taskId;
    }

    @Override
    public RavenCommand<GetShardedPeriodicBackupStatusOperationResult> getCommand(DocumentConventions conventions) {
        return new GetShardedPeriodicBackupStatusCommand(_taskId);
    }

    private static class GetShardedPeriodicBackupStatusCommand extends RavenCommand<GetShardedPeriodicBackupStatusOperationResult> {
        private final long _taskId;

        public GetShardedPeriodicBackupStatusCommand(long taskId) {
            super(GetShardedPeriodicBackupStatusOperationResult.class);
            _taskId = taskId;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/periodic-backup/status?name=" + node.getDatabase() + "&taskId=" + _taskId;

            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);

            if (!result.isSharded()) {
                throw new IllegalStateException("Database is not sharded, can't use GetShardedPeriodicBackupStatusOperation, use GetPeriodicBackupStatusOperation instead.");
            }
        }
    }

    public static class GetShardedPeriodicBackupStatusOperationResult extends AbstractGetPeriodicBackupStatusOperationResult {
        private Map<Integer, PeriodicBackupStatus> statuses;

        public Map<Integer, PeriodicBackupStatus> getStatuses() {
            return statuses;
        }

        public void setStatuses(Map<Integer, PeriodicBackupStatus> statuses) {
            this.statuses = statuses;
        }
    }
}
