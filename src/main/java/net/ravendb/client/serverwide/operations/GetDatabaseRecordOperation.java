package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.serverwide.DatabaseRecordWithEtag;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetDatabaseRecordOperation implements IServerOperation<DatabaseRecordWithEtag> {
    private final String _database;

    public GetDatabaseRecordOperation(String database) {
        _database = database;
    }

    public RavenCommand<DatabaseRecordWithEtag> getCommand(DocumentConventions conventions) {
        return new GetDatabaseRecordCommand(_database);
    }

    private static class GetDatabaseRecordCommand extends RavenCommand<DatabaseRecordWithEtag> {
        private final String _database;

        @Override
        public boolean isReadRequest() {
            return false;
        }

        public GetDatabaseRecordCommand(String database) {
            super(DatabaseRecordWithEtag.class);
            _database = database;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/databases?name=" + _database;
            return new HttpGet(url);
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
