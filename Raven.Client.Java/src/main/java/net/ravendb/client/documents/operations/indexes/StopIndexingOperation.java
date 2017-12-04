package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

public class StopIndexingOperation implements IVoidMaintenanceOperation {
    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new StopIndexingCommand();
    }

    private static class StopIndexingCommand extends VoidRavenCommand {
        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/indexes/stop";

            return new HttpPost();
        }
    }
}
