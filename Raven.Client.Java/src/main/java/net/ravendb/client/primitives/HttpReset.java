package net.ravendb.client.primitives;

import org.apache.http.client.methods.HttpRequestBase;

import java.net.URI;

public class HttpReset extends HttpRequestBase {

    public final static String METHOD_NAME = "RESET";

    public HttpReset() {
        super();
    }

    public HttpReset(final URI uri) {
        super();
        setURI(uri);
    }

    /**
     * @param uri Request uri
     * @throws IllegalArgumentException if the uri is invalid.
     */
    public HttpReset(final String uri) {
        super();
        setURI(URI.create(uri));
    }

    @Override
    public String getMethod() {
        return METHOD_NAME;
    }
}
