package net.ravendb.client.documents.operations.revisions;

import com.fasterxml.jackson.annotation.JsonProperty;
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

public class ConfigureRevisionsOperation implements IMaintenanceOperation<ConfigureRevisionsOperation.ConfigureRevisionsOperationResult> {

    private final RevisionsConfiguration _configuration;

    public ConfigureRevisionsOperation(RevisionsConfiguration configuration) {
        _configuration = configuration;
    }

    @Override
    public RavenCommand<ConfigureRevisionsOperationResult> getCommand(DocumentConventions conventions) {
        return new ConfigureRevisionsCommand(_configuration);
    }

    private static class ConfigureRevisionsCommand extends RavenCommand<ConfigureRevisionsOperationResult> {
        private final RevisionsConfiguration _configuration;

        public ConfigureRevisionsCommand(RevisionsConfiguration configuration) {
            super(ConfigureRevisionsOperationResult.class);
            _configuration = configuration;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/revisions/config";

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

            result = mapper.readValue(response, ConfigureRevisionsOperationResult.class);
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
