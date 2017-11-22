package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IServerOperation;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetOperationStateOperation implements IServerOperation<ObjectNode> {

    private final long _id;
    private final boolean _isServerStoreOperation;

    public GetOperationStateOperation(long id, boolean isServerStoreOperation) {
        _id = id;
        _isServerStoreOperation = isServerStoreOperation;
    }

    @Override
    public RavenCommand<ObjectNode> getCommand(DocumentConventions conventions) {
        return new GetOperationStateCommand(DocumentConventions.defaultConventions, _id, _isServerStoreOperation);
    }

    public static class GetOperationStateCommand extends RavenCommand<ObjectNode> {
        @Override
        public boolean isReadRequest() {
            return true;
        }

        private final DocumentConventions _conventions;
        private final long _id;
        private final boolean _isServerStoreOperation;

        public GetOperationStateCommand(DocumentConventions conventions, long id) {
            this(conventions, id, false);
        }

        public GetOperationStateCommand(DocumentConventions conventions, long id, boolean isServerStoreOperation) {
            super(ObjectNode.class);
            _conventions = conventions;
            _id = id;
            _isServerStoreOperation = isServerStoreOperation;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = !_isServerStoreOperation ?
                    node.getUrl() + "/databases/" + node.getDatabase() + "/operations/state?id=" + _id :
                    node.getUrl() + "/operations/state?id=" + _id;


            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }

            ObjectNode node = (ObjectNode) JsonExtensions.getDefaultMapper().readTree(response);
            result = (ObjectNode) _conventions.deserializeEntityFromJson(ObjectNode.class, node);
        }
    }

}
