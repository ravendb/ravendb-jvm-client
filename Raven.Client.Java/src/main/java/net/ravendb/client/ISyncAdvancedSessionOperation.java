package net.ravendb.client;

import net.ravendb.abstractions.basic.CloseableIterator;
import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.FacetQuery;
import net.ravendb.abstractions.data.FacetResults;
import net.ravendb.abstractions.data.MoreLikeThisQuery;
import net.ravendb.abstractions.data.QueryHeaderInformation;
import net.ravendb.abstractions.data.StreamResult;
import net.ravendb.client.document.batches.IEagerSessionOperations;
import net.ravendb.client.document.batches.ILazySessionOperations;
import net.ravendb.client.indexes.AbstractIndexCreationTask;
import net.ravendb.client.indexes.AbstractTransformerCreationTask;
import net.ravendb.client.linq.IRavenQueryable;


/**
 * Advanced synchronous session operations
 */
public interface ISyncAdvancedSessionOperation extends IAdvancedDocumentSessionOperations {


  /**
   * Updates entity with latest changes from server
   * @param entity Instance of an entity that will be refreshed
   */
  public <T> void refresh(T entity);

  /**
   * Loads multiple entities that contain common prefix.
   * @param clazz Defines type of object
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   */
  public <T> T[] loadStartingWith(Class<T> clazz, String keyPrefix);

  /**
   * Loads multiple entities that contain common prefix.
   * @param clazz Defines type of object
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   */
  public <T> T[] loadStartingWith(Class<T> clazz, String keyPrefix, String matches);

  /**
   * Loads multiple entities that contain common prefix.
   * @param clazz Defines type of object
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped. By default: 0.
   */
  public <T> T[] loadStartingWith(Class<T> clazz, String keyPrefix, String matches, int start);

  /**
   * Loads multiple entities that contain common prefix.
   * @param clazz Defines type of object
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped. By default: 0.
   * @param pageSize Maximum number of documents that will be retrieved. By default: 25.
   */
  public <T> T[] loadStartingWith(Class<T> clazz, String keyPrefix, String matches, int start, int pageSize);

  /**
   * Loads multiple entities that contain common prefix.
   * @param clazz Defines type of object
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped. By default: 0.
   * @param pageSize Maximum number of documents that will be retrieved. By default: 25.
   * @param exclude Pipe ('|') separated values for which document keys (after 'keyPrefix') should not be matched ('?' any single character, '*' any characters)
   */
  public <T> T[] loadStartingWith(Class<T> clazz, String keyPrefix, String matches, int start, int pageSize, String exclude);

  /**
   * Loads multiple entities that contain common prefix.
   * @param clazz Defines type of object
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped. By default: 0.
   * @param pageSize Maximum number of documents that will be retrieved. By default: 25.
   * @param exclude Pipe ('|') separated values for which document keys (after 'keyPrefix') should not be matched ('?' any single character, '*' any characters)
   * @param pagingInformation Used to perform rapid pagination on a server side
   */
  public <T> T[] loadStartingWith(Class<T> clazz, String keyPrefix, String matches, int start, int pageSize, String exclude, RavenPagingInformation pagingInformation);

  /**
   * Loads multiple entities that contain common prefix.
   * @param clazz Defines type of object
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped. By default: 0.
   * @param pageSize Maximum number of documents that will be retrieved. By default: 25.
   * @param exclude Pipe ('|') separated values for which document keys (after 'keyPrefix') should not be matched ('?' any single character, '*' any characters)
   * @param pagingInformation Used to perform rapid pagination on a server side
   * @param skipAfter skip document fetching until given key is found and return documents after that key (default: null)
   */
  public <T> T[] loadStartingWith(Class<T> clazz, String keyPrefix, String matches, int start, int pageSize, String exclude, RavenPagingInformation pagingInformation, String skipAfter);

  /**
   * Loads documents with the specified key prefix and applies the specified results transformer against the results
   * @param clazz Defines type of object
   * @param transformerClass
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   */
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix);

  /**
   * Loads documents with the specified key prefix and applies the specified results transformer against the results
   * @param clazz Defines type of object
   * @param transformerClass
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   */
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix, String matches);

  /**
   * Loads documents with the specified key prefix and applies the specified results transformer against the results
   * @param clazz Defines type of object
   * @param transformerClass
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped. By default: 0.
   */
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix, String matches, int start);

  /**
   * Loads documents with the specified key prefix and applies the specified results transformer against the results
   * @param clazz Defines type of object
   * @param transformerClass
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped. By default: 0.
   * @param pageSize Maximum number of documents that will be retrieved. By default: 25.
   */
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix, String matches, int start, int pageSize);


  /**
   * Loads documents with the specified key prefix and applies the specified results transformer against the results
   * @param clazz Defines type of object
   * @param transformerClass
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped. By default: 0.
   * @param pageSize Maximum number of documents that will be retrieved. By default: 25.
   * @param exclude Pipe ('|') separated values for which document keys (after 'keyPrefix') should not be matched ('?' any single character, '*' any characters)
   */
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix, String matches, int start, int pageSize, String exclude);

  /**
   * Loads documents with the specified key prefix and applies the specified results transformer against the results
   * @param clazz Defines type of object
   * @param transformerClass
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped. By default: 0.
   * @param pageSize Maximum number of documents that will be retrieved. By default: 25.
   * @param exclude Pipe ('|') separated values for which document keys (after 'keyPrefix') should not be matched ('?' any single character, '*' any characters)
   * @param pagingInformation Used to perform rapid pagination on a server side
   */
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix, String matches, int start, int pageSize, String exclude,
    RavenPagingInformation pagingInformation);

  /**
   * Loads documents with the specified key prefix and applies the specified results transformer against the results
   * @param clazz Defines type of object
   * @param transformerClass
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped. By default: 0.
   * @param pageSize Maximum number of documents that will be retrieved. By default: 25.
   * @param exclude Pipe ('|') separated values for which document keys (after 'keyPrefix') should not be matched ('?' any single character, '*' any characters)
   * @param pagingInformation Used to perform rapid pagination on a server side
   * @param configure Additional configuration options for operation e.g. AddTransformerParameter
   */
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix, String matches, int start, int pageSize, String exclude,
    RavenPagingInformation pagingInformation, LoadConfigurationFactory configure);

  /**
   * Loads documents with the specified key prefix and applies the specified results transformer against the results
   * @param clazz Defines type of object
   * @param transformerClass
   * @param keyPrefix Prefix for which documents should be returned e.g. "products/"
   * @param matches Pipe ('|') separated values for which document keys (after 'keyPrefix') should be matched ('?' any single character, '*' any characters)
   * @param start Number of documents that should be skipped. By default: 0.
   * @param pageSize Maximum number of documents that will be retrieved. By default: 25.
   * @param exclude Pipe ('|') separated values for which document keys (after 'keyPrefix') should not be matched ('?' any single character, '*' any characters)
   * @param pagingInformation Used to perform rapid pagination on a server side
   * @param configure Additional configuration options for operation e.g. AddTransformerParameter
   * @param skipAfter Skip document fetching until given key is found and return documents after that key (default: null)
   */
  public <TResult, TTransformer extends AbstractTransformerCreationTask> TResult[] loadStartingWith(Class<TResult> clazz, Class<TTransformer> transformerClass,
    String keyPrefix, String matches, int start, int pageSize, String exclude,
    RavenPagingInformation pagingInformation, LoadConfigurationFactory configure, String skipAfter);


  /**
   * Access the lazy operations
   */
  public ILazySessionOperations lazily();

  /**
   * Access the eager operations
   */
  public IEagerSessionOperations eagerly();

  /**
   * Queries the index specified by <T> using lucene syntax.
   * @param clazz The result class of the query.
   * @param indexClass The type of the index creator.
   */
  public <T, S extends AbstractIndexCreationTask> IDocumentQuery<T> documentQuery(Class<T> clazz, Class<S> indexClass);

  /**
   * Query the specified index using Lucene syntax
   * @param indexName Name of the index.
   * @param isMapReduce Control how we treat identifier properties in map/reduce indexes
   */
  public <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName, boolean isMapReduce);

  /**
   * Query the specified index using Lucene syntax
   * @param clazz
   * @param indexName Name of the index.
   */
  public <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName);

  /**
   * Dynamically query RavenDB using Lucene syntax
   * @param clazz
   */
  public <T> IDocumentQuery<T> documentQuery(Class<T> clazz);

  /**
   * Gets the document URL for the specified entity.
   * @param entity Instance of an entity for which url will be returned
   */
  public String getDocumentUrl(Object entity);

  /**
   * Stream the results on the query to the client, converting them to
   * Java types along the way.
   * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
   * @param query
   */
  public <T> CloseableIterator<StreamResult<T>> stream(IRavenQueryable<T> query);

  /**
   * Stream the results on the query to the client, converting them to
   * Java types along the way.
   * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
   * @param query
   * @param queryHeaderInformation
   */
  public <T> CloseableIterator<StreamResult<T>> stream(IRavenQueryable<T> query, Reference<QueryHeaderInformation> queryHeaderInformation);


  /**
   * Stream the results on the query to the client, converting them to
   * Java types along the way.
   * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
   * @param query
   */
  public <T> CloseableIterator<StreamResult<T>> stream(IDocumentQuery<T> query);

  /**
   * Stream the results on the query to the client, converting them to
   * Java types along the way.
   * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
   * @param query
   */
  public <T> CloseableIterator<StreamResult<T>> stream(IDocumentQuery<T> query, Reference<QueryHeaderInformation> queryHeaderInformation);

  /**
   * Stream the results on the query to the client, converting them to
   * Java types along the way.
   * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
   * @param entityClass
   */
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass);


  /**
   * Stream the results on the query to the client, converting them to
   * Java types along the way.
   * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
   * @param fromEtag
   */
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag);

  /**
   * Stream the results on the query to the client, converting them to
   * Java types along the way.
   * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
   * @param fromEtag
   * @param startsWith
   */
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith);

  /**
   * Stream the results on the query to the client, converting them to
   * Java types along the way.
   * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
   * @param fromEtag
   * @param startsWith
   * @param matches
   */
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith, String matches);

  /**
   * Stream the results on the query to the client, converting them to
   * Java types along the way.
   * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
   * @param fromEtag
   * @param startsWith
   * @param matches
   * @param start
   */
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith, String matches, int start);

  /**
   * Stream the results on the query to the client, converting them to
   * Java types along the way.
   * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
   * @param fromEtag
   * @param startsWith
   * @param matches
   * @param start
   * @param pageSize
   */
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith, String matches, int start, int pageSize);

  /**
   * Stream the results on the query to the client, converting them to
   * Java types along the way.
   * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
   * @param fromEtag
   * @param startsWith
   * @param matches
   * @param start
   * @param pageSize
   */
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith, String matches, int start, int pageSize, RavenPagingInformation pagingInformation);

  /**
   * Stream the results on the query to the client, converting them to
   * Java types along the way.
   * Does NOT track the entities in the session, and will not includes changes there when saveChanges() is called
   * @param fromEtag
   * @param startsWith
   * @param matches
   * @param start
   * @param pageSize
   */
  public <T> CloseableIterator<StreamResult<T>> stream(Class<T> entityClass, Etag fromEtag, String startsWith, String matches, int start, int pageSize, RavenPagingInformation pagingInformation, String skipAfter);

  /**
   * @param queries
   */
  public FacetResults[] multiFacetedSearch(FacetQuery... queries);

  public <T> T[] moreLikeThis(Class<T> entityClass, Class<? extends AbstractIndexCreationTask> indexCreator, String documentId);

  public <T> T[] moreLikeThis(Class<T> entityClass, Class<? extends AbstractIndexCreationTask> indexCreator, MoreLikeThisQuery parameters);

  public <T> T[] moreLikeThis(Class<T> entityClass, String index, String documentId);

  public <T> T[] moreLikeThis(Class<T> entityClass, Class<? extends AbstractIndexCreationTask> indexCreator, Class<? extends AbstractTransformerCreationTask> transformerClass, String documentId);

  public <T> T[] moreLikeThis(Class<T> entityClass, Class<? extends AbstractIndexCreationTask> indexCreator, Class<? extends AbstractTransformerCreationTask> transformerClass, MoreLikeThisQuery parameters);

  public <T> T[] moreLikeThis(Class<T> entityClass, String index, String transformer, String documentId);

  public <T> T[] moreLikeThis(Class<T> entityClass, String index, String transformer, MoreLikeThisQuery parameters);
}
