package net.ravendb.client.documents.operations.revisions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

/**
 * Configure the revisions-bin cleaner which cleans the revisions bin automatically when enabled.
 */
public final class ConfigureRevisionsBinCleanerOperation implements IMaintenanceOperation<ConfigureRevisionsBinCleanerOperationResult> {

    private final RevisionsBinConfiguration _configuration;

    /**
     * Configure the revisions-bin cleaner which cleans the revisions bin automatically when enabled.
     * @param configuration The configuration for the revisions bin cleaner. This parameter cannot be null.
     */
    public ConfigureRevisionsBinCleanerOperation(RevisionsBinConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration cannot be null");
        }
        _configuration = configuration;
    }

    @Override
    public RavenCommand<ConfigureRevisionsBinCleanerOperationResult> getCommand(DocumentConventions conventions) {
        return new ConfigureRevisionsBinCleanerCommand(conventions, _configuration);
    }

    private static class ConfigureRevisionsBinCleanerCommand extends RavenCommand<ConfigureRevisionsBinCleanerOperationResult> implements IRaftCommand {
        private final DocumentConventions _conventions;
        private final RevisionsBinConfiguration _configuration;

        public ConfigureRevisionsBinCleanerCommand(DocumentConventions conventions, RevisionsBinConfiguration configuration) {
            super(ConfigureRevisionsBinCleanerOperationResult.class);

            if (conventions == null) {
                throw new IllegalArgumentException("conventions cannot be null");
            }
            if (configuration == null) {
                throw new IllegalArgumentException("configuration cannot be null");
            }

            _conventions = conventions;
            _configuration = configuration;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/revisions/bin-cleaner/config";

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_configuration);
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

            result = mapper.readValue(response, ConfigureRevisionsBinCleanerOperationResult.class);
        }

        private final String raftUniqueRequestId = RaftIdGenerator.newId();

        @Override
        public String getRaftUniqueRequestId() {
            return raftUniqueRequestId;
        }
    }
}
