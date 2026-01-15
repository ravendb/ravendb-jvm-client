package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexResetMode;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.HttpReset;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

/**
 * Rebuilds an index using the {@code ResetIndexOperation}. This operation removes all existing indexed data
 * and re-indexes all items matched by the index definition.
 *
 * <p><strong>Indexes scope:</strong> Both static and auto indexes can be reset.</p>
 *
 * <p><strong>Nodes scope:</strong></p>
 * <ul>
 *     <li>When resetting an index from the client, the index is reset only on the preferred node, not across all database-group nodes.</li>
 *     <li>When resetting an index from the Studio indexes list view, the index is reset on the local node where the browser is opened, even if it is not the preferred node.</li>
 * </ul>
 *
 * <p>If the index is disabled or paused, resetting will return it to the normal running state
 * on the local node where the action was performed.</p>
 */
public class ResetIndexOperation implements IVoidMaintenanceOperation {

    private final String _indexName;
    private final IndexResetMode indexResetMode;
    /**
     * Inherits documentation from {@link ResetIndexOperation}.
     *
     * @param indexName The name of the index to be reset.
     */
    public ResetIndexOperation(String indexName) {
        this(indexName,null);
    }
    /**
     * Inherits documentation from {@link ResetIndexOperation}.
     *
     * @param indexName      The name of the index to be reset.
     * @param indexResetMode The mode to use when resetting the index. Valid values are InPlace and SideBySide.
     */
    public ResetIndexOperation(String indexName, IndexResetMode indexResetMode) {
        if (indexName == null) {
            throw new IllegalArgumentException("indexName cannot be null");
        }
        this._indexName = indexName;
        this.indexResetMode = indexResetMode;
    }

    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new ResetIndexOperation.ResetIndexCommand(_indexName, indexResetMode);
    }

    private static class ResetIndexCommand extends VoidRavenCommand {
        private final String _indexName;
        private final IndexResetMode indexResetMode;

        public ResetIndexCommand(String indexName, IndexResetMode indexResetMode, String nodeTag) {
            if (indexName == null) {
                throw new IllegalArgumentException("Index name cannot be null");
            }

            _indexName = indexName;
            this.indexResetMode = indexResetMode;
            this.selectedNodeTag = nodeTag;
        }

        public ResetIndexCommand(String indexName, IndexResetMode indexResetMode) {
            this(indexName, indexResetMode, null);
        }

        public ResetIndexCommand(String indexName) {
            this(indexName, null, null);
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes?name=" + UrlUtils.escapeDataString(_indexName);

            if (indexResetMode != null) {
                url += "&mode=" + indexResetMode.toString();
            }

            return new HttpReset(url);
        }
    }

}
