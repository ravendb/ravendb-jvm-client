package net.ravendb.client.documents.commands;

import net.ravendb.client.http.*;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;

import java.io.IOException;

public class StreamCommand extends RavenCommand<StreamResultResponse> {
    private final String _url;

    public StreamCommand(String url) {
        super(StreamResultResponse.class);

        if (url == null) {
            throw new IllegalArgumentException("Url cannot be null");
        }
        _url = url;

        responseType = RavenCommandResponseType.EMPTY;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/databases/" + node.getDatabase() + "/" + _url;

        return new HttpGet(url);
    }

    @Override
    public ResponseDisposeHandling processResponse(HttpCache cache, CloseableHttpResponse response, String url) {
        try {
            StreamResultResponse result = new StreamResultResponse();
            result.setResponse(response);
            result.setStream(response.getEntity().getContent());
            setResult(result);

            return ResponseDisposeHandling.MANUALLY;
        } catch (IOException e) {
            throw new RuntimeException("Unable to process stream response: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
