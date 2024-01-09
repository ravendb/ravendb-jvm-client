package net.ravendb.client.documents.operations.refresh;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

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
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/refresh/config";

            HttpPost request = new HttpPost();
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _configuration);
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

            result = mapper.readValue(response, ConfigureRefreshOperationResult.class);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
