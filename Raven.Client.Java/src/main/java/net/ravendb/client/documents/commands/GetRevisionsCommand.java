package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.org.apache.regexp.internal.RE;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetRevisionsCommand extends RavenCommand<ArrayNode> {

    private final String _id;
    private final Integer _start;
    private final Integer _pageSize;
    private final boolean _metadataOnly;

    public GetRevisionsCommand(String id, Integer start, Integer pageSize) {
        this(id, start, pageSize, false);
    }

    public GetRevisionsCommand(String id, Integer start, Integer pageSize, boolean metadataOnly) {
        super(ArrayNode.class);
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        _id = id;
        _start = start;
        _pageSize = pageSize;
        _metadataOnly = metadataOnly;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        HttpGet request = new HttpGet();

        StringBuilder pathBuilder = new StringBuilder(node.getUrl())
                .append("/databases/")
                .append(node.getDatabase())
                .append("/revisions")
                .append("?id=")
                .append(UrlUtils.escapeDataString(_id));

        if (_start != null) {
            pathBuilder.append("&start=").append(_start);
        }

        if (_pageSize != null) {
            pathBuilder.append("&pageSize=").append(_pageSize);
        }

        if (_metadataOnly) {
            pathBuilder.append("&metadata-only=true");
        }

        url.value = pathBuilder.toString();
        return request;
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            throwInvalidResponse();
        }

        result = (ArrayNode) mapper.readTree(response);
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
