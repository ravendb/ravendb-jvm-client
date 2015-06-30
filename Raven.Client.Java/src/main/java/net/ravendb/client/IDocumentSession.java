package net.ravendb.client;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.client.document.ILoaderWithInclude;
import net.ravendb.client.indexes.AbstractIndexCreationTask;
import net.ravendb.client.indexes.AbstractTransformerCreationTask;
import net.ravendb.client.linq.IRavenQueryable;
import net.ravendb.client.shard.ShardReduceFunction;

import com.mysema.query.types.Expression;


/**
 * Interface for document session
 */
public interface IDocumentSession extends CleanCloseable {

  /**
   * Get the accessor for advanced operations
   *
   * Those operations are rarely needed, and have been moved to a separate
   * property to avoid cluttering the API
   */
  public ISyncAdvancedSessionOperation advanced();

  /**
   * Marks the specified entity for deletion. The entity will be deleted when IDocumentSession.SaveChanges is called.
   * @param entity
   */
  public <T> void delete(T entity);

  /**
   * Marks the specified entity for deletion. The entity will be deleted when IDocumentSession.saveChanges is called.
   * WARNING: This method will not call beforeDelete listener!
   * This method allows you to call:
   * Delete&lt;Post&gt;(1)
   * And that call will internally be translated to
   * Delete&lt;Post&gt;("posts/1");
   * Or whatever your conventions specify.
   * @param id Entity Id
   */
  public <T> void delete(Class<T> clazz, Number id);

  /**
   * Marks the specified entity for deletion. The entity will be deleted when IDocumentSession.saveChanges is called.
   * WARNING: This method will not call beforeDelete listener!
   * This method allows you to call:
   * Delete&lt;Post&gt;(1)
   * And that call will internally be translated to
   * Delete&lt;Post&gt;("posts/1");
   * Or whatever your conventions specify.
   * @param id Entity Id
   */
  public <T> void delete(Class<T> clazz, UUID id);

  /**
   * Marks the specified entity for deletion. The entity will be deleted when IDocumentSession.saveChanges is called.
   * WARNING: This method will not call beforeDelete listener!
   * This method allows you to call:
   * Delete&lt;Post&gt;(1)
   * And that call will internally be translated to
   * Delete&lt;Post&gt;("posts/1");
   * Or whatever your conventions specify.
   * @param id Entity Id
   */
  public void delete(String id);

  /**
   * Loads the specified entity with the specified id.
   * @param clazz Defines type of object
   * @param id Identifier of a entity that will be loaded.
   */
  public <T> T load(Class<T> clazz, String id);

  /**
   * Loads the specified entities with the specified ids.
   * @param clazz Defines type of object
   * @param ids Array of Ids that should be loaded
   */
  public <T> T[] load(Class<T> clazz, String...ids);

  /**
   * Loads the specified entities with the specified ids.
   * @param clazz Defines type of object
   * @param ids Collection of Ids that should be loaded
   */
  public <T> T[] load(Class<T> clazz, Collection<String> ids);

  /**
   * Loads the specified entity with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   * Load(Post.class, 1)
   *
   * And that call will internally be translated to
   * Load(Post.class, "posts/1");
   *
   * Or whatever your conventions specify.
   * @param clazz Defines type of object
   * @param id Identifier of a entity that will be loaded.
   */
  public <T> T load(Class<T> clazz, Number id);

  /**
   * Loads the specified entity with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   * Load(Post.class, 1)
   *
   * And that call will internally be translated to
   * Load(Post.class, "posts/1");
   *
   * Or whatever your conventions specify.
   * @param clazz Defines type of object
   * @param id Identifier of a entity that will be loaded.
   */
  public <T> T load(Class<T> clazz, UUID id);

  /**
   * Loads the specified entities with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   * Load(Post.class, 1,2,3)
   * And that call will internally be translated to
   * Load(Post.class, "posts/1","posts/2","posts/3");
   *
   * Or whatever your conventions specify.
   * @param clazz Defines type of object
   * @param ids Collection of Ids that should be loaded
   */
  public <T> T[] load(Class<T> clazz, Number... ids);

  /**
   * Loads the specified entities with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   * Load(Post.class, 1,2,3)
   * And that call will internally be translated to
   * Load(Post.class, "posts/1","posts/2","posts/3");
   *
   * Or whatever your conventions specify.
   * @param clazz Defines type of object
   * @param ids Collection of Ids that should be loaded
   */
  public <T> T[] load(Class<T> clazz, UUID... ids);

  /**
   * Queries the specified index.
   * @param clazz Defines type of object
   * @param indexName Name of the index.
   */
  public <T> IRavenQueryable<T> query(Class<T> clazz, String indexName);

  /**
   * Queries the specified index.
   * @param clazz Defines type of object
   * @param indexName Name of the index.
   * @param isMapReduce Whatever we are querying a map/reduce index (modify how we treat identifier properties)
   */
  public <T> IRavenQueryable<T> query(Class<T> clazz, String indexName, boolean isMapReduce);

  /**
   * Dynamically queries RavenDB.
   * @param clazz Defines type of object
   */
  public <T> IRavenQueryable<T> query(Class<T> clazz);

  /**
   * Queries the index specified by indexCreator.
   * @param clazz Defines type of object
   * @param indexCreator
   */
  public <T> IRavenQueryable<T> query(Class<T> clazz, Class<? extends AbstractIndexCreationTask> indexCreator);

  /**
   * Queries the index specified by indexCreator.
   *
   * This method only makes sense for ShardedDocumentStore + Reduce Indexes
   * @param clazz
   * @param indexCreator
   * @param reduceFunction
   * @return
   */
  public <T> IRavenQueryable<T> query(Class<T> clazz, Class<? extends AbstractIndexCreationTask> indexCreator, ShardReduceFunction reduceFunction);

  /**
   * Begin a load while including the specified path
   * @param path Path in documents in which server should look for a 'referenced' documents.
   */
  public ILoaderWithInclude include(String path);

  /**
   * Begin a load while including the specified path
   * @param path Path in documents in which server should look for a 'referenced' documents.
   */
  public ILoaderWithInclude include(Expression<?> path);

  /**
   * Begin a load while include the specified path
   * @param targetEntityClass Target entity class (used for id generation)
   * @param path Path in documents in which server should look for a 'referenced' documents.
   */
  public ILoaderWithInclude include(Class<?> targetEntityClass, Expression<?> path);

  /**
   * Performs a load that will use the specified results transformer against the specified id
   * @param tranformerClass The transformer to use in this load operation
   * @param clazz The results shape to return after the load operation
   * @param id Identifier of a entity that will be loaded.
   */
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult load(Class<TTransformer> tranformerClass,
      Class<TResult> clazz, String id);

  /**
   * Performs a load that will use the specified results transformer against the specified id
   * @param tranformerClass The transformer to use in this load operation
   * @param clazz The results shape to return after the load operation
   * @param id Identifier of a entity that will be loaded.
   * @param configure Additional configuration options for operation e.g. AddTransformerParameter
   */
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult load(Class<TTransformer> tranformerClass,
      Class<TResult> clazz, String id, LoadConfigurationFactory configure);

  /**
   * Performs a load that will use the specified results transformer against the specified id
   * @param tranformerClass The transformer to use in this load operation
   * @param clazz The results shape to return after the load operation
   * @param ids Array of ids of documents to load
   */
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] load(Class<TTransformer> tranformerClass,
      Class<TResult> clazz, String... ids);

  /**
   * Performs a load that will use the specified results transformer against the specified id
   * @param tranformerClass The transformer to use in this load operation
   * @param clazz The results shape to return after the load operation
   * @param ids Array of ids of documents to load
   * @param configure Additional configuration options for operation e.g. AddTransformerParameter
   */
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] load(Class<TTransformer> tranformerClass,
      Class<TResult> clazz, List<String> ids, LoadConfigurationFactory configure);

  /**
   * Performs a load that will use the specified results transformer against the specified id
   * @param clazz The results shape to return after the load operation
   * @param transformer The transformer to use in this load operation
   * @param id Identifier of a entity that will be loaded.
   */
  public <TResult> TResult load(Class<TResult> clazz, String transformer, String id);

  /**
   * Performs a load that will use the specified results transformer against the specified id
   * @param clazz The results shape to return after the load operation
   * @param transformer The transformer to use in this load operation
   * @param id Identifier of a entity that will be loaded.
   * @param configure Additional configuration options for operation e.g. AddTransformerParameter
   */
  public <TResult> TResult load(Class<TResult> clazz, String transformer, String id, LoadConfigurationFactory configure);

  /**
   * Performs a load that will use the specified results transformer against the specified id
   * @param clazz The results shape to return after the load operation
   * @param transformer The transformer to use in this load operation
   * @param ids Array of ids of documents to load
   */
  public <TResult> TResult[] load(Class<TResult> clazz, String transformer, Collection<String> ids);

  /**
   * Performs a load that will use the specified results transformer against the specified id
   * @param clazz The results shape to return after the load operation
   * @param transformer The transformer to use in this load operation
   * @param ids Array of ids of documents to load
   * @param configure Additional configuration options for operation e.g. AddTransformerParameter
   */
  public <TResult> TResult[] load(Class<TResult> clazz, String transformer, Collection<String> ids, LoadConfigurationFactory configure);

  /**
   * Saves all the changes to the Raven server.
   */
  public void saveChanges();

  /**
   * Stores entity in session, extracts Id from entity using Conventions or generates new one if it is not available.
   *
   * Forces concurrency check if the Id is not available during extraction.
   * @param entity Entity to store.
   */
  public void store(Object entity);

  /**
   * Stores the specified dynamic entity, under the specified id.
   * @param entity Entity to store.
   * @param id Id to store this entity under. If other entity exists with the same id it will be overwritten.
   */
  public void store(Object entity, String id);

  /**
   * Stores entity in session, extracts Id from entity using Conventions or generates new one if it is not available and
   * forces concurrency check with given Etag
   * @param entity Entity to store.
   * @param etag
   */
  public void store(Object entity, Etag etag);

  /**
   * Stores the specified entity with the specified etag, under the specified id
   * @param entity Entity to store.
   * @param etag
   * @param id Id to store this entity under. If other entity exists with the same id it will be overwritten.
   */
  public void store(Object entity, Etag etag, String id);

}
