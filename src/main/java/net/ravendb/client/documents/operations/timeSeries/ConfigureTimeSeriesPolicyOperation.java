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
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class ConfigureTimeSeriesPolicyOperation implements IMaintenanceOperation<ConfigureTimeSeriesOperationResult> {

    private final String _collection;
    private final TimeSeriesPolicy _config;

    public ConfigureTimeSeriesPolicyOperation(String collection, TimeSeriesPolicy config) {
        _collection = collection;
        _config = config;
    }

    @Override
    public RavenCommand<ConfigureTimeSeriesOperationResult> getCommand(DocumentConventions conventions) {
        return new ConfigureTimeSeriesPolicyCommand(_collection, _config);
    }

    private static class ConfigureTimeSeriesPolicyCommand extends RavenCommand<ConfigureTimeSeriesOperationResult> implements IRaftCommand {
        private final TimeSeriesPolicy _configuration;
        private final String _collection;

        public ConfigureTimeSeriesPolicyCommand(String collection, TimeSeriesPolicy configuration) {
            super(ConfigureTimeSeriesOperationResult.class);

            if (configuration == null) {
                throw new IllegalArgumentException("Configuration cannot be null");
            }

            if (collection == null) {
                throw new IllegalArgumentException("Collection cannot be null");
            }

            _configuration = configuration;
            _collection = collection;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/timeseries/policy?collection=" + urlEncode(_collection);

            HttpPut request = new HttpPut();

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

            result = mapper.readValue(response, resultClass);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
