package net.ravendb.client.documents.commands;

import org.apache.hc.core5.http.ClassicHttpResponse;

import java.io.InputStream;

public class StreamResultResponse {
    private ClassicHttpResponse response;
    private InputStream stream;

    public ClassicHttpResponse getResponse() {
        return response;
    }

    public void setResponse(ClassicHttpResponse response) {
        this.response = response;
    }

    public InputStream getStream() {
        return stream;
    }

    public void setStream(InputStream stream) {
        this.stream = stream;
    }
}
