package net.ravendb.client.json;

import com.github.luben.zstd.ZstdOutputStream;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.HttpCompressionAlgorithm;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.AbstractHttpEntity;
import org.apache.hc.core5.http.message.BasicHeader;

import java.io.*;
import java.util.function.Consumer;
import java.util.zip.GZIPOutputStream;

public class ContentProviderHttpEntity extends AbstractHttpEntity {

    private final Consumer<OutputStream> contentProvider;
    private final HttpCompressionAlgorithm compressionAlgorithm;

    public ContentProviderHttpEntity(Consumer<OutputStream> contentProvider, ContentType contentType, DocumentConventions conventions) {
        this.contentProvider = contentProvider;
        this.compressionAlgorithm = Boolean.TRUE.equals(conventions.getUseHttpCompression()) ? conventions.getHttpCompressionAlgorithm() : null;

        if (contentType != null) {
            setContentType(contentType.toString());
        }

        if (compressionAlgorithm != null) {
            switch (compressionAlgorithm) {
                case Gzip:
                    contentEncoding = new BasicHeader(Constants.Headers.CONTENT_ENCODING, Constants.Headers.Encodings.GZIP);
                    break;
                case Zstd:
                    contentEncoding = new BasicHeader(Constants.Headers.CONTENT_ENCODING, Constants.Headers.Encodings.ZSTD);
                    break;
                default:
                    throw new IllegalStateException("Invalid HttpCompressionAlgorithm: " + conventions.getHttpCompressionAlgorithm());
            }
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
}
