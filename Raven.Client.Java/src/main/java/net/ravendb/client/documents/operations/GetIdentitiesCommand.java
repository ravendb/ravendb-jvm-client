package net.ravendb.client.documents.operations;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.util.Map;

public class GetIdentitiesCommand extends RavenCommand<Map<String, Long>> {

    public static class Result {
        private Map<String, Long> identities;

        public Map<String, Long> getIdentities() {
            return identities;
        }

        public void setIdentities(Map<String, Long> identities) {
            this.identities = identities;
        }
    }

    @SuppressWarnings("unchecked")
    public GetIdentitiesCommand() {
        super((Class<Map<String, Long>>)((Class<?>)Map.class));
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/debug/identities";

        return new HttpGet();
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        result = mapper.readValue(response, Result.class).getIdentities();
    }

}
