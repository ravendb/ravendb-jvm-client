package net.ravendb.client.documents;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.AbstractIndexCreationTask;
import net.ravendb.client.documents.indexes.IndexCreation;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.operations.MaintenanceOperationExecutor;
import net.ravendb.client.documents.operations.OperationExecutor;
import net.ravendb.client.documents.operations.indexes.PutIndexesOperation;
import net.ravendb.client.documents.session.*;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.EventHandler;
import net.ravendb.client.primitives.VoidArgs;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.List;

/**
 *  Contains implementation of some IDocumentStore operations shared by DocumentStore implementations
 */
public abstract class DocumentStoreBase implements IDocumentStore {

    private final List<EventHandler<BeforeStoreEventArgs>> onBeforeStore = new ArrayList<>();
    private final List<EventHandler<AfterSaveChangesEventArgs>> onAfterSaveChanges = new ArrayList<>();
    private final List<EventHandler<BeforeDeleteEventArgs>> onBeforeDelete = new ArrayList<>();
    private final List<EventHandler<BeforeQueryEventArgs>> onBeforeQuery = new ArrayList<>();

    protected DocumentStoreBase() {
        //TBD: Subscriptions = new DocumentSubscriptions(this);
    }

    public abstract void close();

    public abstract void addAfterCloseListener(EventHandler<VoidArgs> event);

    public abstract void removeAfterCloseListener(EventHandler<VoidArgs> event);

    protected boolean disposed;

    public boolean isDisposed() {
        return disposed;
    }

    //TBD: public abstract IDisposable AggressivelyCacheFor(TimeSpan cacheDuration, string database = null);

    //TBD: public abstract IDatabaseChanges Changes(string database = null);

    //TBD: public abstract IDisposable DisableAggressiveCaching(string database = null);

    public abstract String getIdentifier();

    public abstract void setIdentifier(String identifier);

    public abstract IDocumentStore initialize();

    public abstract IDocumentSession openSession();

    public abstract IDocumentSession openSession(String database);

    public abstract IDocumentSession openSession(SessionOptions sessionOptions);

    public void executeIndex(AbstractIndexCreationTask task) {
        executeIndex(task, null);
    }

    public void executeIndex(AbstractIndexCreationTask task, String database) {
        assertInitialized();
        task.execute(this, conventions, database);
    }

    @Override
    public void executeIndexes(List<AbstractIndexCreationTask> tasks) {
        executeIndexes(tasks, null);
    }

    @Override
    public void executeIndexes(List<AbstractIndexCreationTask> tasks, String database) {
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
        this.conventions = conventions;
    }

    protected String[] urls = new String[0];

    public String[] getUrls() {
        return urls;
    }

    public void setUrls(String[] value) {
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

    //TBD: public abstract BulkInsertOperation BulkInsert(string database = null);
    //TBD: public IReliableSubscriptions Subscriptions { get; }

    protected void ensureNotClosed() {
        if (disposed) {
            throw new IllegalStateException("The document store has already been disposed and cannot be used");
        }
    }

    protected void assertInitialized() {
        if (!initialized) {
            throw new IllegalStateException("You cannot open a session or access the database commands before initializing the document store. Did you forget calling initialize()?");
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
        if (initialized) {
            throw new IllegalStateException("You cannot change the certificate after the document store was initialized");
        }
        _certificate = certificate;
    }

    public abstract RequestExecutor getRequestExecutor();

    public abstract RequestExecutor getRequestExecutor(String databaseName);

    //TBD public IDisposable AggressivelyCache(string database = null)

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

    public abstract MaintenanceOperationExecutor maintenance();

    public abstract OperationExecutor operations();
}
