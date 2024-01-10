package net.ravendb.client.serverwide.operations;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetServerWideOperationStateOperation implements IServerOperation<ObjectNode> {

    private final long _id;

    public GetServerWideOperationStateOperation(long id) {
        _id = id;
    }

    @Override
    public RavenCommand<ObjectNode> getCommand(DocumentConventions conventions) {
        return new GetServerWideOperationStateCommand(_id);
    }

    public static class GetServerWideOperationStateCommand extends RavenCommand<ObjectNode> {
        private final long _id;

        public GetServerWideOperationStateCommand(long id) {
            this(id, null);
        }

        public GetServerWideOperationStateCommand(long id, String nodeTag) {
            super(ObjectNode.class);

            this._id = id;
            selectedNodeTag = nodeTag;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/operations/state?id=" + _id;

            return new HttpGet(url);
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
