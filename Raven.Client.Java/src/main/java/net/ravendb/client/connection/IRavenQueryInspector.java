package net.ravendb.client.connection;

import java.util.List;

import net.ravendb.abstractions.basic.Tuple;
import net.ravendb.abstractions.data.Facet;
import net.ravendb.abstractions.data.FacetResults;
import net.ravendb.abstractions.data.IndexQuery;
import net.ravendb.client.IDocumentQuery;
import net.ravendb.client.document.InMemoryDocumentSessionOperations;


/**
 * Provide access to the underlying {@link IDocumentQuery}
 */
public interface IRavenQueryInspector {

  /**
   *  Get the name of the index being queried
   */
  public String getIndexQueried();


  /**
   * Grant access to the database commands
   */
  public IDatabaseCommands getDatabaseCommands();

  /**
   * The query session
   */
  public InMemoryDocumentSessionOperations getSession();

  /**
   * The last term that we asked the query to use equals on
   */
  public Tuple<String, String> getLastEqualityTerm();

  /**
   * Get the index query for this query
   */
  public IndexQuery getIndexQuery();

  /**
   * Get the facets as per the specified facet document with the given start and pageSize
   * @param facetSetupDoc
   * @param start
   * @param pageSize
   */
  public FacetResults getFacets(String facetSetupDoc, int start, Integer pageSize);

  /**
   *  Get the facet results as per the specified facets with the given start and pageSize
   * @param facets
   * @param start
   * @param pageSize
   */
  public FacetResults getFacets(List<Facet> facets, int start, Integer pageSize);

}
