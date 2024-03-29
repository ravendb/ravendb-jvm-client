package net.ravendb.client.documents.operations.backups;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class StartBackupOperation implements IMaintenanceOperation<StartBackupOperationResult> {

    private final Boolean _isFullBackup;
    private final long _taskId;

    public StartBackupOperation(Boolean isFullBackup, long taskId) {
        _isFullBackup = isFullBackup;
        _taskId = taskId;
    }

    @Override
    public RavenCommand<StartBackupOperationResult> getCommand(DocumentConventions conventions) {
        return new StartBackupCommand(_isFullBackup, _taskId);
    }

    private static class StartBackupCommand extends RavenCommand<StartBackupOperationResult> {
        private final Boolean _isFullBackup;
        private final long _taskId;

        public StartBackupCommand(boolean isFullBackup, long taskId) {
            super(StartBackupOperationResult.class);

            _isFullBackup = isFullBackup;
            _taskId = taskId;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase()
                    + "/admin/backup/database?taskId=" + _taskId;

            if (_isFullBackup != null) {
                url += "&isFullBackup=" + (_isFullBackup ? "true": "false");
            }

            return new HttpPost(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }
    }
}
