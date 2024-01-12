package net.ravendb.client.documents.operations.dataArchival;

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

public class ConfigureDataArchivalOperation implements IMaintenanceOperation<ConfigureDataArchivalOperationResult> {
    private final DataArchivalConfiguration _configuration;

    public ConfigureDataArchivalOperation(DataArchivalConfiguration configuration) {
        _configuration = configuration;
    }

    @Override
    public RavenCommand<ConfigureDataArchivalOperationResult> getCommand(DocumentConventions conventions) {
        return new ConfigureDataArchivalCommand(conventions, _configuration);
    }

    private static class ConfigureDataArchivalCommand extends RavenCommand<ConfigureDataArchivalOperationResult> implements IRaftCommand {
        private final DocumentConventions _conventions;
        private final DataArchivalConfiguration _configuration;

        public ConfigureDataArchivalCommand(DocumentConventions conventions, DataArchivalConfiguration configuration) {
            super(ConfigureDataArchivalOperationResult.class);

            _conventions = conventions;
            _configuration = configuration;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/data-archival/config";

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

            result = mapper.readValue(response, resultClass);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
