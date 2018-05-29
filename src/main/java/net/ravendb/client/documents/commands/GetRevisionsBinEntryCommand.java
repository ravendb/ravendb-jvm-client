package net.ravendb.client.documents.commands;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.JsonArrayResult;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetRevisionsBinEntryCommand extends RavenCommand<JsonArrayResult> {

    private final long _etag;
    private final Integer _pageSize;

    public GetRevisionsBinEntryCommand(long etag, Integer pageSize) {
        super(JsonArrayResult.class);
        _etag = etag;
        _pageSize = pageSize;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        HttpGet request = new HttpGet();

        StringBuilder path = new StringBuilder(node.getUrl());
        path
                .append("/databases/")
                .append(node.getDatabase())
                .append("/revisions/bin?etag=")
                .append(_etag);

        if (_pageSize != null) {
            path.append("&pageSize=")
                    .append(_pageSize);
        }

        url.value = path.toString();

        return request;
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            throwInvalidResponse();
        }

        result = mapper.readValue(response, JsonArrayResult.class);
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }

}
