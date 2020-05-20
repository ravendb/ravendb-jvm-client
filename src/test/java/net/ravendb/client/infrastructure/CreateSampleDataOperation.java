package net.ravendb.client.infrastructure;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

public class CreateSampleDataOperation implements IVoidMaintenanceOperation {
    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new CreateSampleDataCommand();
    }

    private static class CreateSampleDataCommand extends VoidRavenCommand implements IRaftCommand {
        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/studio/sample-data";

            return new HttpPost();
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
