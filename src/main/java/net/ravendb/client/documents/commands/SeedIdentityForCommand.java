package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class SeedIdentityForCommand extends RavenCommand<Long> implements IRaftCommand {

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
    public HttpUriRequestBase createRequest(ServerNode node) {
        ensureIsNotNullOrString(_id, "id");

        String url = node.getUrl() + "/databases/" + node.getDatabase() + "/identity/seed?name=" + urlEncode(_id) + "&value=" + _value;

        if (_forced) {
            url += "&force=true";
        }

        return new HttpPost(url);
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

    @Override
    public String getRaftUniqueRequestId() {
        return RaftIdGenerator.newId();
    }
}
