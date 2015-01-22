package net.ravendb.client;

import java.util.List;
import java.util.Map;

import net.ravendb.abstractions.basic.Lazy;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.data.Facet;
import net.ravendb.abstractions.data.FacetResults;
import net.ravendb.abstractions.data.IndexQuery;
import net.ravendb.abstractions.data.QueryResult;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.spatial.SpatialCriteria;

import com.mysema.query.types.Path;


/**
 * A query against a Raven index
 *
 * @param <T>
 */
public interface IDocumentQuery<T> extends IDocumentQueryBase<T, IDocumentQuery<T>>, Iterable<T> {
  /**
   * Selects the specified fields directly from the index if the are stored. If the field is not stored in index, value will come from document directly.
   * @param projectionClass The class of the projection
   * @param fields Array of fields to load.
   */
  public <TProjection> IDocumentQuery<TProjection> selectFields(Class<TProjection> projectionClass, String... fields);

  /**
   * Selects the specified fields directly from the index if the are stored. If the field is not stored in index, value will come from document directly.
   * @param projectionClass The class of the projection
   * @param fields
   * @param projections
   */
  public <TProjection> IDocumentQuery<TProjection> selectFields(Class<TProjection> projectionClass, String[] fields, String[] projections);

  /**
   * Selects the specified fields directly from the index if the are stored. If the field is not stored in index, value will come from document directly.
   *
   * Array of fields will be taken from TProjection
   * @param projectionClass The class of the projection
   */
  public <TProjection> IDocumentQuery<TProjection> selectFields(Class<TProjection> projectionClass);

  /**
   * Sets user defined inputs to the query
   * @param transformerParameter
   */
  public void setTransformerParameters(Map<String, RavenJToken> transformerParameter);

  /**
   * Gets the query result. Accessing this property for the first time will execute the query.
   * @return The query result.
   */
  public QueryResult getQueryResult();

  /**
   * Register the query as a lazy query in the session and return a lazy
   * instance that will evaluate the query only when needed
   */
  public Lazy<List<T>> lazily();

  /**
   * Register the query as a lazy-count query in the session and return a lazy
   * instance that will evaluate the query only when needed.
   */
  public Lazy<Integer> countLazily();

  /**
   * Register the query as a lazy query in the session and return a lazy
   * instance that will evaluate the query only when needed.
   * Also provide a function to execute when the value is evaluated
   * @param onEval
   */
  public Lazy<List<T>> lazily(Action1<List<T>> onEval);

  /**
   * Create the index query object for this query
   */
  public IndexQuery getIndexQuery();

  /**
   * Ability to use one factory to determine spatial shape that will be used in query.
   * @param path Spatial field name.
   * @param criteria Spatial criteria factory
   */
  public IDocumentQuery<T> spatial(Path<?> path, SpatialCriteria criteria);

  /**
   * Ability to use one factory to determine spatial shape that will be used in query.
   * @param name Spatial field name.
   * @param criteria Spatial criteria factory
   */
  public IDocumentQuery<T> spatial(String name, SpatialCriteria criteria);

  /**
   * Whatever we should apply distinct operation to the query on the server side
   */
  public boolean isDistinct();

  /**
   * Get the facets as per the specified doc with the given start and pageSize
   * @param facetSetupDoc
   * @param facetStart
   * @param facetPageSize
   */
  public FacetResults getFacets(String facetSetupDoc, int facetStart, Integer facetPageSize);

  /**
   * Get the facets as per the specified facets with the given start and pageSize
   * @param facets
   * @param facetStart
   * @param facetPageSize
   */
  public FacetResults getFacets(List<Facet> facets, int facetStart, Integer facetPageSize);

  /**
   * Query the facets results for this query using the specified facet document with the given start and pageSize
   * @param facetSetupDoc
   */
  public Lazy<FacetResults> toFacetsLazy(String facetSetupDoc);

  /**
   * Query the facets results for this query using the specified facet document with the given start and pageSize
   * @param facetSetupDoc
   * @param start
   */
  public Lazy<FacetResults> toFacetsLazy(String facetSetupDoc, int start);

  /**
   * Query the facets results for this query using the specified facet document with the given start and pageSize
   * @param facetSetupDoc
   * @param start
   * @param pageSize
   */
  public Lazy<FacetResults> toFacetsLazy(String facetSetupDoc, int start, Integer pageSize);

  /**
   * Query the facets results for this query using the specified list of facets with the given start and pageSize
   * @param facets
   */
  public Lazy<FacetResults> toFacetsLazy(List<Facet> facets);

  /**
   * Query the facets results for this query using the specified list of facets with the given start and pageSize
   * @param facets
   * @param start
   */
  public Lazy<FacetResults> toFacetsLazy(List<Facet> facets, int start);

  /**
   * Query the facets results for this query using the specified list of facets with the given start and pageSize
   * @param facets
   * @param start
   * @param pageSize
   */
  public Lazy<FacetResults> toFacetsLazy(List<Facet> facets, int start, Integer pageSize);

  /**
   * Query the facets results for this query using the specified facet document with the given start and pageSize
   * @param facetSetupDoc
   */
  public FacetResults toFacets(String facetSetupDoc);

  /**
   * Query the facets results for this query using the specified facet document with the given start and pageSize
   * @param facetSetupDoc
   * @param start
   */
  public FacetResults toFacets(String facetSetupDoc, int start);

  /**
   * Query the facets results for this query using the specified facet document with the given start and pageSize
   * @param facetSetupDoc
   * @param start
   * @param pageSize
   */
  public FacetResults toFacets(String facetSetupDoc, int start, Integer pageSize);

  /**
   * Query the facets results for this query using the specified list of facets with the given start and pageSize
   * @param facets
   */
  public FacetResults toFacets(List<Facet> facets);

  /**
   * Query the facets results for this query using the specified list of facets with the given start and pageSize
   * @param facets
   * @param start
   */
  public FacetResults toFacets(List<Facet> facets, int start);

  /**
   * Query the facets results for this query using the specified list of facets with the given start and pageSize
   * @param facets
   * @param start
   * @param pageSize
   */
  public FacetResults toFacets(List<Facet> facets, int start, Integer pageSize);

  /**
   * Returns first result
   */
  public T first();

  /**
   * Returns first result
   */
  public T firstOrDefault();

  /**
   * Materialize query, executes request and returns with results
   */
  public List<T> toList();

  /**
   * Returns single result
   */
  public T single();

  /**
   * Returns single result
   */
  public T singleOrDefault();

  /**
   * Returns if any entry matches query
   */
  public boolean any();

}
