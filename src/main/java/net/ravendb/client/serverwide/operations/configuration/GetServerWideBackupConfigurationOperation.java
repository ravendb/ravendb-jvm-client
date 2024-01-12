package net.ravendb.client.serverwide.operations.configuration;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.ResultsResponse;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.serverwide.operations.IServerOperation;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetServerWideBackupConfigurationOperation implements IServerOperation<ServerWideBackupConfiguration> {

    private final String _name;

    public GetServerWideBackupConfigurationOperation(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name cannot be null");
        }

        _name = name;
    }

    @Override
    public RavenCommand<ServerWideBackupConfiguration> getCommand(DocumentConventions conventions) {
        return new GetServerWideBackupConfigurationCommand(_name);
    }

    private static class GetServerWideBackupConfigurationCommand extends RavenCommand<ServerWideBackupConfiguration> {
        private final String _name;

        public GetServerWideBackupConfigurationCommand(String name) {
            super(ServerWideBackupConfiguration.class);

            if (name == null) {
                throw new IllegalArgumentException("Name cannot be null");
            }

            _name = name;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/configuration/server-wide/tasks?type=Backup&name=" + urlEncode(_name);

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }

            ServerWideBackupConfiguration[] results = mapper.readValue(response, ResultsResponse.GetServerWideBackupConfigurationsResponse.class).getResults();
            if (results.length == 0) {
                return;
            }

            if (results.length > 1) {
                throwInvalidResponse();
            }

            result = results[0];
        }
    }
}
