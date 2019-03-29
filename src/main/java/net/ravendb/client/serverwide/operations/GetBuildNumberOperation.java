package net.ravendb.client.serverwide.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetBuildNumberOperation implements IServerOperation<BuildNumber> {
    @Override
    public RavenCommand<BuildNumber> getCommand(DocumentConventions conventions) {
        return new GetBuildNumberCommand();
    }

    private static class GetBuildNumberCommand extends RavenCommand<BuildNumber> {

        public GetBuildNumberCommand() {
            super(BuildNumber.class);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/build/version";

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
