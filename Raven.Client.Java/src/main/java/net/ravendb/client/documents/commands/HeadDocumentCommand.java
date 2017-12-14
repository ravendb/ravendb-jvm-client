package net.ravendb.client.documents.commands;

import net.ravendb.client.extensions.HttpExtensions;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ResponseDisposeHandling;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;

public class HeadDocumentCommand extends RavenCommand<String> {

    private final String _id;
    private final String _changeVector;

    public HeadDocumentCommand(String id, String changeVector) {
        super(String.class);

        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }

        _id = id;
        _changeVector = changeVector;
    }

    @Override
    public boolean isReadRequest() {
        return false;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/docs?id=" + UrlUtils.escapeDataString(_id);

        HttpHead request = new HttpHead();

        if (_changeVector != null) {
            request.setHeader("If-None-Match", _changeVector);
        }

        return request;
    }

    @Override
    public ResponseDisposeHandling processResponse(HttpCache cache, CloseableHttpResponse response, String url) {
        if (HttpStatus.SC_NOT_MODIFIED == response.getStatusLine().getStatusCode()) {
            result = _changeVector;
            return ResponseDisposeHandling.AUTOMATIC;
        }

        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND) {
            result = null;
            return ResponseDisposeHandling.AUTOMATIC;
        }

        result = HttpExtensions.getRequiredEtagHeader(response);
        return ResponseDisposeHandling.AUTOMATIC;
    }

    @Override
    public void setResponse(String response, boolean fromCache) {
        if (response != null) {
            throwInvalidResponse();
        }
        result = null;
    }
}
