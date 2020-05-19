package net.ravendb.client.documents.operations.timeSeries;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetTimeSeriesStatisticsOperation implements IOperation<TimeSeriesStatistics> {

    private final String _documentId;

    /**
     * Retrieve start, end and total number of entries for all time-series of a given document
     * @param documentId Document id
     */
    public GetTimeSeriesStatisticsOperation(String documentId) {
        _documentId = documentId;
    }

    @Override
    public RavenCommand<TimeSeriesStatistics> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new GetTimeSeriesStatisticsCommand(_documentId);
    }

    private static class GetTimeSeriesStatisticsCommand extends RavenCommand<TimeSeriesStatistics> {
        private final String _documentId;

        public GetTimeSeriesStatisticsCommand(String documentId) {
            super(TimeSeriesStatistics.class);
            _documentId = documentId;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/timeseries/stats?docId=" + urlEncode(_documentId);

            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = mapper.readValue(response, resultClass);
        }
    }
}
