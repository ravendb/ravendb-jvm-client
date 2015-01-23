package net.ravendb.client;

import java.util.Collection;
import java.util.Date;

import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.IndexQuery;
import net.ravendb.abstractions.data.QueryOperator;
import net.ravendb.abstractions.data.QueryResult;
import net.ravendb.abstractions.indexing.SpatialOptions.SpatialRelation;
import net.ravendb.abstractions.indexing.SpatialOptions.SpatialUnits;
import net.ravendb.client.document.DocumentConvention;

import com.mysema.query.types.Expression;
import com.mysema.query.types.Path;
import com.mysema.query.types.path.ListPath;


/**
 * A query against a Raven index
 */
public interface IDocumentQueryBase<T, TSelf extends IDocumentQueryBase<T, TSelf>> {

  /**
   * Gets the document convention from the query session
   */
  public DocumentConvention getDocumentConvention();

  /**
   *  Negate the next operation
   */
  public TSelf not();

  /**
   * Negate the next operation
   */
  public void negateNext();

  /**
   * Includes the specified path in the query, loading the document specified in that path
   */
  public TSelf include(String path);

  /**
   * Includes the specified path in the query, loading the document specified in that path
   * @param path The path.
   */
  public TSelf include(Path<?> path);

  /**
   * Takes the specified count.
   * @param count Maximum number of items to take.
   */
  public TSelf take (int count);

  /**
   * Skips the specified count.
   * @param count Number of items to skip.
   */
  public TSelf skip(int count);

  /**
   * Returns first element or default value for type if sequence is empty.
   */
  public T firstOrDefault();

  /**
   * Returns first element or throws if sequence is empty.
   */
  public T first();

  /**
   * Returns first element or default value for given type if sequence is empty. Throws if sequence contains more than one element.
   */
  public T singleOrDefault();

  /**
   * Returns first element or throws if sequence is empty or contains more than one element.
   */
  public T single();

  /**
   * Filter the results from the index using the specified where clause.
   * @param whereClause Lucene-syntax based query predicate.
   */
  public TSelf where(String whereClause);

  /**
   * Matches exact value
   *
   * Defaults to NotAnalyzed
   * @param fieldName
   * @param value
   */
  public TSelf whereEquals(String fieldName, Object value);

  /**
   * Matches exact value
   *
   * Defaults to NotAnalyzed
   * @param propertySelector
   * @param value
   */
  public <TValue> TSelf whereEquals(Expression<? super TValue> propertySelector, TValue value);

  /**
   * Matches exact value
   *
   * Default to allow wildcards only if analyzed
   * @param fieldName
   * @param value
   * @param isAnalyzed
   */
  public TSelf whereEquals(String fieldName, Object value, boolean isAnalyzed);

  /**
   * Matches exact value
   *
   * Defaults to allow wildcards only if analyzed
   * @param propertySelector
   * @param value
   * @param isAnalyzed
   */
  public <TValue> TSelf whereEquals(Expression<? super TValue> propertySelector, TValue value, boolean isAnalyzed);

  /**
   * Matches exact value
   * @param whereParams
   */
  public TSelf whereEquals (WhereParams whereParams);

  /**
   * Check that the field has one of the specified values
   * @param fieldName
   * @param values
   */
  public TSelf whereIn(String fieldName, Collection<?> values);

  /**
   * Check that the field has one of the specified values
   * @param propertySelector
   * @param values
   */
  public <TValue> TSelf whereIn(Expression<? super TValue> propertySelector, Collection<TValue> values);

  /**
   * Matches fields which starts with the specified value.
   * @param fieldName Name of the field.
   * @param value The value.
   */
  public TSelf whereStartsWith(String fieldName, Object value);

  /**
   * Matches fields which starts with the specified value.
   * @param propertySelector Property selector for the field.
   * @param value The value.
   */
  public <TValue> TSelf whereStartsWith(Expression<? super TValue> propertySelector, TValue value);

  /**
   * Matches fields which ends with the specified value.
   * @param fieldName Name of the field.
   * @param value The value.
   */
  public TSelf whereEndsWith (String fieldName, Object value);

  /**
   * Matches fields which ends with the specified value.
   * @param propertySelector Property selector for the field.
   * @param value The value.
   */
  public <TValue> TSelf whereEndsWith(Expression<? super TValue> propertySelector, TValue value);

  /**
   * Matches fields where the value is between the specified start and end, exclusive
   * @param fieldName Name of the field.
   * @param start The start.
   * @param end The end.
   */
  public TSelf whereBetween (String fieldName, Object start, Object end);

  /**
   * Matches fields where the value is between the specified start and end, exclusive
   * @param propertySelector Property selector for the field.
   * @param start The start.
   * @param end The end.
   */
  public <TValue> TSelf whereBetween(Expression<? super TValue> propertySelector, TValue start, TValue end);

  /**
   * Matches fields where the value is between the specified start and end, inclusive
   * @param fieldName Name of the field.
   * @param start The start.
   * @param end The end.
   */
  public TSelf whereBetweenOrEqual (String fieldName, Object start, Object end);

  /**
   * Matches fields where the value is between the specified start and end, inclusive
   * @param propertySelector Property selector for the field.
   * @param start The start.
   * @param end The end.
   */
  public <TValue> TSelf whereBetweenOrEqual(Expression<? super TValue> propertySelector, TValue start, TValue end);

  /**
   * Matches fields where the value is greater than the specified value
   * @param fieldName Name of the field.
   * @param value The value.
   */
  public TSelf whereGreaterThan (String fieldName, Object value);

  /**
   * Matches fields where the value is greater than the specified value
   * @param propertySelector Property selector for the field.
   * @param value The value.
   */
  public <TValue> TSelf whereGreaterThan(Expression<? super TValue> propertySelector, TValue value);

  /**
   * Matches fields where the value is greater than or equal to the specified value
   * @param fieldName Name of the field.
   * @param value The value.
   */
  public TSelf whereGreaterThanOrEqual (String fieldName, Object value);

  /**
   * Matches fields where the value is greater than or equal to the specified value
   * @param propertySelector Property selector for the field.
   * @param value The value.
   */
  public <TValue> TSelf whereGreaterThanOrEqual(Expression<? super TValue> propertySelector, TValue value);

  /**
   * Matches fields where the value is less than the specified value
   * @param fieldName Name of the field.
   * @param value The value.
   */
  public TSelf whereLessThan (String fieldName, Object value);

  /**
   * Matches fields where the value is less than the specified value
   * @param propertySelector Property selector for the field.
   * @param value The value.
   */
  public <TValue> TSelf whereLessThan(Expression<? super TValue> propertySelector, TValue value);

  /**
   * Matches fields where the value is less than or equal to the specified value
   * @param fieldName Name of the field.
   * @param value The value.
   */
  public TSelf whereLessThanOrEqual (String fieldName, Object value);

  /**
   * Matches fields where the value is less than or equal to the specified value
   * @param propertySelector Property selector for the field.
   * @param value The value.
   */
  public <TValue> TSelf whereLessThanOrEqual(Expression<? super TValue> propertySelector, TValue value);

  /**
   * Add an AND to the query
   */
  public TSelf andAlso();

  /**
   * Add an OR to the query
   */
  public TSelf orElse();

  /**
   * Specifies a boost weight to the last where clause.
   * The higher the boost factor, the more relevant the term will be.
   *
   * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Boosting%20a%20Term
   * @param boost
   * boosting factor where 1.0 is default, less than 1.0 is lower weight, greater than 1.0 is higher weight
   */
  public TSelf boost(Double boost);

  /**
   * Specifies a fuzziness factor to the single word term in the last where clause
   *
   * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Fuzzy%20Searches
   * @param fuzzy 0.0 to 1.0 where 1.0 means closer match
   */
  public TSelf fuzzy (Double fuzzy);

  /**
   * Specifies a proximity distance for the phrase in the last where clause
   *
   * http://lucene.apache.org/java/2_4_0/queryparsersyntax.html#Proximity%20Searches
   * @param proximity number of words within
   */
  public TSelf proximity (int proximity);

  /**
   * Filter matches to be inside the specified radius
   * @param radius The radius.
   * @param latitude The latitude.
   * @param longitude The longitude.
   */
  public TSelf withinRadiusOf(double radius, double latitude, double longitude);

  /**
   * Filter matches to be inside the specified radius
   * @param radius The radius.
   * @param latitude The latitude.
   * @param longitude The longitude.
   * @param radiusUnits The unit of the radius.
   */
  public TSelf withinRadiusOf(double radius, double latitude, double longitude, SpatialUnits radiusUnits);

  /**
   * Filter matches to be inside the specified radius
   * @param fieldName The field name for the radius.
   * @param radius The radius.
   * @param latitude The latitude.
   * @param longitude The longitude.
   */
  public TSelf withinRadiusOf(String fieldName, double radius, double latitude, double longitude);

  /**
   * Filter matches to be inside the specified radius
   * @param fieldName The field name for the radius.
   * @param radius The radius.
   * @param latitude The latitude.
   * @param longitude The longitude.
   * @param radiusUnits The unit of the radius.
   */
  public TSelf withinRadiusOf(String fieldName, double radius, double latitude, double longitude, SpatialUnits radiusUnits);

  /**
   * Filter matches based on a given shape - only documents with the shape defined in fieldName that
   * have a relation rel with the given shapeWKT will be returned
   * @param fieldName The name of the field containg the shape to use for filtering.
   * @param shapeWKT The query shape.
   * @param rel Spatial relation to check
   */
  public TSelf relatesToShape(String fieldName, String shapeWKT, SpatialRelation rel);

  /**
   * Filter matches based on a given shape - only documents with the shape defined in fieldName that
   * have a relation rel with the given shapeWKT will be returned
   * @param fieldName The name of the field containg the shape to use for filtering.
   * @param shapeWKT The query shape.
   * @param rel Spatial relation to check
   * @param distanceErrorPct The allowed error percentage.
   */
  public TSelf relatesToShape(String fieldName, String shapeWKT, SpatialRelation rel, double distanceErrorPct);

  /**
   * Sorts the query results by distance.
   */
  public TSelf sortByDistance();

  /**
   * Sorts the query results by distance.
   */
  public TSelf sortByDistance(double lat, double lng);

  /**
   * Sorts the query results by distance.
   */
  public TSelf sortByDistance(double lat, double lng, String sortedFieldName);

  /**
   * Order the results by the specified fields
   * The fields are the names of the fields to sort, defaulting to sorting by ascending.
   * You can prefix a field name with '-' to indicate sorting by descending or '+' to sort by ascending
   * @param fields The fields.
   */
  public TSelf orderBy(String... fields);

  /**
   * Order the results by the specified fields
   * The fields are the names of the fields to sort, defaulting to sorting by ascending.
   * You can prefix a field name with '-' to indicate sorting by descending or '+' to sort by ascending
   * @param propertySelectors Property selector for the fields.
   */
  public <TValue> TSelf orderBy(Expression<?>... propertySelectors);

  /**
   * Order the results by the specified fields
   * The fields are the names of the fields to sort, defaulting to sorting by descending.
   * You can prefix a field name with '-' to indicate sorting by descending or '+' to sort by ascending
   * @param fields The fields
   */
  public TSelf orderByDescending(String... fields);

  /**
   * Order the results by the specified fields
   * The fields are the names of the fields to sort, defaulting to sorting by descending.
   * You can prefix a field name with '-' to indicate sorting by descending or '+' to sort by ascending
   * @param propertySelectors Property selectors for the fields.
   */
  public <TValue> TSelf orderByDescending(Expression<?>... propertySelectors);

  /**
   * Adds matches highlighting for the specified field.
   *
   * The specified field should be analysed and stored for highlighter to work.
   * For each match it creates a fragment that contains matched text surrounded by highlighter tags.
   * @param fieldName The field name to highlight.
   * @param fragmentLength The fragment length.
   * @param fragmentCount The maximum number of fragments for the field.
   * @param fragmentsField The field in query results item to put highlighing into.
   */
  public TSelf highlight(String fieldName, int fragmentLength, int fragmentCount, String fragmentsField);

  /**
   * Adds matches highlighting for the specified field.
   *
   * The specified field should be analysed and stored for highlighter to work.
   * For each match it creates a fragment that contains matched text surrounded by highlighter tags.
   * @param fieldName The field name to highlight.
   * @param fragmentLength The fragment length.
   * @param fragmentCount The fragment count.
   * @param highlightings The maximum number of fragments for the field.
   */
  public TSelf highlight(String fieldName, int fragmentLength, int fragmentCount, Reference<FieldHighlightings> highlightings);

  /**
   * Adds matches highlighting for the specified field.
   *
   * The specified field should be analysed and stored for highlighter to work.
   * For each match it creates a fragment that contains matched text surrounded by highlighter tags.
   * @param propertySelector The property to highlight.
   * @param fragmentLength The fragment length.
   * @param fragmentCount The maximum number of fragments for the field.
   * @param fragmentsPropertySelector The property to put highlightings into.
   */
  public <TValue> TSelf highlight(Expression<?> propertySelector, int fragmentLength, int fragmentCount, ListPath<?, ?> fragmentsPropertySelector);

  /**
   * Adds matches highlighting for the specified field.
   *
   * The specified field should be analysed and stored for highlighter to work.
   * For each match it creates a fragment that contains matched text surrounded by highlighter tags.
   * @param propertySelector The property to highlight.
   * @param fragmentLength The fragment length.
   * @param fragmentCount The maximum number of fragment for the field.
   * @param highlightings Field highlightings for all results.
   */
  public <TValue> TSelf highlight(Expression<?> propertySelector, int fragmentLength, int fragmentCount, Reference<FieldHighlightings> highlightings);

  /**
   * Adds matches highlighting for the specified field on a Map/Reduce Index.
   *
   * This is only valid for Map/Reduce Index querys.
   * The specified fields should be analysed and stored for highlighter to work.
   * For each match it creates a fragment that contains matched text surrounded by highlighter tags.
   * @param propertySelector The property to highlight.
   * @param keyPropertySelector The key property to associate highlights with.
   * @param fragmentLength The fragment length.
   * @param fragmentCount The maximum number of fragment for the field.
   * @param highlightings Field highlightings for all results.
   */
  public <TValue> TSelf highlight(Expression<?> propertySelector, Expression<?> keyPropertySelector, int fragmentLength, int fragmentCount, Reference<FieldHighlightings> highlightings);

  /**
   * Sets the tags to highlight matches with.
   * @param preTag Prefix tag.
   * @param postTag Postfix tag.
   */
  public TSelf setHighlighterTags(String preTag, String postTag);

  /**
   * Sets the tags to highlight matches with.
   * @param preTags Prefix tags.
   * @param postTags Postfix tags.
   */
  public TSelf setHighlighterTags(String[] preTags, String[] postTags);

  /**
   * Instructs the query to wait for non stale results as of now.
   */
  public TSelf waitForNonStaleResultsAsOfNow();

  /**
   * Instructs the query to wait for non stale results as of the last write made by any session belonging to the
   * current document store.
   *
   * This ensures that you'll always get the most relevant results for your scenarios using simple indexes (map only or dynamic queries).
   * However, when used to query map/reduce indexes, it does NOT guarantee that the document that this etag belong to is actually considered for the results.
   */
  public TSelf waitForNonStaleResultsAsOfLastWrite();

  /**
   * Instructs the query to wait for non stale results as of the last write made by any session belonging to the
   * current document store.
   *
   * This ensures that you'll always get the most relevant results for your scenarios using simple indexes (map only or dynamic queries).
   * However, when used to query map/reduce indexes, it does NOT guarantee that the document that this etag belong to is actually considered for the results.
   * @param waitTimeout Maximum time to wait for index query results to become non-stale before exception is thrown.
   */
  public TSelf waitForNonStaleResultsAsOfLastWrite(long waitTimeout);

  /**
   * Instructs the query to wait for non stale results as of now for the specified timeout.
   * @param waitTimeout Maximum time to wait for index query results to become non-stale before exception is thrown.
   */
  public TSelf waitForNonStaleResultsAsOfNow(long waitTimeout);

  /**
   * Instructs the query to wait for non stale results as of the cutoff date.
   * @param cutOff Index will be considered stale if modification date of last indexed document is greater than this value.
   */
  public TSelf waitForNonStaleResultsAsOf(Date cutOff);

  /**
   * Instructs the query to wait for non stale results as of the cutoff date for the specified timeout
   * @param cutOff Index will be considered stale if modification date of last indexed document is greater than this value.
   * @param waitTimeout Maximum time to wait for index query results to become non-stale before exception is thrown.
   */
  public TSelf waitForNonStaleResultsAsOf(Date cutOff, long waitTimeout);

  /**
   * Instructs the query to wait for non stale results as of the cutoff etag.
   *
   * @param cutOffEtag
   * Cutoff etag is used to check if the index has already process a document with the given
   * etag. Unlike Cutoff, which uses dates and is susceptible to clock synchronization issues between
   * machines, cutoff etag doesn't rely on both the server and client having a synchronized clock and
   * can work without it.
   * However, when used to query map/reduce indexes, it does NOT guarantee that the document that this
   * etag belong to is actually considered for the results.
   * What it does it guarantee that the document has been mapped, but not that the mapped values has been reduced.
   * Since map/reduce queries, by their nature,vtend to be far less susceptible to issues with staleness, this is
   * considered to be an acceptable tradeoff.
   * If you need absolute no staleness with a map/reduce index, you will need to ensure synchronized clocks and
   * use the Cutoff date option, instead.
   */
  public TSelf waitForNonStaleResultsAsOf(Etag cutOffEtag);

  /**
   * Instructs the query to wait for non stale results as of the cutoff etag for the specified timeout.
   *
   * @param cutOffEtag
   * Cutoff etag is used to check if the index has already process a document with the given
   * etag. Unlike Cutoff, which uses dates and is susceptible to clock synchronization issues between
   * machines, cutoff etag doesn't rely on both the server and client having a synchronized clock and
   * can work without it.
   * However, when used to query map/reduce indexes, it does NOT guarantee that the document that this
   * etag belong to is actually considered for the results.
   * What it does it guarantee that the document has been mapped, but not that the mapped values has been reduced.
   * Since map/reduce queries, by their nature,vtend to be far less susceptible to issues with staleness, this is
   * considered to be an acceptable tradeoff.
   * If you need absolute no staleness with a map/reduce index, you will need to ensure synchronized clocks and
   * use the Cutoff date option, instead.
   * @param waitTimeout Maximum time to wait for index query results to become non-stale before exception is thrown.
   */
  public TSelf waitForNonStaleResultsAsOf(Etag cutOffEtag, long waitTimeout);

  /**
   * EXPERT ONLY: Instructs the query to wait for non stale results.
   * This shouldn't be used outside of unit tests unless you are well aware of the implications
   */
  public TSelf waitForNonStaleResults();

  /**
   * Allows you to modify the index query before it is sent to the server
   * @param beforeQueryExecution
   */
  public TSelf beforeQueryExecution(Action1<IndexQuery> beforeQueryExecution);

  /**
   * EXPERT ONLY: Instructs the query to wait for non stale results for the specified wait timeout.
   * This shouldn't be used outside of unit tests unless you are well aware of the implications
   * @param waitTimeout Maximum time to wait for index query results to become non-stale before exception is thrown.
   */
  public TSelf waitForNonStaleResults(long waitTimeout);

  /**
   * Order the search results randomly
   */
  public TSelf randomOrdering();

  /**
   * Order the search results randomly using the specified seed
   * this is useful if you want to have repeatable random queries
   * @param seed
   */
  public TSelf randomOrdering(String seed);

  /**
   * Adds an ordering for a specific field to the query
   * @param fieldName Name of the field.
   * @param descending If set to true [descending]
   */
  public TSelf addOrder(String fieldName, boolean descending);

  /**
   * Adds an ordering for a specific field to the query
   * @param propertySelector Property selector for the field.
   * @param descending If set to true [descending]
   */
  public <TValue> TSelf addOrder(Expression<?> propertySelector, boolean descending);

  /**
   * Adds an ordering for a specific field to the query and specifies the type of field for sorting purposes
   * @param fieldName Name of the field.
   * @param descending If set to true [descending]
   * @param fieldType The type of the field to be sorted.
   */
  public TSelf addOrder (String fieldName, boolean descending, Class<?> fieldType);

  /**
   * Simplified method for opening a new clause within the query
   */
  public TSelf openSubclause();

  /**
   * Simplified method for closing a clause within the query
   */
  public TSelf closeSubclause();

  /**
   * Perform a search for documents which fields that match the searchTerms.
   * If there is more than a single term, each of them will be checked independently.
   * @param fieldName Marks a field in which terms should be looked for
   * @param searchTerms Space separated terms e.g. 'John Adam' means that we will look in selected field for 'John' or 'Adam'.
   */
  public TSelf search(String fieldName, String searchTerms);

  /**
   * Perform a search for documents which fields that match the searchTerms.
   * If there is more than a single term, each of them will be checked independently.
   * @param fieldName Marks a field in which terms should be looked for
   * @param searchTerms Space separated terms e.g. 'John Adam' means that we will look in selected field for 'John' or 'Adam'.
   * @param escapeQueryOptions  Terms escaping strategy. One of the following: EscapeAll, AllowPostfixWildcard, AllowAllWildcards, RawQuery. Default: EscapeQueryOptions.RawQuery
   */
  public TSelf search(String fieldName, String searchTerms, EscapeQueryOptions escapeQueryOptions);

  /**
   * Perform a search for documents which fields that match the searchTerms.
   * If there is more than a single term, each of them will be checked independently.
   * @param propertySelector Expression marking a field in which terms should be looked for
   * @param searchTerms Space separated terms e.g. 'John Adam' means that we will look in selected field for 'John' or 'Adam'.
   */
  public <TValue> TSelf search(Expression<?> propertySelector, String searchTerms);

  /**
   * Perform a search for documents which fields that match the searchTerms.
   * If there is more than a single term, each of them will be checked independently.
   * @param propertySelector Expression marking a field in which terms should be looked for
   * @param searchTerms Space separated terms e.g. 'John Adam' means that we will look in selected field for 'John' or 'Adam'.
   * @param escapeQueryOptions Terms escaping strategy. One of the following: EscapeAll, AllowPostfixWildcard, AllowAllWildcards, RawQuery. Default: EscapeQueryOptions.RawQuery
   */
  public <TValue> TSelf search(Expression<?> propertySelector, String searchTerms, EscapeQueryOptions escapeQueryOptions);

  /**
   * Partition the query so we can intersect different parts of the query
   * across different index entries.
   */
  public TSelf intersect();

  /**
   * Performs a query matching ANY of the provided values against the given field (OR)
   * @param fieldName
   * @param values
   */
  public TSelf containsAny(String fieldName, Collection<Object> values);

  /**
   * Performs a query matching ANY of the provided values against the given field (OR)
   * @param propertySelector
   * @param values
   */
  public TSelf containsAny(Expression<?> propertySelector, Collection<Object> values);

  /**
   * Performs a query matching ALL of the provided values against the given field (AND)
   * @param fieldName
   * @param values
   */
  public TSelf containsAll(String fieldName, Collection<Object> values);

  /**
   * Performs a query matching ALL of the provided values against the given field (AND)
   * @param propertySelector
   * @param values
   */
  public TSelf containsAll(Expression<?> propertySelector, Collection<Object> values);

  /**
   * Called externally to raise the after query executed callback
   * @param afterQueryExecuted
   */
  public void afterQueryExecuted(Action1<QueryResult> afterQueryExecuted);

  /**
   * Called externally to raise the after query executed callback
   * @param result
   */
  public void invokeAfterQueryExecuted(QueryResult result);

  /**
   * Provide statistics about the query, such as total count of matching records
   * @param stats
   */
  public TSelf statistics(Reference<RavenQueryStatistics> stats);

  /**
   * Select the default field to use for this query
   * @param field
   */
  public TSelf usingDefaultField(String field);

  /**
   * Select the default operator to use for this query
   * @param queryOperator
   */
  public TSelf usingDefaultOperator(QueryOperator queryOperator);

  /**
   * Disables tracking for queried entities by Raven's Unit of Work.
   * Usage of this option will prevent holding query results in memory.
   */
  public TSelf noTracking();

  /**
   * Disables caching for query results.
   */
  public TSelf noCaching();

  /**
   * If set to true, this property will send multiple index entries from the same document (assuming the index project them)
   * to the result transformer function. Otherwise, those entries will be consolidate an the transformer will be
   * called just once for each document in the result set
   * @param val
   */
  public TSelf setAllowMultipleIndexEntriesForSameDocumentToResultTransformer(boolean val);


  /**
   * Enables calculation of timings for various parts of a query (Lucene search, loading documents, transforming results). Default: false
   */
  public TSelf showTimings();

  /**
   * Apply distinct operation to this query
   */
  public TSelf distinct();

  /**
   * Sets a transformer to use after executing a query
   * @param resultsTransformer
   */
  public TSelf setResultTransformer(String resultsTransformer);

  /**
   * Adds an ordering by score for a specific field to the query
   */
  public TSelf orderByScore();

  /**
   * Adds an ordering by score for a specific field to the query
   */
  public TSelf orderByScoreDescending();

  /**
   * Adds explanations of scores calculated for queried documents to the query result
   */
  public TSelf explainScores();
}
