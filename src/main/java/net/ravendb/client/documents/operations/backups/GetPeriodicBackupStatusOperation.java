package net.ravendb.client.documents.operations.backups;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetPeriodicBackupStatusOperation implements IMaintenanceOperation<GetPeriodicBackupStatusOperationResult> {

    private final long _taskId;

    public GetPeriodicBackupStatusOperation(long taskId) {
        _taskId = taskId;
    }

    @Override
    public RavenCommand<GetPeriodicBackupStatusOperationResult> getCommand(DocumentConventions conventions) {
        return new GetPeriodicBackupStatusCommand(_taskId);
    }

    private static class GetPeriodicBackupStatusCommand extends RavenCommand<GetPeriodicBackupStatusOperationResult> {
        private final long _taskId;

        public GetPeriodicBackupStatusCommand(long taskId) {
            super(GetPeriodicBackupStatusOperationResult.class);

            _taskId = taskId;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/periodic-backup/status?name=" + node.getDatabase() + "&taskId=" + _taskId;

            return new HttpGet(url);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);

            if (result.isSharded()) {
                throw new IllegalStateException("Database is sharded, can't use GetPeriodicBackupStatusOperation. Use GetShardedPeriodicBackupStatusOperation instead.");
            }
        }
    }
}
