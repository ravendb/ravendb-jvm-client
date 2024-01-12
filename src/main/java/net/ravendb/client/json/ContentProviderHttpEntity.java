package net.ravendb.client.json;

import com.github.luben.zstd.ZstdOutputStream;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.HttpCompressionAlgorithm;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;

import java.io.*;
import java.util.zip.GZIPOutputStream;

public class ContentProviderHttpEntity extends AbstractHttpEntity {

    private final ThrowingConsumer<OutputStream> contentProvider;
    private final HttpCompressionAlgorithm compressionAlgorithm;

    public ContentProviderHttpEntity(ThrowingConsumer<OutputStream> contentProvider, ContentType contentType, DocumentConventions conventions) {
        this(contentProvider, contentType, conventions, false);
    }

    public ContentProviderHttpEntity(ThrowingConsumer<OutputStream> contentProvider, ContentType contentType, DocumentConventions conventions, boolean chunked) {
        super(contentType, determinateContentEncoding(conventions), chunked);

        this.contentProvider = contentProvider;
        this.compressionAlgorithm = Boolean.TRUE.equals(conventions.getUseHttpCompression()) ? conventions.getHttpCompressionAlgorithm() : null;
    }

    private static String determinateContentEncoding(DocumentConventions conventions) {
        if (Boolean.TRUE.equals(conventions.getUseHttpCompression())) {
            if (HttpCompressionAlgorithm.Gzip.equals(conventions.getHttpCompressionAlgorithm())) {
                return Constants.Headers.Encodings.GZIP;
            } else if (HttpCompressionAlgorithm.Zstd.equals(conventions.getHttpCompressionAlgorithm())) {
                return Constants.Headers.Encodings.ZSTD;
            }
        }

        return null;
    }

    @Override
    public void close() throws IOException {
        //no-op
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
    public void writeTo(OutputStream outStream) throws IOException {
        if (HttpCompressionAlgorithm.Zstd.equals(compressionAlgorithm)) {
            final ZstdOutputStream zstd = new ZstdOutputStream(outStream);
            this.contentProvider.accept(zstd);

            // Only close output stream if the wrapped entity has been
            // successfully written out
            zstd.close();
        } else if (HttpCompressionAlgorithm.Gzip.equals(compressionAlgorithm)) {
            final GZIPOutputStream gzip = new GZIPOutputStream(outStream);
            this.contentProvider.accept(gzip);

            // Only close output stream if the wrapped entity has been
            // successfully written out
            gzip.close();
        } else {
            this.contentProvider.accept(outStream);
        }
    }

    @Override
    public boolean isStreaming() {
        return false;
    }

    public interface ThrowingConsumer<T> {
        void accept(T t) throws IOException;
    }
}
