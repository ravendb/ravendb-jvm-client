package net.ravendb.client.documents.operations.backups;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class UpdatePeriodicBackupOperation implements IMaintenanceOperation<UpdatePeriodicBackupOperationResult> {

    private final PeriodicBackupConfiguration _configuration;

    public UpdatePeriodicBackupOperation(PeriodicBackupConfiguration configuration) {
        _configuration = configuration;
    }

    @Override
    public RavenCommand<UpdatePeriodicBackupOperationResult> getCommand(DocumentConventions conventions) {
        return new UpdatePeriodicBackupCommand(conventions, _configuration);
    }

    private static class UpdatePeriodicBackupCommand extends RavenCommand<UpdatePeriodicBackupOperationResult> {
        private final DocumentConventions _conventions;
        private final PeriodicBackupConfiguration _configuration;

        public UpdatePeriodicBackupCommand(DocumentConventions conventions, PeriodicBackupConfiguration configuration) {
            super(UpdatePeriodicBackupOperationResult.class);

            _conventions = conventions;
            _configuration = configuration;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/periodic-backup";

            HttpPost request = new HttpPost();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_configuration);
                    generator.writeTree(config);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

            return request;
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
