package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
/**
 * Pauses a single index in the database using the {@code StopIndexOperation}.
 * A paused index performs no indexing on the node it is paused for, but continues indexing new data on database-group nodes where the index is not paused.
 * Although a paused index can still be queried, results may be stale when querying the node where the index is paused.
 *
 * <p><strong>Notes:</strong></p>
 * <ul>
 *     <li>The index will be paused only on the preferred node, not across all database-group nodes.</li>
 *     <li>To pause indexing for all indexes in the database, use the StopIndexingOperation.</li>
 * </ul>
 */
public class StopIndexOperation implements IVoidMaintenanceOperation {

    private final String _indexName;
    /**
     * Inherits documentation from {@link StopIndexOperation}.
     *
     * @param indexName The name of the index to be paused.
     */
    public StopIndexOperation(String indexName) {
        if (indexName == null) {
            throw new IllegalArgumentException("Index name cannot be null");
        }

        _indexName = indexName;
    }

    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new StopIndexCommand(_indexName);
    }

    private static class StopIndexCommand extends VoidRavenCommand {
        private final String _indexName;

        public StopIndexCommand(String indexName) {
            if (indexName == null) {
                throw new IllegalArgumentException("Index name cannot be null");
            }

            _indexName = indexName;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/indexes/stop?name=" + UrlUtils.escapeDataString(_indexName);

            return new HttpPost(url);
        }
    }

}
