package net.ravendb.client.documents.operations.backups;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.OperationIdResult;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.operations.IServerOperation;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class RestoreBackupOperation implements IServerOperation<OperationIdResult> {

    private final RestoreBackupConfigurationBase _restoreConfiguration;
    private final String _nodeTag;

    public RestoreBackupOperation(RestoreBackupConfigurationBase restoreConfiguration) {
        _restoreConfiguration = restoreConfiguration;
        _nodeTag = null;
    }

    public RestoreBackupOperation(RestoreBackupConfigurationBase restoreConfiguration, String nodeTag) {
        _restoreConfiguration = restoreConfiguration;
        _nodeTag = nodeTag;
    }

    @Override
    public RavenCommand<OperationIdResult> getCommand(DocumentConventions conventions) {
        return new RestoreBackupCommand(conventions, _restoreConfiguration, _nodeTag);
    }

    public String getNodeTag() {
        return _nodeTag;
    }

    private static class RestoreBackupCommand extends RavenCommand<OperationIdResult> {
        private final DocumentConventions _conventions;
        private final RestoreBackupConfigurationBase _restoreConfiguration;

        public RestoreBackupCommand(DocumentConventions conventions, RestoreBackupConfigurationBase restoreConfiguration) {
            this(conventions, restoreConfiguration, null);
        }

        public RestoreBackupCommand(DocumentConventions conventions, RestoreBackupConfigurationBase restoreConfiguration, String nodeTag) {
            super(OperationIdResult.class);

            if (restoreConfiguration == null) {
                throw new IllegalArgumentException("RestoreConfiguration cannot be null");
            }

            _conventions = conventions;
            _restoreConfiguration = restoreConfiguration;
            selectedNodeTag = nodeTag;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/restore/database";

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_restoreConfiguration);
                    generator.writeTree(config);
                }
            }, ContentType.APPLICATION_JSON, _conventions));
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
