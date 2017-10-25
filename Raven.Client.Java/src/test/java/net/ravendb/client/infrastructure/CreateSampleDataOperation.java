package net.ravendb.client.infrastructure;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidAdminOperation;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

public class CreateSampleDataOperation implements IVoidAdminOperation {
    @Override
    public VoidRavenCommand getCommand(DocumentConventions conventions) {
        return new CreateSampleDataCommand();
    }


    private static class CreateSampleDataCommand extends VoidRavenCommand {
        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/studio/sample-data";

            return new HttpPost();
        }
    }
}
