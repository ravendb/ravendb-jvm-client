package net.ravendb.client.documents.commands;

import net.ravendb.client.Constants;
import net.ravendb.client.extensions.HttpExtensions;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ResponseDisposeHandling;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;

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
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/databases/" + node.getDatabase() + "/docs?id=" + UrlUtils.escapeDataString(_id);

        HttpHead request = new HttpHead(url);

        if (_changeVector != null) {
            request.setHeader(Constants.Headers.IF_NONE_MATCH, _changeVector);
        }

        return request;
    }

    @Override
    public ResponseDisposeHandling processResponse(HttpCache cache, ClassicHttpResponse response, String url) {
        if (HttpStatus.SC_NOT_MODIFIED == response.getCode()) {
            result = _changeVector;
            return ResponseDisposeHandling.AUTOMATIC;
        }

        if (response.getCode() == HttpStatus.SC_NOT_FOUND) {
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
