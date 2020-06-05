package net.ravendb.client.documents.operations.timeSeries;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class RemoveTimeSeriesPolicyOperation implements IMaintenanceOperation<ConfigureTimeSeriesOperationResult> {
    private final String _collection;
    private final String _name;

    public RemoveTimeSeriesPolicyOperation(String collection, String name) {
        if (collection == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("Collection cannot be null");
        }

        _collection = collection;
        _name = name;
    }

    @Override
    public RavenCommand<ConfigureTimeSeriesOperationResult> getCommand(DocumentConventions conventions) {
        return new RemoveTimeSeriesPolicyCommand(_collection, _name);
    }

    private static class RemoveTimeSeriesPolicyCommand extends RavenCommand<ConfigureTimeSeriesOperationResult>
        implements IRaftCommand {
        private final String _collection;
        private final String _name;

        public RemoveTimeSeriesPolicyCommand(String collection, String name) {
            super(ConfigureTimeSeriesOperationResult.class);
            _collection = collection;
            _name = name;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase()
                    + "/admin/timeseries/policy?collection=" + urlEncode(_collection) + "&name=" + urlEncode(_name);

            return new HttpDelete();
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
