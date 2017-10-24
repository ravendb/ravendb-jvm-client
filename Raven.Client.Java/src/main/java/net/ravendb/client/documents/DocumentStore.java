package net.ravendb.client.documents;

import net.ravendb.client.documents.operations.AdminOperationExecutor;
import net.ravendb.client.documents.operations.OperationExecutor;
import net.ravendb.client.http.RequestExecutor;
import net.ravendb.client.primitives.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    //TODO: private AsyncMultiDatabaseHiLoIdGenerator _asyncMultiDbHiLo;

    private AdminOperationExecutor adminOperationExecutor;
    private OperationExecutor operationExecutor;

    //TODO: private DatabaseSmuggler _smuggler;

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

            //return unused hilo keys
            if (_asyncMultiDbHiLo != null)
            {
                try
                {
                    AsyncHelpers.RunSync(() => _asyncMultiDbHiLo.ReturnUnusedRange());
                }
                catch
                {
                    // failed, because server is down.
                }
            }

            Subscriptions?.Dispose();
*/
        disposed = true;
        EventHelper.invoke(afterDispose, this, EventArgs.EMPTY);

        for (Map.Entry<String, RequestExecutor> kvp : requestExecutors.entrySet()) {
            kvp.getValue().close();
        }
    }



    /*TODO

        /// <summary>
        /// Opens the session.
        /// </summary>
        /// <returns></returns>
        public override IDocumentSession OpenSession()
        {
            return OpenSession(new SessionOptions());
        }

        /// <summary>
        /// Opens the session for a particular database
        /// </summary>
        public override IDocumentSession OpenSession(string database)
        {
            return OpenSession(new SessionOptions
            {
                Database = database
            });
        }


        public override IDocumentSession OpenSession(SessionOptions options)
        {
            AssertInitialized();
            EnsureNotClosed();

            var sessionId = Guid.NewGuid();
            var databaseName = options.Database ?? Database;
            var requestExecutor = options.RequestExecutor ?? GetRequestExecutor(databaseName);
            var session = new DocumentSession(databaseName, this, sessionId, requestExecutor);
            RegisterEvents(session);
            // AfterSessionCreated(session);
            return session;
        }

        public event EventHandler<RequestExecutor> RequestExecutorCreated;
*/

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

        if (!getConventions().getDisableTopologyUpdates()) {
            executor = RequestExecutor.create(getUrls(), getDatabase(), getConventions()); //TODO: certificates
            //TODO:  RequestExecutorCreated?.Invoke(this, requestExecutor);
        } else {
            executor = RequestExecutor.createForSingleNodeWithConfigurationUpdates(getUrls()[0], getDatabase(), getConventions()); //TODO: certificates
            //tODO: RequestExecutorCreated?.Invoke(this, forSingleNode);
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
            /* TODO
             if (Conventions.AsyncDocumentIdGenerator == null) // don't overwrite what the user is doing
                {
                    var generator = new AsyncMultiDatabaseHiLoIdGenerator(this, Conventions);
                    _asyncMultiDbHiLo = generator;
                    Conventions.AsyncDocumentIdGenerator = (dbName, entity) => generator.GenerateDocumentIdAsync(dbName, entity);
                }
             */

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

        /// <summary>
        /// Opens the async session.
        /// </summary>
        /// <returns></returns>
        public override IAsyncDocumentSession OpenAsyncSession(string database)
        {
            return OpenAsyncSession(new SessionOptions
            {
                Database = database
            });
        }

        public override IAsyncDocumentSession OpenAsyncSession(SessionOptions options)
        {
            return OpenAsyncSessionInternal(options);
        }

        public override IAsyncDocumentSession OpenAsyncSession()
        {
            return OpenAsyncSessionInternal(new SessionOptions());
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

    //TODO: public DatabaseSmuggler Smuggler => _smuggler ?? (_smuggler = new DatabaseSmuggler(this));

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
