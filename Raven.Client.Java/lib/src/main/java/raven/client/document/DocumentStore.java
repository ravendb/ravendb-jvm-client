package raven.client.document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;

import org.apache.commons.lang.StringUtils;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import raven.abstractions.basic.EventHandler;
import raven.abstractions.basic.EventHelper;
import raven.abstractions.basic.VoidArgs;
import raven.abstractions.closure.Action1;
import raven.abstractions.closure.Function0;
import raven.abstractions.closure.Function1;
import raven.abstractions.closure.Function3;
import raven.abstractions.connection.profiling.RequestResultArgs;
import raven.abstractions.data.ConnectionStringParser;
import raven.abstractions.data.Constants;
import raven.abstractions.data.RavenConnectionStringOptions;
import raven.client.DocumentStoreBase;
import raven.client.IDocumentSession;
import raven.client.IDocumentStore;
import raven.client.changes.IDatabaseChanges;
import raven.client.connection.ICredentials;
import raven.client.connection.IDatabaseCommands;
import raven.client.connection.NetworkCredential;
import raven.client.connection.ReplicationInformer;
import raven.client.connection.ServerClient;
import raven.client.connection.implementation.HttpJsonRequestFactory;
import raven.client.extensions.MultiDatabase;
import raven.client.listeners.IDocumentConflictListener;
import raven.client.util.AtomicDictionary;
import raven.client.util.EvictItemsFromCacheBasedOnChanges;
import raven.client.utils.Closer;

/**
 * Manages access to RavenDB and open sessions to work with RavenDB.
 */
public class DocumentStore extends DocumentStoreBase {

  // The current session id - only used during construction
  protected static ThreadLocal<UUID> currentSessionId;

  private final static int DEFAULT_NUMBER_OF_CACHED_REQUESTS = 2048;
  private int maxNumberOfCachedRequests = DEFAULT_NUMBER_OF_CACHED_REQUESTS;
  private boolean aggressiveCachingUsed;

  protected Function0<IDatabaseCommands> databaseCommandsGenerator;
  private final ConcurrentMap<String, ReplicationInformer> replicationInformers = new ConcurrentHashMap<>();
  private String identifier;
  private ICredentials credentials;


  private final AtomicDictionary<IDatabaseChanges> databaseChanges = new AtomicDictionary<>();
  private HttpJsonRequestFactory jsonRequestFactory = new HttpJsonRequestFactory(DEFAULT_NUMBER_OF_CACHED_REQUESTS);


  private ConcurrentMap<String, EvictItemsFromCacheBasedOnChanges> observeChangesAndEvictItemsFromCacheForDatabases = new ConcurrentHashMap<>();

  private String apiKey;
  private String defaultDatabase;

  /**
   * Called after dispose is completed
   */
  private List<EventHandler<VoidArgs>> afterDispose = new ArrayList<>();


  /**
   * Called after dispose is completed
   * @param event
   */
  public void addAfterDisposeEventHandler(EventHandler<VoidArgs> event) {
    this.afterDispose.add(event);
  }

  public String getDefaultDatabase() {
    return defaultDatabase;
  }

  public void setDefaultDatabase(String defaultDatabase) {
    this.defaultDatabase = defaultDatabase;
  }

  /**
   * Remove event handler
   * @param event
   */
  public void removeAfterDisposeEventHandler(EventHandler<VoidArgs> event) {
    this.afterDispose.remove(event);
  }


  public boolean hasJsonRequestFactory() {
    return true;
  }
  public HttpJsonRequestFactory getJsonRequestFactory() {
    return jsonRequestFactory;
  }

  public IDatabaseCommands getDatabaseCommands() {
    assertInitialized();
    IDatabaseCommands commands = databaseCommandsGenerator.apply();
    for (String key: getSharedOperationsHeaders().keySet()) {
      String value = getSharedOperationsHeaders().get(key);
      if (value == null) {
        continue;
      }
      commands.getOperationsHeaders().put(key, value);
    }
    return commands;
  }

  public DocumentStore() {
    credentials = new NetworkCredential();
    setResourceManagerId(UUID.fromString("E749BAA6-6F76-4EEF-A069-40A4378954F8"));

    setSharedOperationsHeaders(new HashMap<String, String>());
    setConventions(new DocumentConvention());
  }

  public ICredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(ICredentials credentials) {
    this.credentials = credentials;
  }

  public String getIdentifier() {
    if (identifier != null) {
      return identifier;
    }
    if (getUrl() == null) {
      return null;
    }
    if (defaultDatabase != null) {
      return getUrl() + " (DB: " + defaultDatabase + ")";
    }
    return url;
  }

  public void setIdentifier(String value) {
    this.identifier = value;
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  /**
   *  Set document store settings based on a given connection string.
   *  Ex. Url=http://localhost:8123;
   * @param connString
   */
  public void parseConnectionString(String connString) {
    ConnectionStringParser<RavenConnectionStringOptions> connectionStringOptions = ConnectionStringParser.fromConnectionString(RavenConnectionStringOptions.class, connString);
    connectionStringOptions.parse();
    setConnectionStringSettings(connectionStringOptions.getConnectionStringOptions());
  }
  /**
   *  Copy the relevant connection string settings
   */
  protected void setConnectionStringSettings(RavenConnectionStringOptions options) {
    if (options.getResourceManagerId() != null) {
      setResourceManagerId(options.getResourceManagerId());
    }
    if (options.getCredentials() != null) {
      setCredentials(options.getCredentials());
    }
    if (StringUtils.isNotEmpty(options.getUrl())) {
      setUrl(options.getUrl());
    }
    if (StringUtils.isNotEmpty(options.getDefaultDatabase())) {
      defaultDatabase = options.getDefaultDatabase();
    }
    if (StringUtils.isNotEmpty(options.getApiKey())) {
      apiKey = options.getApiKey();
    }
    setEnlistInDistributedTransactions(options.isEnlistInDistributedTransactions());
  }


  @Override
  public void close() throws Exception {
    for (EvictItemsFromCacheBasedOnChanges observeChangesAndEvictItemsFromCacheForDatabase : observeChangesAndEvictItemsFromCacheForDatabases.values()) {
      observeChangesAndEvictItemsFromCacheForDatabase.close();
    }

    List<Future<?>> tasks = new ArrayList<>();

    /*
     * TODO:
    for (Map.Entry<String, IDatabaseChanges> databaseChange: databaseChanges) {
      IDatabaseChanges dbChange = databaseChange.getValue();
      if (dbChange instanceof RemoteDatabaseChanges) {
        tasks.add(((RemoteDatabaseChanges) dbChange).disposeAsync());
      } else {
        if (databaseChange.getValue() instanceof Closeable) {
          ((Closeable)databaseChange.getValue()).close();
        }
      }
    }*/

    for (ReplicationInformer ri : replicationInformers.values()) {
      ri.close();
    }

    for (Future<?> future: tasks) {
      future.wait(3000);
    }

    // if this is still going, we continue with disposal, it is for grace only, anyway

    if (jsonRequestFactory != null) {
      jsonRequestFactory.close();
    }

    setWasDisposed(true);
    if (afterDispose != null) {
      EventHelper.invoke(afterDispose, this, null);
    }
  }

  /**
   * Opens the session.
   */
  @Override
  public IDocumentSession openSession() {
    return openSession(new OpenSessionOptions());
  }

  /**
   * Opens the session for a particular database
   */
  @Override
  public IDocumentSession openSession(String database) {
    OpenSessionOptions opts = new OpenSessionOptions();
    opts.setDatabase(database);
    return openSession(opts);
  }

  @Override
  public IDocumentSession openSession(OpenSessionOptions options) {
    ensureNotClosed();

    UUID sessionId = UUID.randomUUID();
    currentSessionId.set(sessionId);
    try {
      DocumentSession session = new DocumentSession(options.getDatabase(), this, listeners, sessionId,
          setupCommands(getDatabaseCommands(), options.getDatabase(), options.getCredentials(), options));
      session.setDatabaseName(options.getDatabase() != null ? options.getDatabase() : defaultDatabase);

      afterSessionCreated(session);
      return session;
    } finally {
      currentSessionId = null;
    }
  }

  private static IDatabaseCommands setupCommands(IDatabaseCommands databaseCommands, String database, ICredentials credentialsForSession, OpenSessionOptions options) {
    if (database != null) {
      databaseCommands = databaseCommands.forDatabase(database);
    }
    if (credentialsForSession != null) {
      databaseCommands = databaseCommands.with(credentialsForSession);
    }
    if (options.isForceReadFromMaster()) {
      databaseCommands.forceReadFromMaster();
    }
    return databaseCommands;
  }

  @Override
  public IDocumentStore initialize() {
    if (initialized) {
      return this;
    }

    assertValidConfiguration();

    jsonRequestFactory = new HttpJsonRequestFactory(getMaxNumberOfCachedRequests());
    try {
      initializeSecurity();

      initializeInternal();

      if (conventions.getDocumentKeyGenerator() == null) { // don't overwrite what the user is doing
        final MultiDatabaseHiLoGenerator generator = new MultiDatabaseHiLoGenerator(32);
        conventions.setDocumentKeyGenerator(new Function3<String, IDatabaseCommands, Object, String>() {

          @Override
          public String apply(String dbName, IDatabaseCommands databaseCommands, Object entity) {
            return generator.generateDocumentKey(dbName, databaseCommands, conventions, entity);
          }
        });
      }

      initialized = true;

      recoverPendingTransactions();

      if (StringUtils.isNotEmpty(defaultDatabase)) {
        getDatabaseCommands().forSystemDatabase().getAdmin().ensureDatabaseExists(defaultDatabase, true);
      }
    } catch (Exception e) {
      Closer.close(this);
      throw e;
    }

    return this;
  }

  public void initializeProfiling() {
    if (jsonRequestFactory == null) {
      throw new IllegalStateException("Cannot call InitializeProfiling() before Initialize() was called.");
    }
    conventions.setDisableProfiling(false);
    jsonRequestFactory.addLogRequestEventHandler(new EventHandler<RequestResultArgs>() {

      @Override
      public void handle(Object sender, final RequestResultArgs args) {
        if (conventions.isDisableProfiling()) {
          return;
        }
        if (args.getTotalSize() > 1024 * 1024 * 2) {
          RequestResultArgs newArgs = new RequestResultArgs();
          newArgs.setUrl(args.getUrl());
          newArgs.setPostedData("total request/response size > 2MB, not tracked");
          newArgs.setResult("total request/response size > 2MB, not tracked");
          profilingContext.recordAction(sender, newArgs);
          return;
        }
        profilingContext.recordAction(sender, args);
      }
    });
  }

  private void recoverPendingTransactions() {
    if (!getEnlistInDistributedTransactions())
      return;

    /*TODO:
    var pendingTransactionRecovery = new PendingTransactionRecovery(this);
    pendingTransactionRecovery.Execute(ResourceManagerId, DatabaseCommands);
     */
  }

  private void initializeSecurity() {
    /*TODO: initializeSecurity*/
  }

  /**
   * validate the configuration for the document store
   */
  protected void assertValidConfiguration() {
    if (StringUtils.isEmpty(url)) {
      throw new IllegalArgumentException("Document store URL cannot be empty");
    }
  }

  /**
   * Initialize the document store access method to RavenDB
   */
  protected void initializeInternal() {

    final String rootDatabaseUrl = MultiDatabase.getRootDatabaseUrl(url);
    HttpParams httpParams = jsonRequestFactory.getHttpClient().getParams();
    httpParams.setBooleanParameter(HttpConnectionParams.TCP_NODELAY, true); // disable Nagle's algorithm

    databaseCommandsGenerator = new Function0<IDatabaseCommands>() {

      @Override
      public IDatabaseCommands apply() {
        String databaseUrl = getUrl();
        if (StringUtils.isNotEmpty(defaultDatabase)) {
          databaseUrl = rootDatabaseUrl;
          databaseUrl += "/databases/" + defaultDatabase;
        }

        return new ServerClient(databaseUrl, conventions, getCredentials(),
            new ReplicationInformerGetter()
        , null, jsonRequestFactory, currentSessionId.get(), listeners.getConflictListeners().toArray(new IDocumentConflictListener[0]));
      }
    };

  }
  private class ReplicationInformerGetter implements Function1<String, ReplicationInformer> {

    @Override
    public ReplicationInformer apply(String dbName) {
      return getReplicationInformerForDatabase(dbName);
    }

  }


  public ReplicationInformer getReplicationInformerForDatabase() {
    return getReplicationInformerForDatabase(null);
  }

  public ReplicationInformer getReplicationInformerForDatabase(String dbName) {
    String key = url;
    if (dbName == null) {
      dbName = defaultDatabase;
    }
    if (StringUtils.isNotEmpty(dbName)) {
      key = MultiDatabase.getRootDatabaseUrl(url) + "/databases/" + dbName;
    }
    return replicationInformers.putIfAbsent(key, conventions.getReplicationInformerFactory().apply(key));
  }

  /**
   * Setup the context for no aggressive caching
   *
   * This is mainly useful for internal use inside RavenDB, when we are executing
   * queries that have been marked with WaitForNonStaleResults, we temporarily disable
   * aggressive caching.
   */
  @Override
  public AutoCloseable disableAggressiveCaching() {
    assertInitialized();
    final Long old = jsonRequestFactory.getAggressiveCacheDuration();
    jsonRequestFactory.setAggressiveCacheDuration(null);
    return new AutoCloseable() {

      @Override
      public void close() throws Exception {
        jsonRequestFactory.setAggressiveCacheDuration(old);
      }
    };
  }

  /**
   *  Subscribe to change notifications from the server
   */
  public IDatabaseChanges changes() {
    return changes(null);
  }

  /**
   * Subscribe to change notifications from the server
   * @param database
   * @return
   */
  @Override
  public IDatabaseChanges changes(String database) {
    assertInitialized();
    if (database == null) {
      database = defaultDatabase;
    }
    return databaseChanges.getOrAdd(database, new Function1<String, IDatabaseChanges>() {

      @Override
      public IDatabaseChanges apply(String database) {
        return createDatabaseChanges(database);
      }
    });
  }

  protected IDatabaseChanges createDatabaseChanges(String database) {
    /*TODO:
      if (StringUtils.isEmpty(url)) {
        throw new IllegalStateException("Changes API requires usage of server/client");
      }

      if (database == null) {
        database = defaultDatabase;
      }

      String dbUrl = MultiDatabase.getRootDatabaseUrl(url);
      if (StringUtils.isNotEmpty(database)) {
        dbUrl = dbUrl + "/databases/" + database;
      }

      return new RemoteDatabaseChanges(dbUrl,
          Credentials,
          jsonRequestFactory,
          Conventions,
          GetReplicationInformerForDatabase(database),
          () => databaseChanges.Remove(database),
          ((AsyncServerClient)AsyncDatabaseCommands).TryResolveConflictByUsingRegisteredListenersAsync);
     */
    return null; //TODO:
  }

  /**
   * Setup the context for aggressive caching.
   *
   * Aggressive caching means that we will not check the server to see whatever the response
   * we provide is current or not, but will serve the information directly from the local cache
   * without touching the server.
   */
  @Override
  public AutoCloseable aggressivelyCacheFor(long cacheDurationInMilis)
  {
    assertInitialized();
    if (cacheDurationInMilis < 1000)
      throw new IllegalArgumentException("cacheDuration must be longer than a single second");

    final Long old = jsonRequestFactory.getAggressiveCacheDuration();
    jsonRequestFactory.setAggressiveCacheDuration(cacheDurationInMilis);

    aggressiveCachingUsed = true;

    return new AutoCloseable() {

      @Override
      public void close() throws Exception {
        jsonRequestFactory.setAggressiveCacheDuration(old);
      }
    };
  }



  public int getMaxNumberOfCachedRequests() {
    return maxNumberOfCachedRequests;
  }

  public void setMaxNumberOfCachedRequests(int value) {
    maxNumberOfCachedRequests = value;
    if (jsonRequestFactory != null) {
      Closer.close(jsonRequestFactory);
    }
    jsonRequestFactory = new HttpJsonRequestFactory(maxNumberOfCachedRequests);
  }


  /*TODO:
  public override BulkInsertOperation BulkInsert(string database = null, BulkInsertOptions options = null)
  {
    return new BulkInsertOperation(database ?? DefaultDatabase, this, listeners, options ?? new BulkInsertOptions(), Changes(database ?? DefaultDatabase));
  }*/

  @Override
  protected void afterSessionCreated(InMemoryDocumentSessionOperations session) {
    if (conventions.isShouldAggressiveCacheTrackChanges() && aggressiveCachingUsed) {
      String databaseName = session.getDatabaseName();
      if (databaseName == null) {
        databaseName = Constants.SYSTEM_DATABASE;
      }

      observeChangesAndEvictItemsFromCacheForDatabases.putIfAbsent(databaseName,
          new EvictItemsFromCacheBasedOnChanges(databaseName, createDatabaseChanges(databaseName), new ExpireItemsFromCacheAction()));
    }

    super.afterSessionCreated(session);
  }
  private class ExpireItemsFromCacheAction implements Action1<String>  {

    @Override
    public void apply(String db) {
      jsonRequestFactory.expireItemsFromCache(db);
    }
  }



  /*TODO
    public Task GetObserveChangesAndEvictItemsFromCacheTask(string database = null)
    {
      var changes = observeChangesAndEvictItemsFromCacheForDatabases.GetOrDefault(database ?? Constants.SystemDatabase);
      return changes == null ? new CompletedTask() : changes.ConnectionTask;
    }
   */
}
