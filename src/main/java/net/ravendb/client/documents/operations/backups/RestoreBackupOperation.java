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
        return new RestoreBackupCommand(_restoreConfiguration, _nodeTag);
    }

    public String getNodeTag() {
        return _nodeTag;
    }

    private static class RestoreBackupCommand extends RavenCommand<OperationIdResult> {
        private final RestoreBackupConfigurationBase _restoreConfiguration;

        public RestoreBackupCommand(RestoreBackupConfigurationBase restoreConfiguration) {
            this(restoreConfiguration, null);
        }

        public RestoreBackupCommand(RestoreBackupConfigurationBase restoreConfiguration, String nodeTag) {
            super(OperationIdResult.class);

            if (restoreConfiguration == null) {
                throw new IllegalArgumentException("RestoreConfiguration cannot be null");
            }

            _restoreConfiguration = restoreConfiguration;
            selectedNodeTag = nodeTag;
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
