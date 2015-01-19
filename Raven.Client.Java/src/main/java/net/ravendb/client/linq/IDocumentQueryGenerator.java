package net.ravendb.client.linq;

import net.ravendb.client.IDocumentQuery;
import net.ravendb.client.document.DocumentConvention;

/**
 * Generate a new document query
 */
public interface IDocumentQueryGenerator {
  /**
   * Gets the conventions associated with this query
   */
  public DocumentConvention getConventions();

  /**
   * Create a new query for
   */
  public <T> IDocumentQuery<T> documentQuery(Class<T> clazz, String indexName, boolean isMapReduce);

  /**
   * Generates a query inspector
   */
  public <T> RavenQueryInspector<T> createRavenQueryInspector();

}
