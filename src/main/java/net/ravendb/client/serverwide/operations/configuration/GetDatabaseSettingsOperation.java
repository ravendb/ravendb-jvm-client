package net.ravendb.client.serverwide.operations.configuration;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetDatabaseSettingsOperation implements IMaintenanceOperation<DatabaseSettings> {

    private final String _databaseName;

    public GetDatabaseSettingsOperation(String databaseName) {
        if (databaseName == null) {
            throw new IllegalArgumentException("DatabaseName cannot be null");
        }
        _databaseName = databaseName;
    }

    @Override
    public RavenCommand<DatabaseSettings> getCommand(DocumentConventions conventions) {
        return new GetDatabaseSettingsCommand(_databaseName);
    }

    private static class GetDatabaseSettingsCommand extends RavenCommand<DatabaseSettings> {

        private final String _databaseName;

        public GetDatabaseSettingsCommand(String databaseName) {
            super(DatabaseSettings.class);

            if (databaseName == null) {
                throw new IllegalArgumentException("DatabaseName cannot be null");
            }
            _databaseName = databaseName;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + _databaseName + "/admin/record";
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
