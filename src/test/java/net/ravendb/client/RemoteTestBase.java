package net.ravendb.client;

import com.google.common.collect.Sets;
import net.ravendb.client.documents.DocumentStore;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.driver.RavenServerLocator;
import net.ravendb.client.driver.RavenTestDriver;
import net.ravendb.client.exceptions.database.DatabaseDoesNotExistException;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.DatabaseRecord;
import net.ravendb.client.serverwide.operations.CreateDatabaseOperation;
import net.ravendb.client.serverwide.operations.DeleteDatabasesOperation;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SuppressWarnings("SameParameterValue")
public class RemoteTestBase extends RavenTestDriver implements CleanCloseable {

    private final RavenServerLocator locator;

    private static IDocumentStore globalServer;
    private static Process globalServerProcess;

    private final Set<DocumentStore> documentStores = Sets.newConcurrentHashSet();

    private static final AtomicInteger index = new AtomicInteger();

    private static class TestServiceLocator extends RavenServerLocator {

        @Override
        public String[] getCommandArguments() {
            return new String[] {
                    "--ServerUrl=http://127.0.0.1:0",
                    "--ServerUrl.Tcp=tcp://127.0.0.1:38881",
                    "--Features.Availability=Experimental"
            };
        }
    }


    public RemoteTestBase() {
        this.locator = new TestServiceLocator();
    }

    protected void customizeDbRecord(DatabaseRecord dbRecord) {

    }

    protected void customizeStore(DocumentStore store) {

    }


    @SuppressWarnings("UnusedReturnValue")
    private IDocumentStore runServer() throws Exception {
        Reference<Process> processReference = new Reference<>();
        IDocumentStore store = runServerInternal(getLocator(), processReference, s -> {

        });
        setGlobalServerProcess(processReference.value);

        globalServer = store;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> killGlobalServerProcess()));
        return store;
    }

    private RavenServerLocator getLocator() {
        return locator;
    }

    private static IDocumentStore getGlobalServer() {
        return globalServer;
    }

    private static Process getGlobalProcess() {
        return globalServerProcess;
    }

    private static void setGlobalServerProcess(Process p) {
        globalServerProcess = p;
    }

    private static void killGlobalServerProcess() {
        Process p;

        p = globalServerProcess;
        globalServerProcess = null;
        globalServer.close();
        globalServer = null;

        killProcess(p);
    }

    public DocumentStore getDocumentStore() throws Exception {
        return getDocumentStore("test_db");
    }

    public DocumentStore getDocumentStore(String database) throws Exception {
        String name = database + "_" + index.incrementAndGet();
        reportInfo("getDocumentStore for db " + database + ".");

        if (getGlobalServer() == null) {
            synchronized (RavenTestDriver.class) {
                if (getGlobalServer() == null) {
                    runServer();
                }
            }
        }

        IDocumentStore documentStore = getGlobalServer();
        DatabaseRecord databaseRecord = new DatabaseRecord();
        databaseRecord.setDatabaseName(name);

        customizeDbRecord(databaseRecord);

        CreateDatabaseOperation createDatabaseOperation = new CreateDatabaseOperation(databaseRecord);
        documentStore.maintenance().server().send(createDatabaseOperation);


        DocumentStore store = new DocumentStore();
        store.setUrls(documentStore.getUrls());
        store.setDatabase(name);

        customizeStore(store);

        store.initialize();


        setupDatabase(store);


        documentStores.add(store);
        return store;
    }



    @Override
    public void close() {
        if (disposed) {
            return;
        }

        ArrayList<Exception> exceptions = new ArrayList<>();

        for (DocumentStore documentStore : documentStores) {
            try {
                documentStore.close();
            } catch (Exception e) {
                exceptions.add(e);
            }
        }

        disposed = true;

        if (exceptions.size() > 0) {
            throw new RuntimeException(exceptions
                    .stream()
                    .map(x -> x.toString())
                    .collect(Collectors.joining(", ")));
        }
    }
}
