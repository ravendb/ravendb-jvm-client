package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.ResultsResponse;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetIndexesOperation implements IMaintenanceOperation<IndexDefinition[]> {

    private final int _start;
    private final int _pageSize;

    public GetIndexesOperation(int start, int pageSize) {
        _start = start;
        _pageSize = pageSize;
    }

    @Override
    public RavenCommand<IndexDefinition[]> getCommand(DocumentConventions conventions) {
        return new GetIndexesCommand(_start, _pageSize);
    }

    private static class GetIndexesCommand extends RavenCommand<IndexDefinition[]> {
        private final int _start;
        private final int _pageSize;

        public GetIndexesCommand(int start, int pageSize) {
            super(IndexDefinition[].class);
            _start = start;
            _pageSize = pageSize;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes?start=" + _start + "&pageSize=" + _pageSize;

            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, ResultsResponse.GetIndexesResponse.class).getResults();
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }
}

