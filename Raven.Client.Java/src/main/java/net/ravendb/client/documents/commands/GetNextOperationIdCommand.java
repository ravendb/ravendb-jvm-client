package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.io.InputStream;

public class GetNextOperationIdCommand extends RavenCommand<Long> {
    public GetNextOperationIdCommand() {
        super(Long.class);
    }

    @Override
    public boolean isReadRequest() {
        return false; // disable caching
    }

    @Override
    public HttpRequestBase createRequest(ObjectMapper context, ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/operations/next-operation-id";

        return new HttpGet();
    }

    @Override
    public void setResponse(ObjectMapper context, InputStream response, boolean fromCache) throws IOException {
        JsonNode jsonNode = context.readTree(response);

        if (jsonNode.has("Id")) {
            result = jsonNode.get("Id").asLong();
        }
    }
}
