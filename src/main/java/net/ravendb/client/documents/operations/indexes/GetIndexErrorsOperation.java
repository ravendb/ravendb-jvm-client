package net.ravendb.client.documents.operations.indexes;

import com.fasterxml.jackson.databind.node.ArrayNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexErrors;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

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
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes/errors";

            if (_indexNames != null && _indexNames.length > 0) {
                url.value += "?";

                for (String indexName : _indexNames) {
                    url.value += "&name=" + indexName;
                }
            }

            return new HttpGet();
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
