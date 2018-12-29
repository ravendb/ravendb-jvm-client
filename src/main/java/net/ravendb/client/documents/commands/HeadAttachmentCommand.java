package net.ravendb.client.documents.commands;

import net.ravendb.client.extensions.HttpExtensions;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ResponseDisposeHandling;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpRequestBase;

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
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl()
                + "/databases/" + node.getDatabase()
                + "/attachments?id=" + UrlUtils.escapeDataString(_documentId)
                + "&name=" + UrlUtils.escapeDataString(_name);

        HttpHead httpHead = new HttpHead();

        if (_changeVector != null) {
            httpHead.addHeader("If-None-Match", _changeVector);
        }

        return httpHead;
    }

    @Override
    public ResponseDisposeHandling processResponse(HttpCache cache, CloseableHttpResponse response, String url) {
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
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

    @Override
    public boolean isReadRequest() {
        return false;
    }
}
