package net.ravendb.client.documents.session;

import net.ravendb.client.http.RequestExecutor;

public class SessionOptions {
    private String database;
    private RequestExecutor requestExecutor;

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public RequestExecutor getRequestExecutor() {
        return requestExecutor;
    }

    public void setRequestExecutor(RequestExecutor requestExecutor) {
        this.requestExecutor = requestExecutor;
    }
}
