package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexResetMode;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.HttpReset;
import net.ravendb.client.primitives.SharpEnum;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

public class ResetIndexOperation implements IVoidMaintenanceOperation {

    private final String _indexName;
    private final IndexResetMode _indexResetMode;

    public ResetIndexOperation(String indexName) {
        this(indexName, null);
    }

    /**
     * @param indexName name of the index to reset
     * @param indexResetMode how to reset the index; when null, the server's configured reset mode is used
     */
    public ResetIndexOperation(String indexName, IndexResetMode indexResetMode) {
        if (indexName == null) {
            throw new IllegalArgumentException("Index name cannot be null");
        }

        _indexName = indexName;
        _indexResetMode = indexResetMode;
    }

    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new ResetIndexOperation.ResetIndexCommand(_indexName, _indexResetMode);
    }

    private static class ResetIndexCommand extends VoidRavenCommand {
        private final String _indexName;
        private final IndexResetMode _indexResetMode;

        public ResetIndexCommand(String indexName, IndexResetMode indexResetMode) {
            if (indexName == null) {
                throw new IllegalArgumentException("Index name cannot be null");
            }

            _indexName = indexName;
            _indexResetMode = indexResetMode;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes?name=" + UrlUtils.escapeDataString(_indexName);

            if (_indexResetMode != null) {
                url += "&mode=" + SharpEnum.value(_indexResetMode);
            }

            return new HttpReset(url);
        }
    }

}
