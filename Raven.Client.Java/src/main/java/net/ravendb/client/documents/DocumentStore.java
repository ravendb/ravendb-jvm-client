package net.ravendb.client.documents;

import net.ravendb.client.documents.identity.MultiDatabaseHiLoIdGenerator;
import net.ravendb.client.documents.operations.AdminOperationExecutor;
import net.ravendb.client.documents.operations.OperationExecutor;
import net.ravendb.client.documents.session.DocumentSession;
import net.ravendb.client.documents.session.IDocumentSession;
import net.ravendb.client.documents.session.SessionOptions;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;

/**
 * Manages access to RavenDB and open sessions to work with RavenDB.
 */
public class DocumentStore extends DocumentStoreBase {
    //TODO:private readonly AtomicDictionary<IDatabaseChanges> _databaseChanges = new AtomicDictionary<IDatabaseChanges>(StringComparer.OrdinalIgnoreCase);
    //TODO: private ConcurrentDictionary<string, Lazy<EvictItemsFromCacheBasedOnChanges>> _aggressiveCacheChanges = new ConcurrentDictionary<string, Lazy<EvictItemsFromCacheBasedOnChanges>>();
    //TODO: private readonly ConcurrentDictionary<string, EvictItemsFromCacheBasedOnChanges> _observeChangesAndEvictItemsFromCacheForDatabases = new ConcurrentDictionary<string, EvictItemsFromCacheBasedOnChanges>();

    private final ConcurrentMap<String, RequestExecutor> requestExecutors = new ConcurrentSkipListMap<>(String.CASE_INSENSITIVE_ORDER);

    private MultiDatabaseHiLoIdGenerator _multiDbHiLo;

    private AdminOperationExecutor adminOperationExecutor;
    private OperationExecutor operationExecutor;

    private String identifier;
    //tODO: private bool _aggressiveCachingUsed;

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
        /* TODO

            foreach (var observeChangesAndEvictItemsFromCacheForDatabase in _observeChangesAndEvictItemsFromCacheForDatabases)
                observeChangesAndEvictItemsFromCacheForDatabase.Value.Dispose();

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

        //TODO: Subscriptions?.Dispose();

        disposed = true;
        EventHelper.invoke(afterDispose, this, EventArgs.EMPTY);

        for (Map.Entry<String, RequestExecutor> kvp : requestExecutors.entrySet()) {
            kvp.getValue().close();
        }
    }

    /**
     * Opens the session.
     * @return
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
        String databaseName = Lang.coalesce(options.getDatabase(), getDatabase());
        RequestExecutor requestExecutor = Lang.coalesce(options.getRequestExecutor(), getRequestExecutor(databaseName));

        DocumentSession session = new DocumentSession(databaseName, this, sessionId, requestExecutor);
        //TODO: registerEvents(session);
        // AfterSessionCreated(session);
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
            executor = RequestExecutor.create(getUrls(), getDatabase(), getConventions()); //TODO: certificates
        } else {
            executor = RequestExecutor.createForSingleNodeWithConfigurationUpdates(getUrls()[0], getDatabase(), getConventions()); //TODO: certificates
        }

        requestExecutors.put(database, executor);

        return executor;
    }

    /* TODO
        public override IDisposable SetRequestsTimeout(TimeSpan timeout, string database = null)
        {
            AssertInitialized();

            var requestExecutor = GetRequestExecutor(database);
            var oldTimeout = requestExecutor.DefaultTimeout;
            requestExecutor.DefaultTimeout = timeout;

            return new DisposableAction(() =>
            {
                requestExecutor.DefaultTimeout = oldTimeout;
            });
        }

        */

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

                getConventions().setDocumentIdGenerator((dbName, entity) -> generator.generateDocumentId(dbName, entity));
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

        /* TODO

        /// <summary>
        /// Setup the context for no aggressive caching
        /// </summary>
        /// <remarks>
        /// This is mainly useful for internal use inside RavenDB, when we are executing
        /// queries that have been marked with WaitForNonStaleResults, we temporarily disable
        /// aggressive caching.
        /// </remarks>
        public override IDisposable DisableAggressiveCaching(string database = null)
        {
            AssertInitialized();
            var re = GetRequestExecutor(database ?? Database);
            var old = re.AggressiveCaching.Value;
            re.AggressiveCaching.Value = null;
            return new DisposableAction(() => re.AggressiveCaching.Value = old);
        }

        /// <summary>
        /// Subscribe to change notifications from the server
        /// </summary>
        public override IDatabaseChanges Changes(string database = null)
        {
            AssertInitialized();

            return _databaseChanges.GetOrAdd(database ?? Database, CreateDatabaseChanges);
        }

        protected virtual IDatabaseChanges CreateDatabaseChanges(string database)
        {
            return new DatabaseChanges(GetRequestExecutor(database), database, () => _databaseChanges.Remove(database));
        }

        /// <summary>
        /// Setup the context for aggressive caching.
        /// </summary>
        /// <remarks>
        /// Aggressive caching means that we will not check the server to see whether the response
        /// we provide is current or not, but will serve the information directly from the local cache
        /// without touching the server.
        /// </remarks>
        public override IDisposable AggressivelyCacheFor(TimeSpan cacheDuration, string database = null)
        {
            AssertInitialized();
            database = database ?? Database;
            if (_aggressiveCachingUsed == false)
            {
                ListenToChangesAndUpdateTheCache(database);
            }
            var re = GetRequestExecutor(database);
            var old = re.AggressiveCaching.Value;
            re.AggressiveCaching.Value = new AggressiveCacheOptions
            {
                Duration = cacheDuration
            };
            return new DisposableAction(() => re.AggressiveCaching.Value = old);
        }

        private void ListenToChangesAndUpdateTheCache(string database)
        {
            Debug.Assert(database != null);
            // this is intentionally racy, most cases, we'll already
            // have this set once, so we won't need to do it again
            _aggressiveCachingUsed = true;
            if (_aggressiveCacheChanges.TryGetValue(database, out var lazy) == false)
            {
                lazy = _aggressiveCacheChanges.GetOrAdd(database, new Lazy<EvictItemsFromCacheBasedOnChanges>(
                    () => new EvictItemsFromCacheBasedOnChanges(this, database)));
            }
            GC.KeepAlive(lazy.Value); // here we force it to be evaluated
        }

        private AsyncDocumentSession OpenAsyncSessionInternal(SessionOptions options)
        {
            AssertInitialized();
            EnsureNotClosed();

            var sessionId = Guid.NewGuid();
            var databaseName = options.Database ?? Database;
            var requestExecutor = options.RequestExecutor ?? GetRequestExecutor(databaseName);
            var session = new AsyncDocumentSession(databaseName, this, requestExecutor, sessionId);
            //AfterSessionCreated(session);
            return session;
        }

*/

    private List<EventHandler<VoidArgs>> afterDispose = new ArrayList<>();

    public void addAfterCloseListener(EventHandler<VoidArgs> event) {
        this.afterDispose.add(event);
    }

    @Override
    public void removeAfterCloseListener(EventHandler<VoidArgs> event) {
        this.afterDispose.remove(event);
    }

    @Override
    public AdminOperationExecutor admin() {
        assertInitialized();

        if (adminOperationExecutor == null) {
            adminOperationExecutor = new AdminOperationExecutor(this);
        }

        return adminOperationExecutor;
    }

    @Override

    public OperationExecutor operations() {
        if (operationExecutor == null) {
            operationExecutor = new OperationExecutor(this);
        }

        return operationExecutor;
    }


    /* TODO
        public override BulkInsertOperation BulkInsert(string database = null)
        {
            AssertInitialized();
            return new BulkInsertOperation(database ?? Database, this);
        }
    }
     */

}
