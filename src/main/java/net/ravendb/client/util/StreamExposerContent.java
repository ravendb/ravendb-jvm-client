package net.ravendb.client.util;

import net.ravendb.client.primitives.ExceptionsUtils;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class StreamExposerContent extends AbstractHttpEntity {
    public final CompletableFuture<OutputStream> outputStream;
    protected final CompletableFuture<Void> _done;

    public StreamExposerContent() {
        super(ContentType.APPLICATION_JSON, null, true);
        outputStream = new CompletableFuture<>();
        _done = new CompletableFuture<>();
    }

    @Override
    public InputStream getContent() throws IOException, UnsupportedOperationException {
        throw new UnsupportedEncodingException();
    }

    @SuppressWarnings("SameReturnValue")
    @Override
    public boolean isStreaming() {
        return false;
    }


    public boolean isDone() {
        return _done.isDone();
    }

    @Override
    public long getContentLength() {
        return -1;
    }

    @Override
    public void writeTo(OutputStream outputStream) {
        this.outputStream.complete(outputStream);
        try {
            _done.get();
        } catch (Exception e) {
            throw ExceptionsUtils.unwrapException(e);
        }
    }

    @Override
    public void close() throws IOException {
        // no-op
    }

    public boolean complete() {
        return _done.complete(null);
    }

    public void errorOnProcessingRequest(Exception exception) {
        _done.completeExceptionally(exception);
    }

    public void errorOnRequestStart(Exception exception) {
        outputStream.completeExceptionally(exception);
    }
}
