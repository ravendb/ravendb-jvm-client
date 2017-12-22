package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class NextIdentityForCommand extends RavenCommand<Long> {

    private final String _id;

    public NextIdentityForCommand(String id) {
        super(Long.class);

        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        _id = id;
    }

    @Override
    public boolean isReadRequest() {
        return false;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        ensureIsNotNullOrString(_id, "id");

        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/identity/next?name=" + urlEncode(_id);

        return new HttpPost();
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            throwInvalidResponse();
        }

        JsonNode jsonNode = mapper.readTree(response);
        if (!jsonNode.has("NewIdentityValue")) {
            throwInvalidResponse();
        }

        result = jsonNode.get("NewIdentityValue").asLong();
    }
}
