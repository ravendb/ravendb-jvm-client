package net.ravendb.client.documents.operations.refresh;

import com.fasterxml.jackson.core.JsonGenerator;
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

public class ConfigureRefreshOperation implements IMaintenanceOperation<ConfigureRefreshOperationResult> {

    private final RefreshConfiguration _configuration;

    public ConfigureRefreshOperation(RefreshConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        _configuration = configuration;
    }

    @Override
    public RavenCommand<ConfigureRefreshOperationResult> getCommand(DocumentConventions conventions) {
        return new ConfigureRefreshCommand(conventions, _configuration);
    }

    private static class ConfigureRefreshCommand extends RavenCommand<ConfigureRefreshOperationResult> implements IRaftCommand {
        private final RefreshConfiguration _configuration;
        private final DocumentConventions _conventions;

        public ConfigureRefreshCommand(DocumentConventions conventions, RefreshConfiguration configuration) {
            super(ConfigureRefreshOperationResult.class);
            _configuration = configuration;
            _conventions = conventions;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/refresh/config";

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _configuration);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, ConfigureRefreshOperationResult.class);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
