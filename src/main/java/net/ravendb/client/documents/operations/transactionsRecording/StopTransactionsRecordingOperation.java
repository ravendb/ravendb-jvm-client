package net.ravendb.client.documents.operations.transactionsRecording;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

public class StopTransactionsRecordingOperation implements IVoidMaintenanceOperation {
    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new StopTransactionsRecordingCommand();
    }

    private static class StopTransactionsRecordingCommand extends VoidRavenCommand {
        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/transactions/stop-recording";

            return new HttpPost();
        }
    }
}
