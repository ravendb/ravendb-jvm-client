package net.ravendb.client.documents.operations.backups;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class StartBackupOperation implements IMaintenanceOperation<StartBackupOperationResult> {

    private final boolean _isFullBackup;
    private final long _taskId;

    public StartBackupOperation(boolean isFullBackup, long taskId) {
        _isFullBackup = isFullBackup;
        _taskId = taskId;
    }

    @Override
    public RavenCommand<StartBackupOperationResult> getCommand(DocumentConventions conventions) {
        return new StartBackupCommand(_isFullBackup, _taskId);
    }

    private static class StartBackupCommand extends RavenCommand<StartBackupOperationResult> {
        private final boolean _isFullBackup;
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
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase()
                    + "/admin/backup/database?isFullBackup=" + (_isFullBackup ? "true":"false")
                    + "&taskId=" + _taskId;

            return new HttpPost();
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
