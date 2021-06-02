package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.time.Duration;

public class GetOperationStateOperation implements IMaintenanceOperation<ObjectNode> {

    private final long _id;
    private final String _nodeTag;

    public GetOperationStateOperation(long id) {
        _id = id;
        _nodeTag = null;
    }

    public GetOperationStateOperation(long id, String nodeTag) {
        _id = id;
        _nodeTag = nodeTag;
    }

    @Override
    public RavenCommand<ObjectNode> getCommand(DocumentConventions conventions) {
        return new GetOperationStateCommand(_id, _nodeTag);
    }

    public static class GetOperationStateCommand extends RavenCommand<ObjectNode> {
        @Override
        public boolean isReadRequest() {
            return true;
        }

        private final long _id;

        public GetOperationStateCommand(long id) {
            this(id, null);
        }

        public GetOperationStateCommand(long id, String nodeTag) {
            super(ObjectNode.class);
            _id = id;
            selectedNodeTag = nodeTag;

            timeout = Duration.ofSeconds(15);
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/operations/state?id=" + _id;


            return new HttpGet();
        }

        @SuppressWarnings("UnnecessaryLocalVariable")
        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }

            ObjectNode node = (ObjectNode) JsonExtensions.getDefaultMapper().readTree(response);
            result = node;
        }
    }
}
