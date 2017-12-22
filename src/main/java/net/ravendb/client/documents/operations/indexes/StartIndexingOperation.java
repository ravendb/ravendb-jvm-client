package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

public class StartIndexingOperation implements IVoidMaintenanceOperation {
    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new StartIndexingCommand();
    }

    private static class StartIndexingCommand extends VoidRavenCommand {
        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/indexes/start";

            return new HttpPost();
        }
    }
}