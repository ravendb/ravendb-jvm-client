package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class AddDatabaseNodeOperation implements IServerOperation<DatabasePutResult> {
    private final String _databaseName;
    private final String _node;

    public AddDatabaseNodeOperation(String databaseName) {
        this(databaseName, null);
    }

    public AddDatabaseNodeOperation(String databaseName, String node) {
        _databaseName = databaseName;
        _node = node;
    }

    @Override
    public RavenCommand<DatabasePutResult> getCommand(DocumentConventions conventions) {
        return new AddDatabaseNodeCommand(_databaseName, _node);
    }

    private static class AddDatabaseNodeCommand extends RavenCommand<DatabasePutResult>
    {
        private final String _databaseName;
        private final String _node;


        public AddDatabaseNodeCommand(String databaseName, String node) {
            super(DatabasePutResult.class);

            if (StringUtils.isBlank(databaseName)) {
                throw new IllegalArgumentException("DatabaseName cannot be null");
            }

            _databaseName = databaseName;
            _node = node;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/databases/node?name=" + _databaseName;
            if (node != null) {
                url.value += "&node=" + _node;
            }

            return new HttpPut();
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
