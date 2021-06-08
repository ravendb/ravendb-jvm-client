package net.ravendb.client.serverwide.operations.sorters;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IVoidServerOperation;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

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
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admiin/sorters?name=" + urlEncode(_sorterName);

            return new HttpDelete();
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
