package net.ravendb.client.linq;

import java.util.List;

import com.mysema.query.types.Expression;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.BooleanExpression;
public interface IOrderedQueryable<T> extends Iterable<T> {
  /**
   * Filters a sequence of values based on a predicate.
   * @param predicate
   * @return IRavenQueryable
   */
  public IRavenQueryable<T> where(Predicate predicate);

  /**
   * Projects results
   * @param projectionClass
   * @return projection
   */
  public <TProjection> IRavenQueryable<TProjection> select(Class<TProjection> projectionClass);

  /**
   * Projects results
   * @param projectionClass
   * @return projection
   */
  public <TProjection> IRavenQueryable<TProjection> select(Class<TProjection> projectionClass, String... fields);

  /**
   * Projects results
   * @param projectionClass
   * @return projection
   */
  public <TProjection> IRavenQueryable<TProjection> select(Class<TProjection> projectionClass, String[] fields, String[] projections);

  /**
   * Projects results
   * @param projectionClass
   * @return projection
   */
  public <TProjection> IRavenQueryable<TProjection> select(Class<TProjection> projectionClass, Path<?>... fields);

  /**
   * Projects results
   * @param projectionClass
   * @return projection
   */
  public <TProjection> IRavenQueryable<TProjection> select(Class<TProjection> projectionClass, Path<?>[] fields, Path<?>[] projections);


  /**
   * Projects results based on projection path
   * @param projectionPath
   * @return projection
   */
  public <TProjection> IRavenQueryable<TProjection> select(Path<TProjection> projectionPath);

  /**
   * Changes order of result elements
   * @param asc
   * @return IRavenQueryable
   */
  public IRavenQueryable<T> orderBy(OrderSpecifier<?>... asc);

  /**
   * Materialize query and returns results as list.
   * @return results as list
   */
  public List<T> toList();

  /**
   * Skips specified number of records.
   * Method is used for paging.
   * @param itemsToSkip
   * @return IRavenQueryable
   */
  public IRavenQueryable<T> skip(int itemsToSkip);

  /**
   * Takes specified number of records.
   * Method is used for paging.
   * @param amount
   * @return IRavenQueryable
   */
  public IRavenQueryable<T> take(int amount);

  /**
   * Returns only first entry from result.
   * Throws if zero results was found.
   * @return first result
   */
  public T first();

  /**
   * Returns only first entry from result which suffices specified predicate.
   * Throws if zero results was found.
   * @param predicate
   * @return first result that matches predicate or throws exception
   */
  public T first(BooleanExpression predicate);

  /**
   * Returns first entry from result or default value if none found.
   * @return first result that matches predicate or default type value.
   */
  public T firstOrDefault();

  /**
   * Returns first entry from result which suffices specified predicate or default value if none found.
   * @param predicate
   * @return first result that matches predicate or default type value.
   */
  public T firstOrDefault(BooleanExpression predicate);

  /**
   * Return value is based on result amount:
   * 2 entries and over: throws exception
   * 1 entry - return it
   * 0 - throws
   * @return single result or throws exception when > 1 found
   */
  public T single();

  /**
   * Return value is based on result amount.
   * 2 entries and over: throws exception
   * 1 entry - return it
   * 0 - throws
   * @param predicate
   * @return single result that matches predicate or exception when > 1 found
   */
  public T single(BooleanExpression predicate);

  /**
   * Return value is based on result amount.
   * 2 entries and over: throws exception
   * 1 entry - return it
   * 0 - returns default value
   * @return single result or default
   */
  public T singleOrDefault();

  /**
   * Return value is based on result amount.
   * 2 entries and over: throws exception
   * 1 entry - return it
   * 0 - returns default value
   * @param predicate
   * @return single result match matches predicate or default
   */
  public T singleOrDefault(BooleanExpression predicate);

  /**
   * Performs count query.
   * @return count
   */
  public int count();

  /**
   * Performs any query.
   * Returns true is any entry would be returned in normal query.
   * @return any query
   */
  public boolean any();

  /**
   * Performs count query - each result must match specified predicate.
   * @param predicate
   * @return document count that matches predicate
   */
  public int count(BooleanExpression predicate);

  /**
   * Performs count query.
   * @return document count as long
   */
  public long longCount();

  /**
   * Performs count query - each result must match specified predicate.
   * @param predicate
   * @return document count as long which matches predicate
   */
  public long longCount(BooleanExpression predicate);

  /**
   * Returns element type
   * @return element type
   */
  public Class<?> getElementType();

  /**
   * Expression created via DSL
   * @return DSL expression
   */
  public Expression<?> getExpression();

  /**
   * Query provider.
   * @return query provider.
   */
  public IQueryProvider getProvider();

  /**
   * Project using a different type
   * @param clazz
   * @return projection
   */
  public <TResult> IRavenQueryable<TResult> as(Class<TResult> clazz);

}
