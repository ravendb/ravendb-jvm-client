package net.ravendb.client.documents.changes;

import java.util.concurrent.ExecutorService;
import net.ravendb.client.http.RequestExecutor;

class AggressiveCacheDatabaseChanges extends DatabaseChanges {

    public AggressiveCacheDatabaseChanges(RequestExecutor requestExecutor, String databaseName, ExecutorService executorService, Runnable onDispose) {
        super(requestExecutor, databaseName, executorService, onDispose, null);
    }

    @Override
    protected void notifyAboutReconnection(Exception e) {
        notifyAboutError(e);
    }
}
