package net.ravendb.client.documents.operations.indexes;

import com.fasterxml.jackson.databind.node.ArrayNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexErrors;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetIndexErrorsOperation implements IMaintenanceOperation<IndexErrors[]> {

    private final String[] _indexNames;

    public GetIndexErrorsOperation() {
        _indexNames = null;
    }

    public GetIndexErrorsOperation(String[] indexNames) {
        _indexNames = indexNames;
    }

    @Override
    public RavenCommand<IndexErrors[]> getCommand(DocumentConventions conventions) {
        return new GetIndexErrorsCommand(_indexNames);
    }

    private static class GetIndexErrorsCommand extends RavenCommand<IndexErrors[]> {
        private final String[] _indexNames;

        public GetIndexErrorsCommand(String[] indexNames) {
            super(IndexErrors[].class);
            _indexNames = indexNames;
        }

        @SuppressWarnings("StringConcatenationInLoop")
        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes/errors";

            if (_indexNames != null && _indexNames.length > 0) {
                url += "?";

                for (String indexName : _indexNames) {
                    url += "&name=" + urlEncode(indexName);
                }
            }

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
                return;
            }
            ArrayNode results = (ArrayNode)mapper.readTree(response).get("Results");
            result = new IndexErrors[results.size()];
            for (int i = 0; i < results.size(); i++) {
                result[i] = mapper.convertValue(results.get(i), IndexErrors.class);
            }
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }
}
