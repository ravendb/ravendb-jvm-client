package net.ravendb.client.serverwide.operations.sorters;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

public class DeleteServerWideSorterOperation implements IVoidServerOperation {
    private final String _sorterName;

    public DeleteServerWideSorterOperation(String sorterName) {
        if (sorterName == null) {
            throw new IllegalArgumentException("SorterName cannot be null");
        }

        _sorterName = sorterName;
    }

    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new DeleteServerWideSorterCommand(_sorterName);
    }

    private static class DeleteServerWideSorterCommand extends VoidRavenCommand implements IRaftCommand {
        private final String _sorterName;

        public DeleteServerWideSorterCommand(String sorterName) {
            if (sorterName == null) {
                throw new IllegalArgumentException("SorterName cannot be null");
            }

            _sorterName = sorterName;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/sorters?name=" + urlEncode(_sorterName);

            return new HttpDelete(url);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
