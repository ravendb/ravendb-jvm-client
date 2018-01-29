package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.DatabaseRecord;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetDatabaseRecordOperation implements IServerOperation<DatabaseRecord> {
    private final String _database;

    public GetDatabaseRecordOperation(String database) {
        _database = database;
    }

    public RavenCommand<DatabaseRecord> getCommand(DocumentConventions conventions) {
        return new GetDatabaseRecordCommand(conventions, _database);
    }


    private static class GetDatabaseRecordCommand extends RavenCommand<DatabaseRecord> {
        private final DocumentConventions _conventions;
        private final String _database;

        @Override
        public boolean isReadRequest() {
            return false;
        }

        public GetDatabaseRecordCommand(DocumentConventions conventions, String database) {
            super(DatabaseRecord.class);
            _conventions = conventions;
            _database = database;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/databases?name=" + _database;
            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                result = null;
                return;
            }

            result = mapper.readValue(response, resultClass);
        }
    }
}
