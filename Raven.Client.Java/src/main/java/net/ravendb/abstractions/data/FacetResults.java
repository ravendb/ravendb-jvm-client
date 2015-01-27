package net.ravendb.abstractions.data;

import java.util.HashMap;
import java.util.Map;

public class FacetResults {
  private Map<String, FacetResult> results;
  private Etag indexStateEtag;

  /**
   * A list of results for the facet.  One entry for each term/range as specified in the facet setup document.
   */
  public Map<String, FacetResult> getResults() {
    return results;
  }

  /**
   * A list of results for the facet.  One entry for each term/range as specified in the facet setup document.
   * @param results
   */
  public void setResults(Map<String, FacetResult> results) {
    this.results = results;
  }

  public FacetResults() {
    results = new HashMap<>();
  }


  public Etag getIndexStateEtag() {
    return indexStateEtag;
  }


  public void setIndexStateEtag(Etag indexStateEtag) {
    this.indexStateEtag = indexStateEtag;
  }
}
