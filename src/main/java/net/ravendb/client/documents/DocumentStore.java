package net.ravendb.client.documents;

import net.ravendb.client.documents.identity.MultiDatabaseHiLoIdGenerator;
import net.ravendb.client.documents.operations.MaintenanceOperationExecutor;
import net.ravendb.client.documents.operations.OperationExecutor;
import net.ravendb.client.documents.session.DocumentSession;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.http.AggressiveCacheOptions;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.*;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Manages access to RavenDB and open sessions to work with RavenDB.
 */
public class DocumentStore extends DocumentStoreBase {
    //TBD:private readonly AtomicDictionary<IDatabaseChanges> _databaseChanges = new AtomicDictionary<IDatabaseChanges>(StringComparer.OrdinalIgnoreCase);
    //TBD: private ConcurrentDictionary<string, Lazy<EvictItemsFromCacheBasedOnChanges>> _aggressiveCacheChanges = new ConcurrentDictionary<string, Lazy<EvictItemsFromCacheBasedOnChanges>>();

    private final ConcurrentMap<String, RequestExecutor> requestExecutors = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    private MultiDatabaseHiLoIdGenerator _multiDbHiLo;

    private MaintenanceOperationExecutor maintenanceOperationExecutor;
    private OperationExecutor operationExecutor;

    private String identifier;
    private boolean _aggressiveCachingUsed;

    public DocumentStore(String url, String database) {
        this.setUrls(new String[]{ url });
        this.setDatabase(database);
    }

    public DocumentStore(String[] urls, String database) {
        this.setUrls(urls);
        this.setDatabase(database);
    }

    public DocumentStore() {

    }

    /**
     * Gets the identifier for this store.
     */
    public String getIdentifier() {
        if (identifier != null) {
            return identifier;
        }

        if (urls == null) {
            return null;
        }

        if (database != null) {
            return String.join(",", urls) + " (DB: " + database + ")";
        }

        return String.join(",", urls);
    }

    /**
     * Sets the identifier for this store.
     */
    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public void close() {
        EventHelper.invoke(beforeClose, this, EventArgs.EMPTY);
        /* TBD

            foreach (var value in _aggressiveCacheChanges.Values)
            {
                if (value.IsValueCreated == false)
                    continue;

                value.Value.Dispose();
            }

            var tasks = new List<Task>();
            foreach (var changes in _databaseChanges)
            {
                using (changes.Value)
                { }
            }

            // try to wait until all the async disposables are completed
            Task.WaitAll(tasks.ToArray(), TimeSpan.FromSeconds(3));
            // if this is still going, we continue with disposal, it is for graceful shutdown only, anyway

*/

        if (_multiDbHiLo != null) {
            try {
                _multiDbHiLo.returnUnusedRange();
            } catch (Exception e ){
                // ignore
            }
        }

        //TBD: Subscriptions?.Dispose();

        disposed = true;

        EventHelper.invoke(new ArrayList<>(afterClose), this, EventArgs.EMPTY);

        for (Map.Entry<String, RequestExecutor> kvp : requestExecutors.entrySet()) {
            kvp.getValue().close();
        }
    }

    /**
     * Opens the session.
     */
    @Override
    public IDocumentSession openSession() {
        return openSession(new SessionOptions());
    }

    /**
     * Opens the session for a particular database
     */
    @Override
    public IDocumentSession openSession(String database) {
        SessionOptions sessionOptions = new SessionOptions();
        sessionOptions.setDatabase(database);

        return openSession(sessionOptions);
    }

    @Override
    public IDocumentSession openSession(SessionOptions options) {
        assertInitialized();
        ensureNotClosed();

        UUID sessionId = UUID.randomUUID();
        String databaseName = ObjectUtils.firstNonNull(options.getDatabase(), getDatabase());
        RequestExecutor requestExecutor = ObjectUtils.firstNonNull(options.getRequestExecutor(), getRequestExecutor(databaseName));

        DocumentSession session = new DocumentSession(databaseName, this, sessionId, requestExecutor);
        registerEvents(session);
        afterSessionCreated(session);
        return session;

    }

    @Override
    public RequestExecutor getRequestExecutor() {
        return getRequestExecutor(null);
    }

    @Override
    public RequestExecutor getRequestExecutor(String database) {
        assertInitialized();

        if (database == null) {
            database = getDatabase();
        }

        RequestExecutor executor = requestExecutors.get(database);
        if (executor != null) {
            return executor;
        }

        if (!getConventions().isDisableTopologyUpdates()) {
            executor = RequestExecutor.create(getUrls(), getDatabase(), getCertificate(), getConventions());
        } else {
            executor = RequestExecutor.createForSingleNodeWithConfigurationUpdates(getUrls()[0], getDatabase(), getCertificate(), getConventions());
        }

        requestExecutors.put(database, executor);

        return executor;
    }

    /**
     * Initializes this instance.
     */
    @Override
    public IDocumentStore initialize() {
        if (initialized) {
            return this;
        }

        assertValidConfiguration();

        try {
            if (getConventions().getDocumentIdGenerator() == null) { // don't overwrite what the user is doing
                MultiDatabaseHiLoIdGenerator generator = new MultiDatabaseHiLoIdGenerator(this, getConventions());
                _multiDbHiLo = generator;

                getConventions().setDocumentIdGenerator(generator::generateDocumentId);
            }

            getConventions().freeze();
            initialized = true;
        } catch (Exception e) {
            close();
            throw ExceptionsUtils.unwrapException(e);
        }

        return this;
    }


    /**
     * Validate the configuration for the document store
     */
    protected void assertValidConfiguration() {
        if (urls == null || urls.length == 0) {
            throw new IllegalArgumentException("Document store URLs cannot be empty");
        }
    }

    /**
     * Setup the context for no aggressive caching
     *
     * This is mainly useful for internal use inside RavenDB, when we are executing
     * queries that have been marked with WaitForNonStaleResults, we temporarily disable
     * aggressive caching.
     */
    public CleanCloseable disableAggressiveCaching() {
        return disableAggressiveCaching(null);
    }

    /**
     * Setup the context for no aggressive caching
     *
     * This is mainly useful for internal use inside RavenDB, when we are executing
     * queries that have been marked with WaitForNonStaleResults, we temporarily disable
     * aggressive caching.
     */
    public CleanCloseable disableAggressiveCaching(String databaseName) {
        assertInitialized();
        RequestExecutor re = getRequestExecutor(ObjectUtils.firstNonNull(database, getDatabase()));
        AggressiveCacheOptions old = re.AggressiveCaching.get();
        re.AggressiveCaching.set(null);

        return () -> re.AggressiveCaching.set(old);
    }

    //TBD public override IDatabaseChanges Changes(string database = null)
    //TBD protected virtual IDatabaseChanges CreateDatabaseChanges(string database)
    //TBD public override IDisposable AggressivelyCacheFor(TimeSpan cacheDuration, string database = null)
    //TBD private void ListenToChangesAndUpdateTheCache(string database)

    private final List<EventHandler<VoidArgs>> afterClose = new ArrayList<>();

    private final List<EventHandler<VoidArgs>> beforeClose = new ArrayList<>();

    public void addBeforeCloseListener(EventHandler<VoidArgs> event) {
        this.beforeClose.add(event);
    }

    @Override
    public void removeBeforeCloseListener(EventHandler<VoidArgs> event) {
        this.beforeClose.remove(event);
    }

    public void addAfterCloseListener(EventHandler<VoidArgs> event) {
        this.afterClose.add(event);
    }

    @Override
    public void removeAfterCloseListener(EventHandler<VoidArgs> event) {
        this.afterClose.remove(event);
    }

    @Override
    public MaintenanceOperationExecutor maintenance() {
        assertInitialized();

        if (maintenanceOperationExecutor == null) {
            maintenanceOperationExecutor = new MaintenanceOperationExecutor(this);
        }

        return maintenanceOperationExecutor;
    }

    @Override
    public OperationExecutor operations() {
        if (operationExecutor == null) {
            operationExecutor = new OperationExecutor(this);
        }

        return operationExecutor;
    }

    // TBD public override BulkInsertOperation BulkInsert(string database = null)
}
