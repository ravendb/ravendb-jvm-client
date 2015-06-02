package net.ravendb.client;

import java.util.Date;

import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.IndexQuery;
import net.ravendb.abstractions.indexing.SpatialOptions.SpatialRelation;
import net.ravendb.abstractions.indexing.SpatialOptions.SpatialUnits;
import net.ravendb.client.shard.ShardReduceFunction;
import net.ravendb.client.spatial.SpatialCriteria;

import com.mysema.query.types.Path;


/**
 * Customize the document query
 */
public interface IDocumentQueryCustomization {
  /**
   * Instructs the query to wait for non stale results as of the last write made by any session belonging to the
   * current document store.
   * This ensures that you'll always get the most relevant results for your scenarios using simple indexes (map only or dynamic queries).
   * However, when used to query map/reduce indexes, it does NOT guarantee that the document that this etag belong to is actually considered for the results.
   */
  public IDocumentQueryCustomization waitForNonStaleResultsAsOfLastWrite();

  /**
   * Instructs the query to wait for non stale results as of the last write made by any session belonging to the
   * current document store.
   * This ensures that you'll always get the most relevant results for your scenarios using simple indexes (map only or dynamic queries).
   * However, when used to query map/reduce indexes, it does NOT guarantee that the document that this etag belong to is actually considered for the results.
   * @param waitTimeout Maximum time to wait for index query results to become non-stale before exception is thrown.
   */
  public IDocumentQueryCustomization waitForNonStaleResultsAsOfLastWrite(long waitTimeout);

  /**
   * Instructs the query to wait for non stale results as of now.
   */
  public IDocumentQueryCustomization waitForNonStaleResultsAsOfNow();

  /**
   * Instructs the query to wait for non stale results as of now for the specified timeout.
   * @param waitTimeout Maximum time to wait for index query results to become non-stale before exception is thrown.
   */
  public IDocumentQueryCustomization waitForNonStaleResultsAsOfNow(long waitTimeout);

  /**
   * Instructs the query to wait for non stale results as of the cutoff date.
   * @param cutOff Index will be considered stale if modification date of last indexed document is greater than this value.
   */
  public IDocumentQueryCustomization waitForNonStaleResultsAsOf(Date cutOff);

  public IDocumentQueryCustomization transformResults(ShardReduceFunction func);

  /**
   * Instructs the query to wait for non stale results as of the cutoff date for the specified timeout
   * @param cutOff Index will be considered stale if modification date of last indexed document is greater than this value.
   * @param waitTimeout Maximum time to wait for index query results to become non-stale before exception is thrown.
   */
  public IDocumentQueryCustomization waitForNonStaleResultsAsOf(Date cutOff, long waitTimeout);

  /**
   * Instructs the query to wait for non stale results as of the cutoff etag.
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
  public IDocumentQueryCustomization waitForNonStaleResultsAsOf(Etag cutOffEtag);

  /**
   * Instructs the query to wait for non stale results as of the cutoff etag for the specified timeout.
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
  public IDocumentQueryCustomization waitForNonStaleResultsAsOf(Etag cutOffEtag, long waitTimeout);

  /**
   * EXPERT ONLY: Instructs the query to wait for non stale results.
   * This shouldn't be used outside of unit tests unless you are well aware of the implications
   */
  public IDocumentQueryCustomization waitForNonStaleResults();

  /**
   * Includes the specified path in the query, loading the document specified in that path
   * @param path
   */
  public IDocumentQueryCustomization include(Path<?> path);

  /**
   * Includes the specified path in the query, loading the document specified in that path
   * @param path
   */
  public IDocumentQueryCustomization include(String path);

  /**
   * Includes the specified path in the query, loading the document specified in that path
   * @param path
   */
  public IDocumentQueryCustomization include(Class<?> targetClass, Path<?> path);

  /**
   * EXPERT ONLY: Instructs the query to wait for non stale results for the specified wait timeout.
   * This shouldn't be used outside of unit tests unless you are well aware of the implications
   * @param waitTimeout Maximum time to wait for index query results to become non-stale before exception is thrown.
   */
  public IDocumentQueryCustomization waitForNonStaleResults(long waitTimeout);

  /**
   * Filter matches to be inside the specified radius. This method assumes that spatial data is found under default spatial field name: __spatial
   * @param radius Radius (in kilometers) in which matches should be found.
   * @param latitude Latitude pointing to a circle center.
   * @param longitude Longitude pointing to a circle center.
   */
  public IDocumentQueryCustomization withinRadiusOf(double radius, double latitude, double longitude);

  /**
   * Filter matches to be inside the specified radius
   * @param fieldName Spatial field name.
   * @param radius Radius (in kilometers) in which matches should be found.
   * @param latitude Latitude pointing to a circle center.
   * @param longitude Longitude pointing to a circle center.
   */
  public IDocumentQueryCustomization withinRadiusOf(String fieldName, double radius, double latitude, double longitude);

  /**
   * Filter matches to be inside the specified radius
   * @param radius Radius (measured in units passed to radiusUnits parameter) in which matches should be found.
   * @param latitude Latitude poiting to a circle center.
   * @param longitude Longitude poiting to a circle center.
   * @param radiusUnits Units that will be used to measure distances (Kilometers, Miles).
   */
  public IDocumentQueryCustomization withinRadiusOf(double radius, double latitude, double longitude, SpatialUnits radiusUnits);

  /**
   * Filter matches to be inside the specified radius
   * @param fieldName Spatial field name.
   * @param radius Radius (measured in units passed to radiusUnits parameter) in which matches should be found.
   * @param latitude Latitude pointing to a circle center.
   * @param longitude Longitude pointing to a circle center.
   * @param radiusUnits Units that will be used to measure distances (Kilometers, Miles).
   */
  public IDocumentQueryCustomization withinRadiusOf(String fieldName, double radius, double latitude, double longitude, SpatialUnits radiusUnits);

  /**
   * Filter matches based on a given shape - only documents with the shape defined in fieldName that
   * have a relation rel with the given shapeWKT will be returned
   * @param fieldName The name of the field containing the shape to use for filtering
   * @param shapeWKT The query shape
   * @param rel Spatial relation to check
   */
  public IDocumentQueryCustomization relatesToShape(String fieldName, String shapeWKT, SpatialRelation rel);

  /**
   * Ability to use one factory to determine spatial shape that will be used in query.
   * @param fieldName Spatial field name.
   * @param criteria Function with spatial criteria factory
   */
  public IDocumentQueryCustomization spatial(String fieldName, SpatialCriteria criteria);

  /**
   * When using spatial queries, instruct the query to sort by the distance from the origin point
   */
  public IDocumentQueryCustomization sortByDistance();

  /**
   * When using spatial queries, instruct the query to sort by the distance from the origin point
   */
  public IDocumentQueryCustomization sortByDistance(double lat, double lng);

  /**
   * When using spatial queries, instruct the query to sort by the distance from the origin point
   * @return
   */
  public IDocumentQueryCustomization sortByDistance(double lat, double lng, String sortedFieldName);

  /**
   * Order the search results randomly
   */
  public IDocumentQueryCustomization randomOrdering();

  /**
   * Order the search results randomly using the specified seed
   * this is useful if you want to have repeatable random queries
   * @param seed
   */
  public IDocumentQueryCustomization randomOrdering(String seed);

  /**
   * Sort using custom sorter on the server
   * @param typeName
   */
  public IDocumentQueryCustomization customSortUsing(String typeName);

  /**
   * Sort using custom sorter on the server
   * @param typeName
   * @param descending
   */
  public IDocumentQueryCustomization customSortUsing(String typeName, boolean descending);

  /**
   * Allow you to modify the index query before it is executed
   * @param action
   */
  public IDocumentQueryCustomization beforeQueryExecution(Action1<IndexQuery> action);

  /**
   * Adds matches highlighting for the specified field.
   *
   * The specified field should be analysed and stored for highlighter to work.
   *  For each match it creates a fragment that contains matched text surrounded by highlighter tags.
   * @param fieldName The field name to highlight.
   * @param fragmentLength The fragment length.
   * @param fragmentCount The maximum number of fragments for the field.
   * @param fragmentsField The field in query results item to put highlightings into.
   */
  public IDocumentQueryCustomization highlight(String fieldName, int fragmentLength, int fragmentCount, String fragmentsField);

  /**
   * Adds matches highlighting for the specified field.
   *
   * The specified field should be analysed and stored for highlighter to work.
   * For each match it creates a fragment that contains matched text surrounded by highlighter tags.
   * @param fieldName The field name to highlight.
   * @param fragmentLength The fragment length.
   * @param fragmentCount The maximum number of fragments for the field.
   * @param highlightings Field highlightings for all results.
   */
  public IDocumentQueryCustomization highlight(String fieldName, int fragmentLength, int fragmentCount, Reference<FieldHighlightings> highlightings);

  /**
   * Adds matches highlighting for the specified field.
   *
   * The specified field should be analysed and stored for highlighter to work.
   * For each match it creates a fragment that contains matched text surrounded by highlighter tags.
   * @param fieldName The field name to highlight.
   * @param fragmentLength The fragment length.
   * @param fragmentCount The maximum number of fragments for the field.
   * @param highlightings Field highlightings for all results.
   */
  public IDocumentQueryCustomization highlight(String fieldName, String fieldKeyName, int fragmentLength, int fragmentCount, Reference<FieldHighlightings> highlightings);



  /**
   * If set to true, this property will send multiple index entries from the same document (assuming the index project them)
   * to the result transformer function. Otherwise, those entries will be consolidate an the transformer will be
   * called just once for each document in the result set
   * @param val
   */
  public IDocumentQueryCustomization setAllowMultipleIndexEntriesForSameDocumentToResultTransformer(boolean val);

  /**
   * Sets the tags to highlight matches with.
   * @param preTag Prefix tag.
   * @param postTag Postfix tag.
   */
  public IDocumentQueryCustomization setHighlighterTags(String preTag, String postTag);

  /**
   * Sets the tags to highlight matches with.
   * @param preTags Prefix tags.
   * @param postTags Postfix tags.
   */
  public IDocumentQueryCustomization setHighlighterTags(String[] preTags, String[] postTags);

  /**
   * Disables tracking for queried entities by Raven's Unit of Work.
   * Usage of this option will prevent holding query results in memory.
   */
  public IDocumentQueryCustomization noTracking();

  /**
   * Disables caching for query results.
   */
  public IDocumentQueryCustomization noCaching();

  /**
   * Enables calculation of timings for various parts of a query (Lucene search, loading documents, transforming results). Default: false
   */
  public IDocumentQueryCustomization showTimings();
}
