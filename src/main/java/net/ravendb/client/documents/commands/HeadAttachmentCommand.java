package net.ravendb.client.documents.commands;

import net.ravendb.client.Constants;
import net.ravendb.client.extensions.HttpExtensions;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ResponseDisposeHandling;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpHead;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpStatus;

public class HeadAttachmentCommand extends RavenCommand<String> {

    private final String _documentId;
    private final String _name;
    private final String _changeVector;

    public HeadAttachmentCommand(String documentId, String name, String changeVector) {
        super(String.class);

        if (StringUtils.isBlank(documentId)) {
            throw new IllegalArgumentException("DocumentId cannot be null or empty");
        }
        if (StringUtils.isBlank(name)) {
            throw new IllegalArgumentException("Name cannot be null or empty");
        }

        _documentId = documentId;
        _name = name;
        _changeVector = changeVector;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl()
                + "/databases/" + node.getDatabase()
                + "/attachments?id=" + UrlUtils.escapeDataString(_documentId)
                + "&name=" + UrlUtils.escapeDataString(_name);

        HttpHead httpHead = new HttpHead(url);

        if (_changeVector != null) {
            httpHead.addHeader(Constants.Headers.IF_NONE_MATCH, _changeVector);
        }

        return httpHead;
    }

    @Override
    public ResponseDisposeHandling processResponse(HttpCache cache, ClassicHttpResponse response, String url) {
        if (response.getCode() == HttpStatus.SC_NOT_MODIFIED) {
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

    @Override
    public boolean isReadRequest() {
        return false;
    }
}
