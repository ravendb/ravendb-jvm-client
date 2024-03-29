package net.ravendb.client.documents.operations.backups;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.OperationIdResult;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class BackupOperation implements IMaintenanceOperation<OperationIdResult> {
    private final BackupConfiguration _backupConfiguration;

    public BackupOperation(BackupConfiguration backupConfiguration) {
        if (backupConfiguration == null) {
            throw new IllegalArgumentException("BackupConfiguration cannot be null");
        }

        _backupConfiguration = backupConfiguration;
    }

    @Override
    public RavenCommand<OperationIdResult> getCommand(DocumentConventions conventions) {
        return new BackupCommand(conventions, _backupConfiguration);
    }

    private static class BackupCommand extends RavenCommand<OperationIdResult> {

        private final BackupConfiguration _backupConfiguration;
        private final DocumentConventions _conventions;

        @Override
        public boolean isReadRequest() {
            return false;
        }

        public BackupCommand(DocumentConventions conventions, BackupConfiguration backupConfiguration) {
            super(OperationIdResult.class);
            _backupConfiguration = backupConfiguration;
            _conventions = conventions;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/backup";

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    mapper.writeValue(generator, _backupConfiguration);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            StartBackupOperationResult result = mapper.readValue(response, StartBackupOperationResult.class);
            OperationIdResult operationIdResult = mapper.readValue(response, OperationIdResult.class);

            // OperationNodeTag used to fetch operation status
            if (operationIdResult.getOperationNodeTag() == null) {
                operationIdResult.setOperationNodeTag(result.getResponsibleNode());
            }

            this.result = operationIdResult;
        }
    }
}
