package net.ravendb.client.shard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.basic.EventHelper;
import net.ravendb.abstractions.basic.VoidArgs;
import net.ravendb.abstractions.closure.Function1;
import net.ravendb.abstractions.closure.Function2;
import net.ravendb.abstractions.data.BulkInsertOptions;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.util.AtomicDictionary;
import net.ravendb.client.DocumentStoreBase;
import net.ravendb.client.IDocumentSession;
import net.ravendb.client.IDocumentStore;
import net.ravendb.client.changes.IDatabaseChanges;
import net.ravendb.client.connection.IDatabaseCommands;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;
import net.ravendb.client.document.*;
import net.ravendb.client.indexes.AbstractIndexCreationTask;
import net.ravendb.client.indexes.AbstractTransformerCreationTask;

/**
 * Implements a sharded document store
 * Hiding most sharding details behind this and the ShardedDocumentSession gives you the ability to use
 * sharding without really thinking about this too much
 */
public class ShardedDocumentStore extends DocumentStoreBase {

  private String identifier;

  private List<EventHandler<VoidArgs>> afterDispose = new ArrayList<>();

  private final AtomicDictionary<IDatabaseChanges> changes = new AtomicDictionary<>(String.CASE_INSENSITIVE_ORDER);

  private ShardStrategy shardStrategy;



  @Override
  protected void setSharedOperationsHeaders(Map<String, String> sharedOperationsHeaders) {
    throw new UnsupportedOperationException("Sharded document store doesn't have a "
      + "SharedOperationsHeaders. you need to explicitly use the shard instances to get access to the SharedOperationsHeaders");
  }

  @Override
  public Map<String, String> getSharedOperationsHeaders() {
    throw new UnsupportedOperationException("Sharded document store doesn't have a "
      + "SharedOperationsHeaders. you need to explicitly use the shard instances to get access to the SharedOperationsHeaders");
  }

  /**
   * Whatever this instance has json request factory available
   */
  @Override
  public boolean hasJsonRequestFactory() {
    return false;
  }

  @Override
  public HttpJsonRequestFactory getJsonRequestFactory() {
    throw new UnsupportedOperationException("Sharded document store doesn't have a JsonRequestFactory. you need to explicitly use the shard instances to get access to the JsonRequestFactory");
  }

  /**
   * Initializes a new instance of the ShardedDocumentStore class.
   */
  public ShardedDocumentStore(ShardStrategy shardStrategy) {
    if (shardStrategy == null) {
      throw new IllegalArgumentException("Must have shard strategy");
    }
    this.shardStrategy = shardStrategy;
  }

  @Override
  public DocumentConvention getConventions() {
    return shardStrategy.getConventions();
  }

  @Override
  public void setConventions(DocumentConvention conventions) {
    shardStrategy.setConventions(conventions);
  }

  @Override
  public String getIdentifier() {
    return identifier;
  }

  @Override
  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  @Override
  public void addAfterDisposeEventHandler(EventHandler<VoidArgs> event) {
    this.afterDispose.add(event);
  }

  @Override
  public void removeAfterDisposeEventHandler(EventHandler<VoidArgs> event) {
    this.afterDispose.remove(event);
  }

  @Override
  public void close() {
    Map<String, IDocumentStore> shards = shardStrategy.getShards();
    for (IDocumentStore shard: shards.values()) {
      shard.close();
    }

    setWasDisposed(true);
    if (afterDispose != null) {
      EventHelper.invoke(afterDispose, this, null);
    }
  }

  /**
   *  Subscribe to change notifications from the server
   */
  @Override
  public IDatabaseChanges changes() {
    return changes(null);
  }

  /**
   * Subscribe to change notifications from the server
   * @param database
   */
  @Override
  public IDatabaseChanges changes(String database) {
    return changes.getOrAdd(database, new Function1<String, IDatabaseChanges>() {

      @SuppressWarnings("hiding")
      @Override
      public IDatabaseChanges apply(String database) {
        IDatabaseChanges[] array = new IDatabaseChanges[getShardStrategy().getShards().size()];
        int i = 0;
        for (IDocumentStore store : getShardStrategy().getShards().values()) {
          array[i] = store.changes(database);
          i++;
        }
        return new ShardedDatabaseChanges(array);
      }
    });
  }

  /**
   * Setup the context for aggressive caching.
   *
   * aggressive caching means that we will not check the server to see whatever the response
   * we provide is current or not, but will serve the information directly from the local cache
   * without touching the server.
   */
  @Override
  public CleanCloseable aggressivelyCacheFor(long cacheDurationInMilis) {
    Map<String, IDocumentStore> shards = shardStrategy.getShards();
    final List<CleanCloseable> closeables = new ArrayList<>();
    for (IDocumentStore shard : shards.values()) {
      closeables.add(shard.aggressivelyCacheFor(cacheDurationInMilis));
    }

    return new CleanCloseable() {
      @Override
      public void close() {
        for (CleanCloseable closeable: closeables) {
          closeable.close();
        }
      }
    };
  }

  /**
   * Setup the context for no aggressive caching
   *
   * This is mainly useful for internal use inside RavenDB, when we are executing
   * queries that has been marked with WaitForNonStaleResults, we temporarily disable
   * aggressive caching.
   */
  @Override
  public CleanCloseable disableAggressiveCaching() {
    Map<String, IDocumentStore> shards = shardStrategy.getShards();
    final List<CleanCloseable> closeables = new ArrayList<>();
    for (IDocumentStore shard : shards.values()) {
      closeables.add(shard.disableAggressiveCaching());
    }

    return new CleanCloseable() {
      @Override
      public void close() {
        for (CleanCloseable closeable: closeables) {
          closeable.close();
        }
      }
    };
  }

  @Override
  public CleanCloseable setRequestsTimeoutFor(long timeout) {
    Map<String, IDocumentStore> shards = shardStrategy.getShards();
    final List<CleanCloseable> closeables = new ArrayList<>();
    for (IDocumentStore shard : shards.values()) {
      closeables.add(shard.setRequestsTimeoutFor(timeout));
    }

    return new CleanCloseable() {
      @Override
      public void close() {
        for (CleanCloseable closeable: closeables) {
          closeable.close();
        }
      }
    };
  }

  /**
   * Opens the session.
   */
  @Override
  public IDocumentSession openSession() {
    Map<String, IDatabaseCommands> commands = new HashMap<>();
    for (Map.Entry<String, IDocumentStore> shard: shardStrategy.getShards().entrySet()) {
      commands.put(shard.getKey(), shard.getValue().getDatabaseCommands());
    }
    return openSessionInternal(null, commands);
  }

  /**
   * Opens the session for a particular database
   */
  @Override
  public IDocumentSession openSession(String database) {
    Map<String, IDatabaseCommands> commands = new HashMap<>();
    for (Map.Entry<String, IDocumentStore> shard: shardStrategy.getShards().entrySet()) {
      commands.put(shard.getKey(), shard.getValue().getDatabaseCommands().forDatabase(database));
    }
    return openSessionInternal(database, commands);
  }

  /**
   *  Opens the session with the specified options.
   */
  @Override
  public IDocumentSession openSession(OpenSessionOptions sessionOptions) {
    Map<String, IDatabaseCommands> commands = new HashMap<>();
    for (Map.Entry<String, IDocumentStore> shard: shardStrategy.getShards().entrySet()) {
      commands.put(shard.getKey(), shard.getValue().getDatabaseCommands().forDatabase(sessionOptions.getDatabase()));
    }
    return openSessionInternal(sessionOptions.getDatabase(), commands);
  }

  private IDocumentSession openSessionInternal(String database, Map<String, IDatabaseCommands> shardDbCommands) {
    ensureNotClosed();

    UUID sessionId = UUID.randomUUID();
    ShardedDocumentSession session = new ShardedDocumentSession(database, this, getListeners(), sessionId, shardStrategy, shardDbCommands);
    session.setDatabaseName(database);
    afterSessionCreated(session);
    return session;
  }

  /**
   * Gets the database commands.
   */
  @Override
  public IDatabaseCommands getDatabaseCommands() {
    throw new UnsupportedOperationException("Sharded document store doesn't have a database commands. you need to explicitly use the shard instances to get access to the database commands");
  }

  @Override
  public String getUrl() {
    throw new UnsupportedOperationException("There isn't a singular url when using sharding");
  }

  public ShardStrategy getShardStrategy() {
    return shardStrategy;
  }

  /**
   * Gets the etag of the last document written by any session belonging to this
   */
  @Override
  public Etag getLastWrittenEtag() {
    throw new UnsupportedOperationException("This isn't a single last written etag when sharding");
  }

  /**
   * Cannot use BulkInsert using sharded store, use shardedBulkInsert, instead
   */
  @Override
  @Deprecated
  public BulkInsertOperation bulkInsert() {
    return bulkInsert(null, null);
  }

  /**
   * Cannot use BulkInsert using sharded store, use shardedBulkInsert, instead
     */
  @Override
  public BulkInsertOperation bulkInsert(String database) {
    return bulkInsert(database, null);

  }

  /**
   * Cannot use BulkInsert using sharded store, use shardedBulkInsert, instead
     */
  @Override
  public BulkInsertOperation bulkInsert(String database, BulkInsertOptions options) {
    throw new UnsupportedOperationException("Cannot use BulkInsert using sharded store, use shardedBulkInsert, instead");
  }

  public ShardedBulkInsertOperation shardedBulkInsert() {
    return new ShardedBulkInsertOperation(null, null, null);
  }

  public ShardedBulkInsertOperation shardedBulkInsert(String database) {
    return new ShardedBulkInsertOperation(database, null, null);
  }

  public ShardedBulkInsertOperation shardedBulkInsert(String database, ShardedDocumentStore store, BulkInsertOptions options) {
    //TODO: store param isn't used!
    return new ShardedBulkInsertOperation(database, this, options != null ? options : new BulkInsertOptions());
  }


  @Override
  public void initializeProfiling() {
    Map<String, IDocumentStore> shards = shardStrategy.getShards();
    for (IDocumentStore store: shards.values()) {
      store.initializeProfiling();
    }
  }

  /**
   * Initializes this instance.
   * @return
   */
  @Override
  public IDocumentStore initialize() {
    try {
      Map<String, IDocumentStore> shards = shardStrategy.getShards();
      for (IDocumentStore store : shards.values()) {
        store.initialize();
      }

      Map<UUID, Integer> shardsPointingToSameDb = new HashMap<>();

      for (Map.Entry<String, IDocumentStore> x : shardStrategy.getShards().entrySet()) {
        try {
          UUID databaseId = x.getValue().getDatabaseCommands().getStatistics().getDatabaseId();
          if (shardsPointingToSameDb.containsKey(x.getKey())) {
            shardsPointingToSameDb.put(databaseId, shardsPointingToSameDb.get(x.getKey()) + 1);
          } else {
            shardsPointingToSameDb.put(databaseId, 1);
          }
        } catch (Exception e) {
          // we ignore connection error here
        }
      }

      for (Map.Entry<UUID, Integer> entry : shardsPointingToSameDb.entrySet()) {
        if (entry.getValue() > 1) {
          throw new UnsupportedOperationException("Multiple keys in shard map are not supported. Duplicate database id = " + entry.getKey());
        }
      }

      if (getConventions().getDocumentKeyGenerator() == null) { // don't overwrite what the user is doing
        final ShardedHiloKeyGenerator generator = new ShardedHiloKeyGenerator(this, 32);
        getConventions().setDocumentKeyGenerator(new DocumentKeyGenerator() {
          @Override
          public String generate(String dbName, IDatabaseCommands dbCommands, Object entity) {
            return generator.generateDocumentKey(dbCommands, getConventions(), entity);
          }
        });
      }
    } catch (Exception e) {
      close();
      throw e;
    }

    return this;
  }

  public IDatabaseCommands databaseCommandsFor(String shardId) {
    IDocumentStore store = shardStrategy.getShards().get(shardId);
    if (store == null) {
      throw new IllegalArgumentException("Could not find a shard named: " + shardId);
    }
    return store.getDatabaseCommands();
  }

  /**
   * Executes the transformer creation
   */
  @Override
  public void executeTransformer(final AbstractTransformerCreationTask transformerCreationTask) {
    Collection<IDocumentStore> stores = shardStrategy.getShards().values();
    List<IDatabaseCommands> commands = new ArrayList<>();
    for (IDocumentStore store : stores) {
      commands.add(store.getDatabaseCommands());
    }

    shardStrategy.getShardAccessStrategy().apply(Void.class, commands, new ShardRequestData(), new Function2<IDatabaseCommands, Integer, Void>() {
      @SuppressWarnings("hiding")
      @Override
      public Void apply(IDatabaseCommands commands, Integer i) {
        transformerCreationTask.execute(commands, getConventions());
        return null;
      }
    });
  }

  /**
   * Executes the index creation against each of the shards.
   */
  @Override
  public void executeIndex(final AbstractIndexCreationTask indexCreationTask) {
    Collection<IDocumentStore> stores = shardStrategy.getShards().values();
    List<IDatabaseCommands> commands = new ArrayList<>();
    for (IDocumentStore store : stores) {
      commands.add(store.getDatabaseCommands());
    }

    shardStrategy.getShardAccessStrategy().apply(Void.class, commands, new ShardRequestData(), new Function2<IDatabaseCommands, Integer, Void>() {
      @SuppressWarnings("hiding")
      @Override
      public Void apply(IDatabaseCommands commands, Integer i) {
        indexCreationTask.execute(commands, getConventions());
        return null;
      }
    });
  }

  @Override
  public void sideBySideExecuteIndex(final AbstractIndexCreationTask indexCreationTask, final Etag minimumEtagBeforeReplace,
    final Date replaceTimeUtc) {
    Collection<IDocumentStore> stores = shardStrategy.getShards().values();
    List<IDatabaseCommands> commands = new ArrayList<>();
    for (IDocumentStore store : stores) {
      commands.add(store.getDatabaseCommands());
    }

    shardStrategy.getShardAccessStrategy().apply(Void.class, commands, new ShardRequestData(), new Function2<IDatabaseCommands, Integer, Void>() {
      @SuppressWarnings("hiding")
      @Override
      public Void apply(IDatabaseCommands commands, Integer i) {
        indexCreationTask.sideBySideExecute(commands, getConventions(), minimumEtagBeforeReplace, replaceTimeUtc);
        return null;
      }
    });
  }
}
