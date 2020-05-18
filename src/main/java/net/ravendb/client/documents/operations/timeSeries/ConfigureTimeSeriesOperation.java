package net.ravendb.client.documents.operations.timeSeries;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

public class ConfigureTimeSeriesOperation implements IMaintenanceOperation<ConfigureTimeSeriesOperationResult> {

    private final TimeSeriesConfiguration _configuration;

    public ConfigureTimeSeriesOperation(TimeSeriesConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }

        _configuration = configuration;
    }

    @Override
    public RavenCommand<ConfigureTimeSeriesOperationResult> getCommand(DocumentConventions conventions) {
        return new ConfigureTimeSeriesCommand(_configuration);
    }

    private static class ConfigureTimeSeriesCommand extends RavenCommand<ConfigureTimeSeriesOperationResult> implements IRaftCommand {
        private final TimeSeriesConfiguration _configuration;

        public ConfigureTimeSeriesCommand(TimeSeriesConfiguration configuration) {
            super(ConfigureTimeSeriesOperationResult.class);

            _configuration = configuration;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/timeseries/config";

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

            result = mapper.readValue(response, ConfigureTimeSeriesOperationResult.class);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
