package net.ravendb.client.documents.changes;

import net.ravendb.client.http.RequestExecutor;

import java.util.concurrent.ExecutorService;

public class AggressiveCacheDatabaseChanges extends DatabaseChanges {

    public AggressiveCacheDatabaseChanges(RequestExecutor requestExecutor, String databaseName, ExecutorService executorService, Runnable onDispose) {
        super(requestExecutor, databaseName, executorService, onDispose, null);
    }

    @Override
    protected void notifyAboutReconnection(Exception e) {
        notifyAboutError(e);
    }

    @Override
    public AggressiveCacheDatabaseChanges ensureConnectedNow() {
        return (AggressiveCacheDatabaseChanges) super.ensureConnectedNow();
    }
}
