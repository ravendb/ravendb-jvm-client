package net.ravendb.client.documents.operations.timeSeries;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

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
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/timeseries/stats?docId=" + urlEncode(_documentId);

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                result = null;
                return;
            }
            result = mapper.readValue(response, resultClass);
        }
    }
}
