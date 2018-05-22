package net.ravendb.client.json;

import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;

import java.io.*;
import java.util.function.Consumer;

public class ContentProviderHttpEntity extends AbstractHttpEntity {

    private final Consumer<OutputStream> contentProvider;

    public ContentProviderHttpEntity(Consumer<OutputStream> contentProvider, ContentType contentType) {
        this.contentProvider = contentProvider;

        if (contentType != null) {
            setContentType(contentType.toString());
        }
    }

    @Override
    public boolean isRepeatable() {
        return false;
    }

    @Override
    public long getContentLength() {
        return -1;
    }

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        throw new UnsupportedEncodingException();
    }

    @Override
    public void writeTo(OutputStream outstream) {
        this.contentProvider.accept(outstream);
    }

    @Override
    public boolean isStreaming() {
        return false;
    }
}
