package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

public class DeleteIndexErrorsOperation implements IVoidMaintenanceOperation {

    private final String[] _indexNames;

    public DeleteIndexErrorsOperation() {
        _indexNames = null;
    }

    public DeleteIndexErrorsOperation(String[] indexNames) {
        _indexNames = indexNames;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new DeleteIndexErrorsCommand(_indexNames);
    }

    private static class DeleteIndexErrorsCommand extends VoidRavenCommand {
        private final String[] _indexNames;

        public DeleteIndexErrorsCommand(String[] indexNames) {
            _indexNames = indexNames;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes/errors";

            if (_indexNames != null && _indexNames.length > 0) {
                url.value += "?";

                for (String indexName : _indexNames) {
                    url.value += "&name=" + urlEncode(indexName);
                }
            }

            return new HttpDelete();
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }
    }
}
