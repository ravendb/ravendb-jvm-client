package net.ravendb.client.documents.operations.backups;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.OperationIdResult;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IServerOperation;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class RestoreBackupOperation implements IServerOperation<OperationIdResult> {

    private final RestoreBackupConfiguration _restoreConfiguration;

    public RestoreBackupOperation(RestoreBackupConfiguration restoreConfiguration) {
        _restoreConfiguration = restoreConfiguration;
    }

    @Override
    public RavenCommand<OperationIdResult> getCommand(DocumentConventions conventions) {
        return new RestoreBackupCommand(conventions, _restoreConfiguration);
    }

    private static class RestoreBackupCommand extends RavenCommand<OperationIdResult> {
        private final DocumentConventions _conventions;
        private final RestoreBackupConfiguration _restoreConfiguration;

        public RestoreBackupCommand(DocumentConventions conventions, RestoreBackupConfiguration restoreConfiguration) {
            super(OperationIdResult.class);
            _conventions = conventions;
            _restoreConfiguration = restoreConfiguration;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/restore/database";

            HttpPost request = new HttpPost();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_restoreConfiguration);
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
