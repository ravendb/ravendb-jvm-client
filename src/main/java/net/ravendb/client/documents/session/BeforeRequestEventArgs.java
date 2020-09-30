package net.ravendb.client.documents.session;

import net.ravendb.client.primitives.EventArgs;
import org.apache.http.HttpRequest;

public class BeforeRequestEventArgs extends EventArgs {

    private String database;
    private String url;
    private HttpRequest request;
    private int attemptNumber;

    public BeforeRequestEventArgs(String database, String url, HttpRequest request, int attemptNumber) {
        this.database = database;
        this.url = url;
        this.request = request;
        this.attemptNumber = attemptNumber;
    }

    public String getDatabase() {
        return database;
    }

    public String getUrl() {
        return url;
    }

    public HttpRequest getRequest() {
        return request;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }
}
