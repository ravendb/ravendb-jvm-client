package net.ravendb.client.documents.operations.sorters;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

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

        public DeleteSorterCommand(String sorterName) {
            if (sorterName == null) {
                throw new IllegalArgumentException("SorterName cannot be null");
            }
            _sorterName = sorterName;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/sorters?name=" + urlEncode(_sorterName);

            return new HttpDelete(url);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
