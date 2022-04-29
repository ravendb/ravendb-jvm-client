package net.ravendb.client;

import com.google.common.base.Stopwatch;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.exceptions.ConflictException;

import java.util.concurrent.TimeUnit;

public class ReplicationTestBase2 {
    private final RemoteTestBase parent;

    public ReplicationTestBase2(RemoteTestBase parent) {
        this.parent = parent;
    }

    public void waitForConflict(IDocumentStore store, String id) throws InterruptedException {
        Stopwatch sw = Stopwatch.createStarted();
        while (sw.elapsed(TimeUnit.MILLISECONDS) < 10_000) {
            try (IDocumentSession session = store.openSession()) {
                session.load(Object.class, id);

                Thread.sleep(10);
            } catch (ConflictException e) {
                return;
            }
        }

        throw new IllegalStateException("Waited for conflict on '" + id + "' but it did not happen");
    }
}
