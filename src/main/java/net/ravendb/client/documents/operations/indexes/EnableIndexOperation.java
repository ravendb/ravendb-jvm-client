package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.util.RaftIdGenerator;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

/**
 * Enables an index using the {@code EnableIndexOperation}. Once enabled, the index will begin indexing new data.
 *
 * <p><strong>Scope:</strong> The index can be enabled in two ways:</p>
 * <ul>
 *     <li>On a single node.</li>
 *     <li>Cluster-wide, across all database-group nodes.</li>
 * </ul>
 */
public class EnableIndexOperation implements IVoidMaintenanceOperation {

    private final String _indexName;
    private final boolean _clusterWide;

    /**
     * Inherits documentation from {@link EnableIndexOperation}.
     *
     * @param indexName The name of the index to be enabled.
     */
    public EnableIndexOperation(String indexName) {
        this(indexName, false);
    }
    /**
     * Inherits documentation from {@link EnableIndexOperation}.
     *
     * @param indexName  The name of the index to be enabled.
     * @param clusterWide A boolean value indicating whether the index should be enabled cluster-wide across all database-group nodes.
     */
    public EnableIndexOperation(String indexName, boolean clusterWide) {
        if (indexName == null) {
            throw new IllegalArgumentException("IndexName cannot be null");
        }

        _indexName = indexName;
        _clusterWide = clusterWide;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new EnableIndexOperation.EnableIndexCommand(_indexName, _clusterWide);
    }

    private static class EnableIndexCommand extends VoidRavenCommand implements IRaftCommand {
        private final String _indexName;
        private final boolean _clusterWide;

        public EnableIndexCommand(String indexName, boolean clusterWide) {
            if (indexName == null) {
                throw new IllegalArgumentException("IndexName cannot be null");
            }

            _indexName = indexName;
            _clusterWide = clusterWide;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl()
                    + "/databases/" + node.getDatabase()
                    + "/admin/indexes/enable?name=" + UrlUtils.escapeDataString(_indexName)
                    + "&clusterWide=" + _clusterWide;

            return new HttpPost(url);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
