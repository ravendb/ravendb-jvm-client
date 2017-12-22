package net.ravendb.client.documents.operations.indexes;


import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

public class StartIndexOperation implements IVoidMaintenanceOperation {

    private final String _indexName;

    public StartIndexOperation(String indexName) {
        if (indexName == null) {
            throw new IllegalArgumentException("Index name cannot be null");
        }

        _indexName = indexName;
    }

    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new StartIndexCommand(_indexName);
    }

    private static class StartIndexCommand extends VoidRavenCommand {
        private final String _indexName;

        public StartIndexCommand(String indexName) {
            if (indexName == null) {
                throw new IllegalArgumentException("Index name cannot be null");
            }

            _indexName = indexName;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/indexes/start?name=" + UrlUtils.escapeDataString(_indexName);

            return new HttpPost();
        }
    }

}
