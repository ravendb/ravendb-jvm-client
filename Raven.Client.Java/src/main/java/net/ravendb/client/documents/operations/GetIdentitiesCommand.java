package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.util.Map;

public class GetIdentitiesCommand extends RavenCommand<Map<String, Long>> {

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
        result = mapper.readValue(response, TypeFactory.defaultInstance().constructMapType(Map.class, String.class, Long.class));
    }

}
