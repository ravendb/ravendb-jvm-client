package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

/**
 * Resumes indexing for a specific index using the {@code StartIndexOperation}.
 * This operation is typically used after an index has been paused with StopIndexOperation.
 *
 * <p><strong>Note:</strong> The index is resumed only on the preferred node, not across all database-group nodes.</p>
 */
public class StartIndexOperation implements IVoidMaintenanceOperation {

    private final String _indexName;
    /**
     * Inherits documentation from {@link StartIndexOperation}.
     *
     * @param indexName The name of the index to resume indexing for.
     */
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
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/indexes/start?name=" + UrlUtils.escapeDataString(_indexName);

            return new HttpPost(url);
        }
    }

}
