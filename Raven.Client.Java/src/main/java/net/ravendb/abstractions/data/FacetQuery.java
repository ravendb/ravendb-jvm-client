package net.ravendb.abstractions.data;

import java.util.List;


public class FacetQuery {
  private String indexName;
  private IndexQuery query;
  private String facetSetupDoc;
  private List<Facet> facets;
  private int pageStart;
  private Integer pageSize;

  /**
   * Index name to run facet query on.
   */
  public String getIndexName() {
    return indexName;
  }

  /**
   * Index name to run facet query on.
   * @param indexName
   */
  public void setIndexName(String indexName) {
    this.indexName = indexName;
  }

  /**
   * Query that should be ran on index.
   */
  public IndexQuery getQuery() {
    return query;
  }

  /**
   * Query that should be ran on index.
   * @param query
   */
  public void setQuery(IndexQuery query) {
    this.query = query;
  }

  /**
   * Id of a facet setup document that can be found in database containing facets (mutually exclusive with Facets).
   */
  public String getFacetSetupDoc() {
    return facetSetupDoc;
  }

  /**
   * Id of a facet setup document that can be found in database containing facets (mutually exclusive with Facets).
   * @param facetSetupDoc
   */
  public void setFacetSetupDoc(String facetSetupDoc) {
    this.facetSetupDoc = facetSetupDoc;
  }

  /**
   * List of facets (mutually exclusive with FacetSetupDoc).
   */
  public List<Facet> getFacets() {
    return facets;
  }

  /**
   * List of facets (mutually exclusive with FacetSetupDoc).
   * @param facets
   */
  public void setFacets(List<Facet> facets) {
    this.facets = facets;
  }

  /**
   * Page start for facet query results.
   */
  public int getPageStart() {
    return pageStart;
  }

  /**
   * Page start for facet query results.
   * @param pageStart
   */
  public void setPageStart(int pageStart) {
    this.pageStart = pageStart;
  }

  /**
   * Page size for facet query results.
   */
  public Integer getPageSize() {
    return pageSize;
  }

  /**
   * Page size for facet query results.
   * @param pageSize
   */
  public void setPageSize(Integer pageSize) {
    this.pageSize = pageSize;
  }

}
