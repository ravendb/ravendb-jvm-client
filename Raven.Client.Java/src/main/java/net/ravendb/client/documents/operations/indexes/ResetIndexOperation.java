package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.HttpReset;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.client.methods.HttpRequestBase;

public class ResetIndexOperation implements IVoidMaintenanceOperation {

    private final String _indexName;

    public ResetIndexOperation(String indexName) {
        if (indexName == null) {
            throw new IllegalArgumentException("Index name cannot be null");
        }

        _indexName = indexName;
    }

    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new ResetIndexOperation.ResetIndexCommand(_indexName);
    }

    private static class ResetIndexCommand extends VoidRavenCommand {
        private final String _indexName;

        public ResetIndexCommand(String indexName) {
            if (indexName == null) {
                throw new IllegalArgumentException("Index name cannot be null");
            }

            _indexName = indexName;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes?name=" + UrlUtils.escapeDataString(_indexName);

            return new HttpReset();
        }
    }

}
