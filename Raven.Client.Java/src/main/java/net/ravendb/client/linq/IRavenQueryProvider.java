package net.ravendb.client.linq;

import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ravendb.abstractions.basic.Lazy;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.data.QueryResult;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.IDocumentQuery;
import net.ravendb.client.document.DocumentQueryCustomizationFactory;

import com.mysema.query.types.Expression;


/**
 * Extension for the built-in IQueryProvider allowing for Raven specific operations
 */
public interface IRavenQueryProvider extends IQueryProvider {
  /**
   * Callback to get the results of the query
   * @param afterQueryExecuted
   */
  void afterQueryExecuted(Action1<QueryResult> afterQueryExecuted);

  /**
   * Callback to get the results of the stream
   * @param afterStreamExecuted
   */
  void afterStreamExecuted(Action1<RavenJObject> afterStreamExecuted);

  /**
   * Customizes the query using the specified action
   * @param factory
   */
  void customize(DocumentQueryCustomizationFactory factory);

  /**
   * The name of the transformer to use with this query
   * @param transformerName
   */
  void transformWith(String transformerName);

  /**
   * @return The name of the index.
   */
  public String getIndexName();

  /**
   * @return The query generator
   */
  public IDocumentQueryGenerator getQueryGenerator();

  /**
   * @return The action to execute on the customize query
   */
  public DocumentQueryCustomizationFactory getCustomizeQuery();

  /**
   * Change the result type for the query provider
   * @param clazz
   * @return self
   */
  public <S> IRavenQueryProvider forClass(Class<S> clazz);

  /**
   * Convert the linq query to a Lucene query
   * @param clazz
   * @param expression
   * @return DocumentQuery
   */
  public <T> IDocumentQuery<T> toDocumentQuery(Class<T> clazz, Expression<?> expression);

  /**
   * Convert the Linq query to a lazy Lucene query and provide a function to execute when it is being evaluate
   * @param expression
   * @param onEval
   * @return lazy list with result
   */
  public <T> Lazy<List<T>> lazily(Class<T> clazz, Expression<?> expression, Action1<List<T>> onEval);

  public <T> Lazy<Integer> countLazily(Class<T> clazz, Expression<?> expression);

  /**
   * @return fields to fetch
   */
  public Set<String> getFieldsToFetch();

  /**
   * @return The result transformer to use
   */
  public String getResultTranformer();

  /**
   * @return The query inputs being supplied to transformer
   */
  public Map<String, RavenJToken> getTransformerParameters();

  /**
   * Adds input to this query via a key/value pair
   * @param input
   * @param foo
   */
  public void addTransformerParameter(String input, RavenJToken foo);
}
