package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexStats;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.ResultsResponse;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetIndexStatisticsOperation implements IMaintenanceOperation<IndexStats> {
    private final String _indexName;

    public GetIndexStatisticsOperation(String indexName) {
        if (indexName == null) {
            throw new IllegalArgumentException("IndexName cannot be null");
        }

        _indexName = indexName;
    }

    @Override
    public RavenCommand<IndexStats> getCommand(DocumentConventions conventions) {
        return new GetIndexStatisticsCommand(_indexName);
    }

    private static class GetIndexStatisticsCommand extends RavenCommand<IndexStats> {
        private final String _indexName;

        public GetIndexStatisticsCommand(String indexName) {
            super(IndexStats.class);
            if (indexName == null) {
                throw new IllegalArgumentException("IndexName cannot be null");
            }

            _indexName = indexName;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes/stats?name=" + UrlUtils.escapeDataString(_indexName);

            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            IndexStats[] results = mapper.readValue(response, ResultsResponse.GetIndexStatisticsResponse.class).getResults();
            if (results.length != 1) {
                throwInvalidResponse();
            }

            result = results[0];
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }
}
