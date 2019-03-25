package net.ravendb.client.serverwide.operations.logs;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IServerOperation;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetLogsConfigurationOperation implements IServerOperation<GetLogsConfigurationResult> {
    @Override
    public RavenCommand<GetLogsConfigurationResult> getCommand(DocumentConventions conventions) {
        return new GetLogsConfigurationCommand();
    }

    private static class GetLogsConfigurationCommand extends RavenCommand<GetLogsConfigurationResult> {

        public GetLogsConfigurationCommand() {
            super(GetLogsConfigurationResult.class);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/logs/configuration";

            return new HttpGet();
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
