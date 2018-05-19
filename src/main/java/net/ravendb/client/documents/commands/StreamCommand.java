package net.ravendb.client.documents.commands;

import net.ravendb.client.http.*;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

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
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        HttpGet request = new HttpGet();

        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/" + _url;
        return request;
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
