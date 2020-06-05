package net.ravendb.client.documents.operations.timeSeries;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class ConfigureTimeSeriesValueNamesOperation implements IMaintenanceOperation<ConfigureTimeSeriesOperationResult> {
    private final Parameters _parameters;

    public ConfigureTimeSeriesValueNamesOperation(Parameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }
        _parameters = parameters;
        _parameters.validate();
    }

    @Override
    public RavenCommand<ConfigureTimeSeriesOperationResult> getCommand(DocumentConventions conventions) {
        return new ConfigureTimeSeriesValueNamesCommand(_parameters);
    }

    private static class ConfigureTimeSeriesValueNamesCommand extends RavenCommand<ConfigureTimeSeriesOperationResult> implements IRaftCommand {
        private final Parameters _parameters;

        public ConfigureTimeSeriesValueNamesCommand(Parameters parameters) {
            super(ConfigureTimeSeriesOperationResult.class);
            _parameters = parameters;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/timeseries/names/config";

            HttpPost request = new HttpPost();

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _parameters);
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

    public static class Parameters {
        private String collection;
        private String timeSeries;
        private String[] valueNames;
        private boolean update;

        public void validate() {
            if (StringUtils.isEmpty(collection)) {
                throw new IllegalArgumentException("Collection cannot be null or empty");
            }
            if (StringUtils.isEmpty(timeSeries)) {
                throw new IllegalArgumentException("TimeSeries cannot be null or empty");
            }
            if (valueNames == null || valueNames.length == 0) {
                throw new IllegalArgumentException("ValuesNames cannot be null or empty");
            }
        }

        public String getCollection() {
            return collection;
        }

        public void setCollection(String collection) {
            this.collection = collection;
        }

        public String getTimeSeries() {
            return timeSeries;
        }

        public void setTimeSeries(String timeSeries) {
            this.timeSeries = timeSeries;
        }

        public String[] getValueNames() {
            return valueNames;
        }

        public void setValueNames(String[] valueNames) {
            this.valueNames = valueNames;
        }

        public boolean isUpdate() {
            return update;
        }

        public void setUpdate(boolean update) {
            this.update = update;
        }
    }
}
