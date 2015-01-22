package net.ravendb.client.document.batches;

import java.util.Collection;
import java.util.UUID;

import net.ravendb.abstractions.basic.Lazy;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.data.MoreLikeThisQuery;
import net.ravendb.client.LoadConfigurationFactory;
import net.ravendb.client.RavenPagingInformation;
import net.ravendb.client.indexes.AbstractTransformerCreationTask;

import com.mysema.query.types.Path;

/**
 * Specify interface for lazy operation for the session
 */
public interface ILazySessionOperations {

  /**
   * Begin a load while including the specified path
   * @param path Path in documents in which server should look for a 'referenced' documents.
   */
  public ILazyLoaderWithInclude include(String path);

  /**
   * Begin a load while including the specified path
   * @param path Path in documents in which server should look for a 'referenced' documents.
   */
  public ILazyLoaderWithInclude include(Path<?> path);

  /**
   * Loads the specified entities with the specified ids.
   * @param ids Array of Ids that should be loaded
   */
  public <TResult> Lazy<TResult[]> load(Class<TResult> clazz, String[] ids);

  /**
   * Loads the specified entities with the specified ids and a function to call when it is evaluated
   * @param clazz Defines type of object
   * @param ids Array of Ids that should be loaded
   * @param onEval
   */
  public <TResult> Lazy<TResult[]> load(Class<TResult> clazz, String[] ids, Action1<TResult[]> onEval);

  /**
   * Loads the specified entities with the specified ids.
   * @param ids Collection of Ids that should be loaded
   */
  public <TResult> Lazy<TResult[]> load(Class<TResult> clazz, Collection<String> ids);

  /**
   * Loads the specified entities with the specified ids and a function to call when it is evaluated
   * @param clazz Defines type of object
   * @param ids Collection of Ids that should be loaded
   * @param onEval
   */
  public <TResult> Lazy<TResult[]> load(Class<TResult> clazz, Collection<String> ids, Action1<TResult[]> onEval);

  /**
   * Loads the specified id.
   * @param clazz Defines type of object
   * @param id Identifier of a entity that will be loaded.
   */
  public <TResult> Lazy<TResult> load(Class<TResult> clazz, String id);

  /**
   * Loads the specified id and a function to call when it is evaluated
   * @param clazz Defines type of object
   * @param id Identifier of a entity that will be loaded.
   * @param onEval
   */
  public <TResult> Lazy<TResult> load(Class<TResult> clazz, String id, Action1<TResult> onEval);

  /**
   * Loads the specified entity with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   * lazyLoad(Post.class, 1)
   * And that call will internally be translated to
   * lazyLoad(Post.class, "posts/1")
   * @param clazz Defines type of object
   * @param id
   */
  public <TResult> Lazy<TResult> load(Class<TResult> clazz, Number id);

  /**
   * Loads the specified entity with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   * lazyLoad(Post.class, 1)
   * And that call will internally be translated to
   * lazyLoad(Post.class, "posts/1")
   * @param clazz Defines type of object
   * @param id
   */
  public <TResult> Lazy<TResult> load(Class<TResult> clazz, UUID id);

  /**
   * Loads the specified entity with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   *
   * lazyLoad(Post.class, 1)
   * And that call will internally be translated to
   * lazyLoad(Post.class, "posts/1")
   *
   * Or whatever your conventions specify.
   * @param id
   * @param onEval
   */
  public <TResult> Lazy<TResult> load(Class<TResult> clazz, Number id, Action1<TResult> onEval);

  /**
   * Loads the specified entity with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   *
   * lazyLoad(Post.class, 1)
   * And that call will internally be translated to
   * lazyLoad(Post.class, "posts/1")
   *
   * Or whatever your conventions specify.
   * @param id
   * @param onEval
   */
  public <TResult> Lazy<TResult> load(Class<TResult> clazz, UUID id, Action1<TResult> onEval);

  /**
   * Loads the specified entities with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   * lazyLoad(Post.class, 1,2,3);
   * And that call will internally be translated to
   * lazyLoad(Post.class, "posts/1", "posts/2", "posts/3")
   *
   * Or whatever your conventions specify.
   */
  public <TResult> Lazy<TResult[]> load(Class<TResult> clazz, Number... ids);

  /**
   * Loads the specified entities with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   * lazyLoad(Post.class, 1,2,3);
   * And that call will internally be translated to
   * lazyLoad(Post.class, "posts/1", "posts/2", "posts/3")
   *
   * Or whatever your conventions specify.
   */
  public <TResult> Lazy<TResult[]> load(Class<TResult> clazz, UUID... ids);

  /**
   * Loads the specified entities with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   * lazyLoad(Post.class, 1,2,3);
   * And that call will internally be translated to
   * lazyLoad(Post.class, "posts/1", "posts/2", "posts/3")
   *
   * Or whatever your conventions specify.
   */
  public <TResult> Lazy<TResult[]> load(Class<TResult> clazz, Action1<TResult[]> onEval, Number... ids);

  /**
   * Loads the specified entities with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   * lazyLoad(Post.class, 1,2,3);
   * And that call will internally be translated to
   * lazyLoad(Post.class, "posts/1", "posts/2", "posts/3")
   *
   * Or whatever your conventions specify.
   */
  public <TResult> Lazy<TResult[]> load(Class<TResult> clazz, Action1<TResult[]> onEval, UUID... ids);

  /**
   * Load documents with the specified key prefix
   * @param clazz
   * @param keyPrefix
   */
  public <TResult> Lazy<TResult[]> loadStartingWith(Class<TResult> clazz, String keyPrefix);

  /**
   * Load documents with the specified key prefix
   * @param clazz
   * @param keyPrefix
   * @param matches
   */
  public <TResult> Lazy<TResult[]> loadStartingWith(Class<TResult> clazz, String keyPrefix, String matches);

  /**
   * Load documents with the specified key prefix
   * @param clazz
   * @param keyPrefix
   * @param matches
   * @param start
   */
  public <TResult> Lazy<TResult[]> loadStartingWith(Class<TResult> clazz, String keyPrefix, String matches, int start);

  /**
   * Load documents with the specified key prefix
   * @param clazz
   * @param keyPrefix
   * @param matches
   * @param start
   * @param pageSize
   */
  public <TResult> Lazy<TResult[]> loadStartingWith(Class<TResult> clazz, String keyPrefix, String matches, int start, int pageSize);

  /**
   * Load documents with the specified key prefix
   * @param clazz Defines type of object
   * @param keyPrefix
   * @param matches
   * @param start
   * @param pageSize
   */
  public <TResult> Lazy<TResult[]> loadStartingWith(Class<TResult> clazz, String keyPrefix, String matches, int start, int pageSize, String exclude);

  /**
   * Loads multiple entities that contain common prefix.
   * @param clazz Defines type of object
   * @param keyPrefix Loads multiple entities that contain common prefix.
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped. By default: 0.
   * @param pageSize Maximum number of documents that will be retrieved. By default: 25.
   */
  public <TResult> Lazy<TResult[]> loadStartingWith(Class<TResult> clazz, String keyPrefix, String matches, int start, int pageSize, String exclude, RavenPagingInformation pagingInformation);

  /**
   * Loads multiple entities that contain common prefix.
   * @param clazz Defines type of object
   * @param keyPrefix Loads multiple entities that contain common prefix.
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped. By default: 0.
   * @param pageSize Maximum number of documents that will be retrieved. By default: 25.
   * @param exclude Pipe ('|') separated values for which document keys (after 'keyPrefix') should not be matched ('?' any single character, '*' any characters)
   * @param pagingInformation Used to perform rapid pagination on a server side
   * @param skipAfter Skip document fetching until given key is found and return documents after that key (default: null)
   */
  public <TResult> Lazy<TResult[]> loadStartingWith(Class<TResult> clazz, String keyPrefix, String matches, int start, int pageSize, String exclude, RavenPagingInformation pagingInformation, String skipAfter);

  public <TResult> Lazy<TResult[]> moreLikeThis(Class<TResult> clazz, MoreLikeThisQuery query);

  /**
   * Performs a load that will use the specified results transformer against the specified id
   * @param tranformerClass The transformer to use in this load operation
   * @param clazz
   * @param id Id of a entity to load
   * @param configure Additional configuration options for operation e.g. AddTransformerParameter
   */
  public <TResult, TTransformer extends AbstractTransformerCreationTask> Lazy<TResult> load(Class<TTransformer> tranformerClass,
    Class<TResult> clazz, String id, LoadConfigurationFactory configure);

  /**
   * Performs a load that will use the specified results transformer against the specified id
   * @param tranformerClass The transformer to use in this load operation
   * @param clazz
   * @param ids Array of ids of documents to load
   * @param configure Additional configuration options for operation e.g. AddTransformerParameter
   */
  public <TResult, TTransformer extends AbstractTransformerCreationTask> Lazy<TResult[]> load(Class<TTransformer> tranformerClass,
    Class<TResult> clazz, String[] ids, LoadConfigurationFactory configure);


}
