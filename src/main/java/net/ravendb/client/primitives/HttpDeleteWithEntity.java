package net.ravendb.client.primitives;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

public class HttpDeleteWithEntity extends HttpEntityEnclosingRequestBase {
    public final static String METHOD_NAME = "DELETE";

    public HttpDeleteWithEntity() {
        super();
    }

    public HttpDeleteWithEntity(final URI uri) {
        super();
        setURI(uri);
    }

    /**
     * @param uri Request uri
     * @throws IllegalArgumentException if the uri is invalid.
     */
    public HttpDeleteWithEntity(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}
