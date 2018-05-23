package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class SeedIdentityForCommand extends RavenCommand<Long> {

    private final String _id;
    private final long _value;
    private final boolean _forced;

    public SeedIdentityForCommand(String id, Long value) {
        this(id, value, false);
    }

    public SeedIdentityForCommand(String id, Long value, boolean forced) {
        super(Long.class);
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        _id = id;
        _value = value;
        _forced = forced;
    }

    @Override
    public boolean isReadRequest() {
        return false;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        ensureIsNotNullOrString(_id, "id");

        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/identity/seed?name=" + urlEncode(_id) + "&value=" + _value;

        if (_forced) {
            url.value += "&force=true";
        }

        return new HttpPost();
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            throwInvalidResponse();
        }


        JsonNode jsonNode = mapper.readTree(response);
        if (!jsonNode.has("NewSeedValue")) {
            throwInvalidResponse();
        }

        result = jsonNode.get("NewSeedValue").asLong();
    }

}
