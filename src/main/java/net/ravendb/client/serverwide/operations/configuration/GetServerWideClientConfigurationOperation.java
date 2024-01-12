package net.ravendb.client.serverwide.operations.configuration;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.configuration.ClientConfiguration;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.serverwide.operations.IServerOperation;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetServerWideClientConfigurationOperation implements IServerOperation<ClientConfiguration> {
    @Override
    public RavenCommand<ClientConfiguration> getCommand(DocumentConventions conventions) {
        return new GetServerWideClientConfigurationCommand();
    }

    private static class GetServerWideClientConfigurationCommand extends RavenCommand<ClientConfiguration> {

        public GetServerWideClientConfigurationCommand() {
            super(ClientConfiguration.class);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/configuration/client";

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }
    }

}
