package net.ravendb.client.serverwide.operations;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetServerWideOperationStateOperation implements IServerOperation<ObjectNode> {

    private final long _id;

    public GetServerWideOperationStateOperation(long id) {
        _id = id;
    }

    @Override
    public RavenCommand<ObjectNode> getCommand(DocumentConventions conventions) {
        return new GetServerWideOperationStateCommand(DocumentConventions.defaultConventions, _id);
    }

    public static class GetServerWideOperationStateCommand extends RavenCommand<ObjectNode> {
        private final long _id;
        private final DocumentConventions _conventions;

        public GetServerWideOperationStateCommand(DocumentConventions conventions, long id) {
            super(ObjectNode.class);

            this._conventions = conventions;
            this._id = id;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/operations/state?id=" + _id;

            return new HttpGet();
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @SuppressWarnings("UnnecessaryLocalVariable")
        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return ;
            }
            ObjectNode node = (ObjectNode) JsonExtensions.getDefaultMapper().readTree(response);
            result = node;
        }
    }

}
