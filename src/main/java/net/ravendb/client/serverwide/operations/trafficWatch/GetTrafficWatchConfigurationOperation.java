package net.ravendb.client.serverwide.operations.trafficWatch;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IServerOperation;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetTrafficWatchConfigurationOperation implements IServerOperation<PutTrafficWatchConfigurationOperation.Parameters> {
    @Override
    public RavenCommand<PutTrafficWatchConfigurationOperation.Parameters> getCommand(DocumentConventions conventions) {
        return new GetTrafficWatchConfigurationCommand();
    }

    public static class GetTrafficWatchConfigurationCommand extends RavenCommand<PutTrafficWatchConfigurationOperation.Parameters> {

        public GetTrafficWatchConfigurationCommand() {
            super(PutTrafficWatchConfigurationOperation.Parameters.class);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/traffic-watch/configuration";

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
