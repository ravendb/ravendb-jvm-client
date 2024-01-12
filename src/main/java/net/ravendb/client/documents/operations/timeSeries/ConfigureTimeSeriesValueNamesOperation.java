package net.ravendb.client.documents.operations.timeSeries;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

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
        return new ConfigureTimeSeriesValueNamesCommand(conventions, _parameters);
    }

    private static class ConfigureTimeSeriesValueNamesCommand extends RavenCommand<ConfigureTimeSeriesOperationResult> implements IRaftCommand {
        private final Parameters _parameters;
        private final DocumentConventions _conventions;

        public ConfigureTimeSeriesValueNamesCommand(DocumentConventions conventions, Parameters parameters) {
            super(ConfigureTimeSeriesOperationResult.class);
            _conventions = conventions;
            _parameters = parameters;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/timeseries/names/config";

            HttpPost request = new HttpPost(url);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _parameters);
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
