package net.ravendb.client.documents;

import net.ravendb.client.documents.changes.IDatabaseChanges;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTaskBase;
import net.ravendb.client.documents.indexes.IndexCreation;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.operations.MaintenanceOperationExecutor;
import net.ravendb.client.documents.operations.OperationExecutor;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.documents.smuggler.DatabaseSmuggler;
import net.ravendb.client.documents.subscriptions.DocumentSubscriptions;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.CleanCloseable;
import net.ravendb.client.primitives.EventHandler;
import net.ravendb.client.primitives.EventHelper;
import net.ravendb.client.primitives.VoidArgs;
import org.apache.commons.lang3.ObjectUtils;
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

    private final List<EventHandler<BeforeStoreEventArgs>> onBeforeStore = new ArrayList<>();
    private final List<EventHandler<AfterSaveChangesEventArgs>> onAfterSaveChanges = new ArrayList<>();
    private final List<EventHandler<BeforeDeleteEventArgs>> onBeforeDelete = new ArrayList<>();
    private final List<EventHandler<BeforeQueryEventArgs>> onBeforeQuery = new ArrayList<>();
    private final List<EventHandler<SessionCreatedEventArgs>> onSessionCreated = new ArrayList<>();

    protected DocumentStoreBase() {
        _subscriptions = new DocumentSubscriptions((DocumentStore)this);
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

    @Override
    public abstract CleanCloseable aggressivelyCacheFor(Duration cacheDuration);

    @Override
    public abstract CleanCloseable aggressivelyCacheFor(Duration cacheDuration, String database);

    public abstract IDatabaseChanges changes();

    public abstract IDatabaseChanges changes(String database);

    @Override
    public abstract CleanCloseable disableAggressiveCaching();

    @Override
    public abstract CleanCloseable disableAggressiveCaching(String database);

    public abstract String getIdentifier();

    public abstract void setIdentifier(String identifier);

    public abstract IDocumentStore initialize();

    public abstract IDocumentSession openSession();

    public abstract IDocumentSession openSession(String database);

    public abstract IDocumentSession openSession(SessionOptions sessionOptions);

    public void executeIndex(AbstractIndexCreationTaskBase task) {
        executeIndex(task, null);
    }

    public void executeIndex(AbstractIndexCreationTaskBase task, String database) {
        assertInitialized();
        task.execute(this, conventions, database);
    }

    @Override
    public void executeIndexes(List<AbstractIndexCreationTaskBase> tasks) {
        executeIndexes(tasks, null);
    }

    @Override
    public void executeIndexes(List<AbstractIndexCreationTaskBase> tasks, String database) {
        assertInitialized();
        IndexDefinition[] indexesToAdd = IndexCreation.createIndexesToAdd(tasks, conventions);

        maintenance()
                .forDatabase(ObjectUtils.firstNonNull(database, getDatabase()))
                .send(new PutIndexesOperation(indexesToAdd));
    }

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

    private KeyStore _certificate;
    private KeyStore _trustStore;

    public abstract BulkInsertOperation bulkInsert();

    public abstract BulkInsertOperation bulkInsert(String database);

    private final DocumentSubscriptions _subscriptions;

    public DocumentSubscriptions subscriptions() {
        return _subscriptions;
    }

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

    public void addBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler) {
        this.onBeforeStore.add(handler);

    }
    public void removeBeforeStoreListener(EventHandler<BeforeStoreEventArgs> handler) {
        this.onBeforeStore.remove(handler);
    }

    public void addAfterSaveChangesListener(EventHandler<AfterSaveChangesEventArgs> handler) {
        this.onAfterSaveChanges.add(handler);
    }

    public void removeAfterSaveChangesListener(EventHandler<AfterSaveChangesEventArgs> handler) {
        this.onAfterSaveChanges.remove(handler);
    }

    public void addBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler) {
        this.onBeforeDelete.add(handler);
    }
    public void removeBeforeDeleteListener(EventHandler<BeforeDeleteEventArgs> handler) {
        this.onBeforeDelete.remove(handler);
    }

    public void addBeforeQueryListener(EventHandler<BeforeQueryEventArgs> handler) {
        this.onBeforeQuery.add(handler);
    }
    public void removeBeforeQueryListener(EventHandler<BeforeQueryEventArgs> handler) {
        this.onBeforeQuery.remove(handler);
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

    /**
     * The client certificate to use for authentication
     * @return Certificate to use
     */
    public KeyStore getCertificate() {
        return _certificate;
    }

    /**
     * The client certificate to use for authentication
     * @param certificate Certificate to use
     */
    public void setCertificate(KeyStore certificate) {
        assertNotInitialized("certificate");
        _certificate = certificate;
    }

    public KeyStore getTrustStore() {
        return _trustStore;
    }

    public void setTrustStore(KeyStore trustStore) {
        this._trustStore = trustStore;
    }

    public abstract DatabaseSmuggler smuggler();

    public abstract RequestExecutor getRequestExecutor();

    public abstract RequestExecutor getRequestExecutor(String databaseName);

    @Override
    public CleanCloseable aggressivelyCache() {
        return aggressivelyCache(null);
    }

    @Override
    public CleanCloseable aggressivelyCache(String database) {
        return aggressivelyCacheFor(Duration.ofDays(1), database);
    }

    protected void registerEvents(InMemoryDocumentSessionOperations session) {
        for (EventHandler<BeforeStoreEventArgs> handler : onBeforeStore) {
            session.addBeforeStoreListener(handler);
        }

        for (EventHandler<AfterSaveChangesEventArgs> handler : onAfterSaveChanges) {
            session.addAfterSaveChangesListener(handler);
        }

        for (EventHandler<BeforeDeleteEventArgs> handler : onBeforeDelete) {
            session.addBeforeDeleteListener(handler);
        }

        for (EventHandler<BeforeQueryEventArgs> handler : onBeforeQuery) {
            session.addBeforeQueryListener(handler);
        }
    }

    protected void afterSessionCreated(InMemoryDocumentSessionOperations session) {
        EventHelper.invoke(onSessionCreated, this, new SessionCreatedEventArgs(session));
    }

    public abstract MaintenanceOperationExecutor maintenance();

    public abstract OperationExecutor operations();
}
