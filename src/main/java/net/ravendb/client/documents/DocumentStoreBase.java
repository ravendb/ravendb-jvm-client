package net.ravendb.client.documents;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.MaintenanceOperationExecutor;
import net.ravendb.client.documents.operations.OperationExecutor;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.EventHandler;
import net.ravendb.client.primitives.EventHelper;
import net.ravendb.client.primitives.VoidArgs;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 *  Contains implementation of some IDocumentStore operations shared by DocumentStore implementations
 */
public abstract class DocumentStoreBase implements IDocumentStore {

    private final List<EventHandler<SessionCreatedEventArgs>> onSessionCreated = new ArrayList<>();

    private final List<EventHandler<SucceedRequestEventArgs>> onSucceedRequest = new ArrayList<>();

    private final List<EventHandler<FailedRequestEventArgs>> onFailedRequest = new ArrayList<>();
    private final List<EventHandler<TopologyUpdatedEventArgs>> onTopologyUpdated = new ArrayList<>();

    protected DocumentStoreBase() {
    }

    public abstract void close();

    public abstract void addBeforeCloseListener(EventHandler<VoidArgs> event);

    public abstract void removeBeforeCloseListener(EventHandler<VoidArgs> event);

    public abstract void addAfterCloseListener(EventHandler<VoidArgs> event);

    public abstract void removeAfterCloseListener(EventHandler<VoidArgs> event);

    protected boolean disposed;

    public boolean isDisposed() {
        return disposed;
    }



    public abstract String getIdentifier();

    public abstract void setIdentifier(String identifier);

    public abstract IDocumentStore initialize();

    public abstract IDocumentSession openSession();

    public abstract IDocumentSession openSession(String database);

    public abstract IDocumentSession openSession(SessionOptions sessionOptions);


    private DocumentConventions conventions;

    /**
     * Gets the conventions.
     */
    @Override
    public DocumentConventions getConventions() {
        if (conventions == null) {
            conventions = new DocumentConventions();
        }
        return conventions;
    }

    public void setConventions(DocumentConventions conventions) {
        assertNotInitialized("conventions");
        this.conventions = conventions;
    }

    protected String[] urls = new String[0];

    public String[] getUrls() {
        return urls;
    }

    public void setUrls(String[] value) {
        assertNotInitialized("urls");

        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }

        for (int i = 0; i < value.length; i++) {
            if (value[i] == null)
                throw new IllegalArgumentException("Urls cannot contain null");

            try {
                new URL(value[i]);
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException("The url '" + value[i] + "' is not valid");
            }

            value[i] = StringUtils.stripEnd(value[i], "/");
        }

        this.urls = value;
    }

    protected boolean initialized;

    private ConcurrentMap<String, Long> _lastRaftIndexPerDatabase = new ConcurrentSkipListMap<>(String::compareToIgnoreCase);

    public Long getLastTransactionIndex(String database) {
        Long index = _lastRaftIndexPerDatabase.get(database);
        if (index == null || index == 0) {
            return null;
        }

        return index;
    }

    public void setLastTransactionIndex(String database, Long index) {
        if (index == null) {
            return;
        }

        _lastRaftIndexPerDatabase.compute(database, (__, initialValue) -> {
            if (initialValue == null) {
                return index;
            }
            return Math.max(initialValue, index);
        });
    }

    protected void ensureNotClosed() {
        if (disposed) {
            throw new IllegalStateException("The document store has already been disposed and cannot be used");
        }
    }

    public void assertInitialized() {
        if (!initialized) {
            throw new IllegalStateException("You cannot open a session or access the database commands before initializing the document store. Did you forget calling initialize()?");
        }
    }

    private void assertNotInitialized(String property) {
        if (initialized) {
            throw new IllegalStateException("You cannot set '" + property + "' after the document store has been initialized.");
        }
    }


    public void addOnSucceedRequestListener(EventHandler<SucceedRequestEventArgs> handler) {
        assertNotInitialized("onSucceedRequest");
        this.onSucceedRequest.add(handler);
    }

    public void removeOnSucceedRequestListener(EventHandler<SucceedRequestEventArgs> handler) {
        assertNotInitialized("onSucceedRequest");
        this.onSucceedRequest.remove(handler);
    }

    public void addOnFailedRequestListener(EventHandler<FailedRequestEventArgs> handler) {
        assertNotInitialized("onFailedRequest");
        this.onFailedRequest.add(handler);
    }

    public void removeOnFailedRequestListener(EventHandler<FailedRequestEventArgs> handler) {
        assertNotInitialized("onFailedRequest");
        this.onFailedRequest.remove(handler);
    }

    public void addOnTopologyUpdatedListener(EventHandler<TopologyUpdatedEventArgs> handler) {
        assertNotInitialized("onTopologyUpdated");
        this.onTopologyUpdated.add(handler);
    }

    public void removeOnTopologyUpdatedListener(EventHandler<TopologyUpdatedEventArgs> handler) {
        assertNotInitialized("onTopologyUpdated");
        this.onTopologyUpdated.remove(handler);
    }

    protected String database;

    /**
     * Gets the default database
     */
    @Override
    public String getDatabase() {
        return database;
    }

    /**
     * Sets the default database
     * @param database Sets the value
     */
    public void setDatabase(String database) {
        assertNotInitialized("database");
        this.database = database;
    }


    public abstract RequestExecutor getRequestExecutor();

    public abstract RequestExecutor getRequestExecutor(String databaseName);


    protected void registerEvents(InMemoryDocumentSessionOperations session) {

    }

    public void registerEvents(RequestExecutor requestExecutor) {
        for (EventHandler<FailedRequestEventArgs> handler : onFailedRequest) {
            requestExecutor.addOnFailedRequestListener(handler);
        }

        for (EventHandler<TopologyUpdatedEventArgs> handler : onTopologyUpdated) {
            requestExecutor.addOnTopologyUpdatedListener(handler);
        }


        for (EventHandler<SucceedRequestEventArgs> handler : onSucceedRequest) {
            requestExecutor.addOnSucceedRequestListener(handler);
        }
    }

    protected void afterSessionCreated(InMemoryDocumentSessionOperations session) {
        EventHelper.invoke(onSessionCreated, this, new SessionCreatedEventArgs(session));
    }

    public abstract MaintenanceOperationExecutor maintenance();

    public abstract OperationExecutor operations();

    public String getEffectiveDatabase(String database) {
        return DocumentStoreBase.getEffectiveDatabase(this, database);
    }

    public static String getEffectiveDatabase(IDocumentStore store, String database) {
        if (database == null) {
            database = store.getDatabase();
        }

        if (StringUtils.isNotBlank(database)) {
            return database;
        }

        throw new IllegalArgumentException("Cannot determine database to operate on. " +
                "Please either specify 'database' directly as an action parameter " +
                "or set the default database to operate on using 'DocumentStore.setDatabase' method. " +
                "Did you forget to pass 'database' parameter?");
    }
}
