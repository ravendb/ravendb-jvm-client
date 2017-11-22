package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IAdminOperation;
import net.ravendb.client.documents.operations.OperationIdResult;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class CompactIndexOperation implements IAdminOperation<OperationIdResult> {

    private final String _indexName;

    public CompactIndexOperation(String indexName) {
        if (indexName == null) {
            throw new IllegalArgumentException("indexName cannot be null");
        }

        _indexName = indexName;
    }

    @Override
    public RavenCommand<OperationIdResult> getCommand(DocumentConventions conventions) {
        return new CompactIndexCommand(_indexName);
    }

    private static class CompactIndexCommand extends RavenCommand<OperationIdResult> {
        private final String _indexName;

        public CompactIndexCommand(String indexName) {
            super(OperationIdResult.class);
            _indexName = indexName;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/indexes/compact?name=" + UrlUtils.escapeDataString(_indexName);

            return new HttpPost();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, OperationIdResult.class);
        }
    }
}
