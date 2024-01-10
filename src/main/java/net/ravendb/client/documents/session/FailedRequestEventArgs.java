package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.EventArgs;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;

public class FailedRequestEventArgs extends EventArgs {

    private final String _database;
    private final String _url;
    private final Exception _exception;

    private final HttpRequest _request;
    private final HttpResponse _response;


    public FailedRequestEventArgs(String database, String url, Exception exception, HttpRequest request, HttpResponse response) {
        _database = database;
        _url = url;
        _exception = exception;
        _request = request;
        _response = response;
    }

    public String getDatabase() {
        return _database;
    }

    public String getUrl() {
        return _url;
    }

    public Exception getException() {
        return _exception;
    }

    public HttpRequest getRequest() {
        return _request;
    }

    public HttpResponse getResponse() {
        return _response;
    }
}
