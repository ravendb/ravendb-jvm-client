package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

/**
 * Pauses indexing for the entire database using the {@code StopIndexingOperation}.
 * If you need to stop a single index, use the StopIndexOperation instead.
 *
 * <p><strong>Note:</strong> Indexing will automatically resume after a server restart or by using the StartIndexingOperation.</p>
 */
public class StopIndexingOperation implements IVoidMaintenanceOperation {
    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new StopIndexingCommand();
    }

    private static class StopIndexingCommand extends VoidRavenCommand {
        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/indexes/stop";

            return new HttpPost(url);
        }
    }
}
