package net.ravendb.client.documents.operations.sorters;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

public class DeleteSorterOperation implements IVoidMaintenanceOperation {
    private final String _sorterName;

    public DeleteSorterOperation(String sorterName) {
        if (sorterName == null) {
            throw new IllegalArgumentException("SorterName cannot be null");
        }

        _sorterName = sorterName;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new DeleteSorterCommand(_sorterName);
    }

    private static class DeleteSorterCommand extends VoidRavenCommand implements IRaftCommand {
        private final String _sorterName;

        public DeleteSorterCommand(String indexName) {
            if (indexName == null) {
                throw new IllegalArgumentException("IndexName cannot be null");
            }
            _sorterName = indexName;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/sorters?name=" + urlEncode(_sorterName);

            return new HttpDelete();
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
