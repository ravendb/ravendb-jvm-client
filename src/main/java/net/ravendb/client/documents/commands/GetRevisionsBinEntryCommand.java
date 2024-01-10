package net.ravendb.client.documents.commands;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.JsonArrayResult;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.io.IOException;

public class GetRevisionsBinEntryCommand extends RavenCommand<JsonArrayResult> {

    private final int _start;
    private final Integer _pageSize;
    private final String _continuationToken;

    public GetRevisionsBinEntryCommand(int start, Integer pageSize) {
        super(JsonArrayResult.class);
        _start = start;
        _pageSize = pageSize;
        _continuationToken = null;
    }

    public GetRevisionsBinEntryCommand(String continuationToken) {
        super(JsonArrayResult.class);
        _continuationToken = continuationToken;
        _start = 0;
        _pageSize = null;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        StringBuilder path = new StringBuilder(node.getUrl());
        path
                .append("/databases/")
                .append(node.getDatabase())
                .append("/revisions/bin?start=")
                .append(_start);

        if (_pageSize != null) {
            path.append("&pageSize=")
                    .append(_pageSize);
        }

        if (StringUtils.isNotEmpty(_continuationToken)) {
            path.append("&continuationToken=")
                    .append(_continuationToken);
        }

        String url = path.toString();

        return new HttpGet(url);
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
