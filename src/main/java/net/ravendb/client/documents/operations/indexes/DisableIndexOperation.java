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

public class DisableIndexOperation implements IVoidMaintenanceOperation {

    private final String _indexName;
    private final boolean _clusterWide;

    public DisableIndexOperation(String indexName) {
        this(indexName, false);
    }

    public DisableIndexOperation(String indexName, boolean clusterWide) {
        if (indexName == null) {
            throw new IllegalArgumentException("IndexName cannot be null");
        }

        _indexName = indexName;
        _clusterWide = clusterWide;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new DisableIndexCommand(_indexName, _clusterWide);
    }

    private static class DisableIndexCommand extends VoidRavenCommand implements IRaftCommand {
        private final String _indexName;
        private final boolean _clusterWide;

        public DisableIndexCommand(String indexName, boolean clusterWide) {
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
                    + "/admin/indexes/disable?name=" + UrlUtils.escapeDataString(_indexName)
                    + "&clusterWide=" + _clusterWide;

            return new HttpPost(url);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
