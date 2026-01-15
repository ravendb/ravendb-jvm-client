package net.ravendb.client.serverwide.operations.configuration;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

/**
 * Retrieves settings from the database record, including: Database topology, Ongoing task configurations, Index information, Revision settings,
 * and other relevant database configurations.
 */
public class GetDatabaseSettingsOperation implements IMaintenanceOperation<DatabaseSettings> {

    private final String _databaseName;

    /**
     * {@inheritDoc}
     * @see GetDatabaseSettingsOperation
     * @param databaseName The name of the database whose settings will be retrieved. Cannot be null.
     */
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
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + _databaseName + "/admin/record";
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
