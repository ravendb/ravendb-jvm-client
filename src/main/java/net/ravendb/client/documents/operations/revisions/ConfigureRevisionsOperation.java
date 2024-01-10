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

public class ConfigureRevisionsOperation implements IMaintenanceOperation<ConfigureRevisionsOperation.ConfigureRevisionsOperationResult> {

    private final RevisionsConfiguration _configuration;

    public ConfigureRevisionsOperation(RevisionsConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        _configuration = configuration;
    }

    @Override
    public RavenCommand<ConfigureRevisionsOperationResult> getCommand(DocumentConventions conventions) {
        return new ConfigureRevisionsCommand(conventions, _configuration);
    }

    private static class ConfigureRevisionsCommand extends RavenCommand<ConfigureRevisionsOperationResult> implements IRaftCommand {
        private final RevisionsConfiguration _configuration;
        private final DocumentConventions _conventions;

        public ConfigureRevisionsCommand(DocumentConventions conventions, RevisionsConfiguration configuration) {
            super(ConfigureRevisionsOperationResult.class);
            _configuration = configuration;
            _conventions = conventions;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/revisions/config";

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode config = mapper.valueToTree(_configuration);
                    generator.writeTree(config);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, ConfigureRevisionsOperationResult.class);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }

    public static class ConfigureRevisionsOperationResult {
        private Long raftCommandIndex;

        public Long getRaftCommandIndex() {
            return raftCommandIndex;
        }

        public void setRaftCommandIndex(Long raftCommandIndex) {
            this.raftCommandIndex = raftCommandIndex;
        }
    }
}
