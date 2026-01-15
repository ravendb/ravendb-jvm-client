package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

/**
 * Resumes indexing for the entire database using the {@code StartIndexingOperation}.
 * This operation is typically used after indexing has been paused or disabled.
 */
public class StartIndexingOperation implements IVoidMaintenanceOperation {
    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new StartIndexingCommand();
    }

    private static class StartIndexingCommand extends VoidRavenCommand {
        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/indexes/start";

            return new HttpPost(url);
        }
    }
}