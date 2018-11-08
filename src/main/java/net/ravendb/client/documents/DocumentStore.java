package net.ravendb.client.documents;

import net.ravendb.client.documents.changes.DatabaseChanges;
import net.ravendb.client.documents.changes.EvictItemsFromCacheBasedOnChanges;
import net.ravendb.client.documents.changes.IDatabaseChanges;
import net.ravendb.client.documents.identity.MultiDatabaseHiLoIdGenerator;
import net.ravendb.client.documents.operations.MaintenanceOperationExecutor;
import net.ravendb.client.documents.operations.OperationExecutor;
import net.ravendb.client.documents.session.DocumentSession;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.documents.smuggler.DatabaseSmuggler;
import net.ravendb.client.http.AggressiveCacheOptions;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.*;
import org.apache.commons.lang3.ObjectUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Manages access to RavenDB and open sessions to work with RavenDB.
 */
public class DocumentStore extends DocumentStoreBase {

    private ExecutorService executorService = Executors.newCachedThreadPool();

    private final ConcurrentMap<String, IDatabaseChanges> _databaseChanges = new ConcurrentSkipListMap<>(String::compareToIgnoreCase);

    private final ConcurrentMap<String, Lazy<EvictItemsFromCacheBasedOnChanges>> _aggressiveCacheChanges = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, RequestExecutor> requestExecutors = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    private MultiDatabaseHiLoIdGenerator _multiDbHiLo;

    private MaintenanceOperationExecutor maintenanceOperationExecutor;
    private OperationExecutor operationExecutor;

    private DatabaseSmuggler _smuggler;

    private String identifier;
    private boolean _aggressiveCachingUsed;

    public DocumentStore(String url, String database) {
        this.setUrls(new String[]{url});
        this.setDatabase(database);
    }

    public DocumentStore(String[] urls, String database) {
        this.setUrls(urls);
        this.setDatabase(database);
    }

    public DocumentStore() {

    }

    public ExecutorService getExecutorService() {
        return executorService;
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

    @SuppressWarnings("EmptyTryBlock")
    public void close() {
        EventHelper.invoke(beforeClose, this, EventArgs.EMPTY);

        for (Lazy<EvictItemsFromCacheBasedOnChanges> value : _aggressiveCacheChanges.values()) {
            if (!value.isValueCreated()) {
                continue;
            }

            value.getValue().close();
        }

        for (IDatabaseChanges changes : _databaseChanges.values()) {
            try (CleanCloseable value = changes) {
                // try will close all values
            }
        }

        if (_multiDbHiLo != null) {
            try {
                _multiDbHiLo.returnUnusedRange();
            } catch (Exception e) {
                // ignore
            }
        }

        if (subscriptions() != null) {
            subscriptions().close();
        }

        disposed = true;

        EventHelper.invoke(new ArrayList<>(afterClose), this, EventArgs.EMPTY);

        for (Map.Entry<String, RequestExecutor> kvp : requestExecutors.entrySet()) {
            kvp.getValue().close();
        }

        executorService.shutdown();
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
        DocumentSession session = new DocumentSession(this, sessionId, options);
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
            executor = RequestExecutor.create(getUrls(), database, getCertificate(), getTrustStore(), executorService, getConventions());
        } else {
            executor = RequestExecutor.createForSingleNodeWithConfigurationUpdates(getUrls()[0], database, getCertificate(), getTrustStore(), executorService, getConventions());
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
     * <p>
     * This is mainly useful for internal use inside RavenDB, when we are executing
     * queries that have been marked with WaitForNonStaleResults, we temporarily disable
     * aggressive caching.
     */
    public CleanCloseable disableAggressiveCaching() {
        return disableAggressiveCaching(null);
    }

    /**
     * Setup the context for no aggressive caching
     * <p>
     * This is mainly useful for internal use inside RavenDB, when we are executing
     * queries that have been marked with WaitForNonStaleResults, we temporarily disable
     * aggressive caching.
     */
    public CleanCloseable disableAggressiveCaching(String databaseName) {
        assertInitialized();
        RequestExecutor re = getRequestExecutor(ObjectUtils.firstNonNull(databaseName, getDatabase()));
        AggressiveCacheOptions old = re.aggressiveCaching.get();
        re.aggressiveCaching.set(null);

        return () -> re.aggressiveCaching.set(old);
    }

    @Override
    public IDatabaseChanges changes() {
        return changes(null);
    }

    @Override
    public IDatabaseChanges changes(String database) {
        assertInitialized();

        return _databaseChanges.computeIfAbsent(ObjectUtils.firstNonNull(database, getDatabase()), this::createDatabaseChanges);
    }

    protected IDatabaseChanges createDatabaseChanges(String database) {
        return new DatabaseChanges(getRequestExecutor(database), database, executorService, () -> _databaseChanges.remove(database));
    }

    public Exception getLastDatabaseChangesStateException() {
        return getLastDatabaseChangesStateException(null);
    }

    public Exception getLastDatabaseChangesStateException(String database) {
        DatabaseChanges databaseChanges = (DatabaseChanges) _databaseChanges.get(ObjectUtils.firstNonNull(database, getDatabase()));

        if (databaseChanges != null) {
            return databaseChanges.getLastConnectionStateException();
        }

        return null;
    }

    @Override
    public CleanCloseable aggressivelyCacheFor(Duration cacheDuration) {
        return aggressivelyCacheFor(cacheDuration, null);
    }

    @Override
    public CleanCloseable aggressivelyCacheFor(Duration cacheDuration, String database) {
        assertInitialized();

        database = ObjectUtils.firstNonNull(database, getDatabase());

        if (database == null) {
            throw new IllegalStateException("Cannot use aggressivelyCache and aggressivelyCacheFor without a default database defined " +
                    "unless 'database' parameter is provided. Did you forget to pass 'database' parameter?");
        }

        if (!_aggressiveCachingUsed) {
            listenToChangesAndUpdateTheCache(database);
        }

        RequestExecutor re = getRequestExecutor(database);
        AggressiveCacheOptions old = re.aggressiveCaching.get();

        re.aggressiveCaching.set(new AggressiveCacheOptions(cacheDuration));

        return () -> re.aggressiveCaching.set(old);
    }

    private void listenToChangesAndUpdateTheCache(String database) {

        // this is intentionally racy, most cases, we'll already
        // have this set once, so we won't need to do it again

        _aggressiveCachingUsed = true;
        Lazy<EvictItemsFromCacheBasedOnChanges> lazy = _aggressiveCacheChanges.get(database);

        if (lazy == null) {
            lazy = _aggressiveCacheChanges.computeIfAbsent(database, db -> new Lazy<>(() -> new EvictItemsFromCacheBasedOnChanges(this, database)));
        }

        lazy.getValue(); // force evaluation
    }

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
    public DatabaseSmuggler smuggler() {
        if (_smuggler == null) {
            _smuggler = new DatabaseSmuggler(this);
        }

        return _smuggler;
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

    @Override
    public BulkInsertOperation bulkInsert() {
        return bulkInsert(null);
    }

    @Override
    public BulkInsertOperation bulkInsert(String database) {
        assertInitialized();

        return new BulkInsertOperation(ObjectUtils.firstNonNull(database, getDatabase()), this);
    }
}
