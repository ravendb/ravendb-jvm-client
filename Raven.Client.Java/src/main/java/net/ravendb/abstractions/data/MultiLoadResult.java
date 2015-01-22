package net.ravendb.abstractions.data;


import java.util.ArrayList;
import java.util.List;

import net.ravendb.abstractions.json.linq.RavenJObject;

public class MultiLoadResult {
  private List<RavenJObject> results;
  private List<RavenJObject> includes;

  /**
   * Loaded documents. The results will be in exact same order as in keys parameter.
   */
  public List<RavenJObject> getResults() {
    return results;
  }

  /**
   * Loaded documents. The results will be in exact same order as in keys parameter.
   * @param results
   */
  public void setResults(List<RavenJObject> results) {
    this.results = results;
  }

  /**
   * Included documents.
   */
  public List<RavenJObject> getIncludes() {
    return includes;
  }

  /**
   * Included documents.
   * @param includes
   */
  public void setIncludes(List<RavenJObject> includes) {
    this.includes = includes;
  }

  public MultiLoadResult() {
    super();
    results = new ArrayList<>();
    includes = new ArrayList<>();
  }


}
