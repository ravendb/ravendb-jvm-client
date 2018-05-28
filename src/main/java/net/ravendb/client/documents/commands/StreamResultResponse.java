package net.ravendb.client.documents.commands;

import org.apache.http.client.methods.CloseableHttpResponse;

import java.io.InputStream;

public class StreamResultResponse {
    private CloseableHttpResponse response;
    private InputStream stream;

    public CloseableHttpResponse getResponse() {
        return response;
    }

    public void setResponse(CloseableHttpResponse response) {
        this.response = response;
    }

    public InputStream getStream() {
        return stream;
    }

    public void setStream(InputStream stream) {
        this.stream = stream;
    }
}
