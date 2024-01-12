package net.ravendb.client.primitives;

import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.net.URI;

public class HttpReset extends HttpUriRequestBase {

    private static final long serialVersionUID = 1L;

    public final static String METHOD_NAME = "RESET";


    /**
     * Creates a new instance initialized with the given URI.
     *
     * @param uri a non-null request URI.
     * @throws IllegalArgumentException if the uri is null.
     */
    public HttpReset(final URI uri) {
        super(METHOD_NAME, uri);
    }

    /**
     * Creates a new instance initialized with the given URI.
     *
     * @param uri a non-null request URI.
     * @throws IllegalArgumentException if the uri is invalid.
     */
    public HttpReset(final String uri) {
        this(URI.create(uri));
    }
}
