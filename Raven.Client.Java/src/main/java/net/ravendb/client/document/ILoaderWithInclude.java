package net.ravendb.client.document;

import com.mysema.query.types.Expression;

import java.util.Collection;
import java.util.UUID;

/**
 * Fluent interface for specifying include paths
 * for loading documents
 *
 */
public interface ILoaderWithInclude {

  /**
   * Includes the specified path.
   * @param path
   */
  public ILoaderWithInclude include(String path);

  /**
   * Includes the specified path.
   * @param path
   */
  public ILoaderWithInclude include(Expression<?> path);

  /**
   * Includes the specified path.
   * @param path
   */
  public ILoaderWithInclude include(Class<?> targetClass, Expression<?> path);

  /**
   * Loads the specified ids.
   * @param clazz
   * @param ids
   */
  public <TResult> TResult[] load(Class<TResult> clazz, String... ids);

  /**
   * Loads the specified ids.
   * @param clazz
   * @param ids
   */
  public <TResult> TResult[] load(Class<TResult> clazz, Collection<String> ids);

  /**
   * Loads the specified id.
   * @param clazz
   * @param id
   */
  public <TResult> TResult load(Class<TResult> clazz, String id);

  /**
   * Loads the specified entity with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   * load(Post.class, 1)
   * And that call will internally be translated to
   * load(Post.class, "posts/1");
   *
   * Or whatever your conventions specify.
   * @param clazz
   * @param id
   */
  public <TResult> TResult load(Class<TResult> clazz, Number id);

  /**
   * Loads the specified entity with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   * load(Post.class, 1)
   * And that call will internally be translated to
   * load(Post.class, "posts/1");
   *
   * Or whatever your conventions specify.
   * @param clazz
   * @param id
   */
  public <TResult> TResult load(Class<TResult> clazz, UUID id);

  /**
   * Loads the specified entity with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   * load(Post.class, 1, 2, 3)
   * And that call will internally be translated to
   * load(Post.class, "posts/1", "posts/2", "posts/3");
   *
   * Or whatever your conventions specify.
   * @param clazz
   * @param ids
   */
  public <TResult> TResult[] load(Class<TResult> clazz, UUID... ids);

  /**
   * Loads the specified entity with the specified id after applying
   * conventions on the provided id to get the real document id.
   *
   * This method allows you to call:
   * load(Post.class, 1, 2, 3)
   * And that call will internally be translated to
   * load(Post.class, "posts/1", "posts/2", "posts/3");
   *
   * Or whatever your conventions specify.
   * @param clazz
   * @param ids
   */
  public <TResult> TResult[] load(Class<TResult> clazz, Number... ids);

}
