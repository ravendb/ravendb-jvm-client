package net.ravendb.client.serverwide.operations.configuration;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.ResultsResponse;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.serverwide.operations.IServerOperation;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetServerWideBackupConfigurationsOperation implements IServerOperation<ServerWideBackupConfiguration[]> {
    @Override
    public RavenCommand<ServerWideBackupConfiguration[]> getCommand(DocumentConventions conventions) {
        return new GetServerWideBackupConfigurationsCommand();
    }

    private static class GetServerWideBackupConfigurationsCommand extends RavenCommand<ServerWideBackupConfiguration[]> {

        public GetServerWideBackupConfigurationsCommand() {
            super(ServerWideBackupConfiguration[].class);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/configuration/server-wide/tasks?type=Backup";

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }

            result = mapper.readValue(response, ResultsResponse.GetServerWideBackupConfigurationsResponse.class).getResults();
        }
    }
}
