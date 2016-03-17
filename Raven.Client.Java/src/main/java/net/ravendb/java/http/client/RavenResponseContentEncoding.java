package net.ravendb.java.http.client;

import org.apache.http.*;
import org.apache.http.client.entity.DeflateDecompressingEntity;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.conn.EofSensorInputStream;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;
import java.util.Locale;


public class RavenResponseContentEncoding implements HttpResponseInterceptor {
    public static final String UNCOMPRESSED = "http.client.response.uncompressed";

    /**
     * Handles the following {@code Content-Encoding}s by
     * using the appropriate decompressor to wrap the response Entity:
     * <ul>
     * <li>gzip - see {@link GzipDecompressingEntity}</li>
     * <li>deflate - see {@link DeflateDecompressingEntity}</li>
     * <li>identity - no action needed</li>
     * </ul>
     *
     * @param response the response which contains the entity
     * @param  context not currently used
     *
     * @throws HttpException if the {@code Content-Encoding} is none of the above
     */
    public void process(
            final HttpResponse response,
            final HttpContext context) throws HttpException, IOException {
        final HttpEntity entity = response.getEntity();

        // entity can be null in case of 304 Not Modified, 204 No Content or similar
        // check for zero length entity.
        if (entity != null && entity.getContentLength() != 0) {
            final Header ceheader = entity.getContentEncoding();
            if (ceheader != null) {
                final HeaderElement[] codecs = ceheader.getElements();
                boolean uncompressed = false;
                for (final HeaderElement codec : codecs) {
                    final String codecname = codec.getName().toLowerCase(Locale.US);
                    if ("gzip".equals(codecname) || "x-gzip".equals(codecname)) {
                        if (!emptyStreamDetected(response.getEntity())) {
                            response.setEntity(new RavenGzipDecompressingEntity(response.getEntity()));
                            uncompressed = true;
                        } else {
                            response.removeHeader(ceheader);
                        }

                        break;
                    } else if ("deflate".equals(codecname)) {
                        response.setEntity(new DeflateDecompressingEntity(response.getEntity()));
                        uncompressed = true;
                        break;
                    } else if ("identity".equals(codecname)) {

                        /* Don't need to transform the content - no-op */
                        return;
                    } else {
                        throw new HttpException("Unsupported Content-Coding: " + codec.getName());
                    }
                }
                if (uncompressed) {
                    response.removeHeaders("Content-Length");
                    response.removeHeaders("Content-Encoding");
                    response.removeHeaders("Content-MD5");
                }
            }
        }
    }

    /**
     * HttpClient customization to detect when we have gzip with empty payload
     *
     * .NET server produces empty gzip stream when passing empty stream.
     *
     * In java gzip decode expects some data in empty gzipped stream.
     * @return
     * @param entity
     */
    private boolean emptyStreamDetected(HttpEntity entity) throws IOException {
        return false;
    }
}
