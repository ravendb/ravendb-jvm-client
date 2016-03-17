package net.ravendb.java.http.client;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.util.Args;

import java.io.*;
import java.util.zip.GZIPInputStream;

public class RavenGzipDecompressingEntity extends HttpEntityWrapper {

    /**
     * Creates a new {@link GzipDecompressingEntity} which will wrap the specified
     * {@link HttpEntity}.
     *
     * @param entity the non-null {@link HttpEntity} to be wrapped
     */
    public RavenGzipDecompressingEntity(HttpEntity entity) {
        super(entity);
    }

    InputStream decorate(final InputStream wrapped) throws IOException {
        try {
            return new GZIPInputStream(wrapped);
        } catch (EOFException e) {
            // WORKAROUND: we want to handle .net GZIP where empty stream is gziped as empty stream
            // see: http://stackoverflow.com/questions/24024886/using-gzipstream-to-compress-empty-input-results-in-an-invalid-gz-file-in-c-shar
            return new ByteArrayInputStream(new byte[0]);
        }
    }

    @Override
    public Header getContentEncoding() {

        /* This HttpEntityWrapper has dealt with the Content-Encoding. */
        return null;
    }

    @Override
    public long getContentLength() {

        /* length of ungzipped content is not known */
        return -1;
    }

    /**
     * Default buffer size.
     */
    private static final int BUFFER_SIZE = 1024 * 2;

    /**
     * {@link #getContent()} method must return the same {@link InputStream}
     * instance when DecompressingEntity is wrapping a streaming entity.
     */
    private InputStream content;

    private InputStream getDecompressingStream() throws IOException {
        final InputStream in = wrappedEntity.getContent();
        try {
            return decorate(in);
        } catch (final IOException ex) {
            in.close();
            throw ex;
        }
    }

    @Override
    public InputStream getContent() throws IOException {
        if (wrappedEntity.isStreaming()) {
            if (content == null) {
                content = getDecompressingStream();
            }
            return content;
        } else {
            return getDecompressingStream();
        }
    }

    @Override
    public void writeTo(final OutputStream outstream) throws IOException {
        Args.notNull(outstream, "Output stream");
        final InputStream instream = getContent();
        try {
            final byte[] buffer = new byte[BUFFER_SIZE];
            int l;
            while ((l = instream.read(buffer)) != -1) {
                outstream.write(buffer, 0, l);
            }
        } finally {
            instream.close();
        }
    }
}
