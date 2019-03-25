package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class PromoteDatabaseNodeOperation implements IServerOperation<DatabasePutResult> {

    private final String _databaseName;
    private final String _node;

    public PromoteDatabaseNodeOperation(String databaseName, String node) {
        _databaseName = databaseName;
        _node = node;
    }

    @Override
    public RavenCommand<DatabasePutResult> getCommand(DocumentConventions conventions) {
        return new PromoteDatabaseNodeCommand(_databaseName, _node);
    }

    private static class PromoteDatabaseNodeCommand extends RavenCommand<DatabasePutResult> {
        private final String _databaseName;
        private final String _node;

        public PromoteDatabaseNodeCommand(String databaseName, String node) {
            super(DatabasePutResult.class);

            if (StringUtils.isEmpty(databaseName)) {
                throw new IllegalArgumentException("DatabaseName cannot be null");
            }

            if (StringUtils.isEmpty(node)) {
                throw new IllegalArgumentException("Node cannot be null");
            }

            _databaseName = databaseName;
            _node = node;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/databases/promote?name=" + _databaseName + "&node=" + _node;

            return new HttpPost();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }
    }
}
