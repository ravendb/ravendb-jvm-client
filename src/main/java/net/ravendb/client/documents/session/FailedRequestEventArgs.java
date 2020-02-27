package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.EventArgs;

public class FailedRequestEventArgs extends EventArgs {

    private String _database;
    private String _url;
    private Exception _exception;


    public FailedRequestEventArgs(String database, String url, Exception exception) {
        _database = database;
        _url = url;
        _exception = exception;
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
}
