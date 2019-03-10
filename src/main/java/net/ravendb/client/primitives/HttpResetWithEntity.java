package net.ravendb.client.primitives;

import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;

import java.net.URI;

public class HttpResetWithEntity extends HttpEntityEnclosingRequestBase {
    public final static String METHOD_NAME = "RESET";

    public HttpResetWithEntity() {
        super();
    }

    public HttpResetWithEntity(final URI uri) {
        super();
        setURI(uri);
    }

    /**
     * @param uri Request uri
     * @throws IllegalArgumentException if the uri is invalid.
     */
    public HttpResetWithEntity(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}
