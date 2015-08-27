package net.ravendb.abstractions.data;

import net.ravendb.abstractions.basic.Reference;
import net.ravendb.abstractions.indexing.SortOptions;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.abstractions.util.EscapingHelper;
import net.ravendb.abstractions.util.NetDateFormat;
import net.ravendb.client.utils.UrlUtils;
import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;


/**
 * All the information required to query a Raven index
 */
public class IndexQuery {
  private int pageSize;

  /**
   * Initializes a new instance of the {@link IndexQuery} class.
   */
  public IndexQuery() {
    totalSize = new Reference<>();
    skippedResults = new Reference<>();
    pageSize = 128;
  }

  public IndexQuery(String query) {
    this();
    this.query = query;
  }

  private boolean pageSizeSet;
  private boolean distinct;
  private String query;
  private Reference<Integer> totalSize;
  private Map<String, SortOptions> sortHints;
  private Map<String, RavenJToken> transformerParameters;
  private int start;
  private String[] fieldsToFetch;
  private SortedField[] sortedFields;
  private Date cutoff;
  private Etag cutoffEtag;
  private boolean waitForNonStaleResultsAsOfNow;
  private boolean waitForNonStaleResults;
  private String defaultField;
  private QueryOperator defaultOperator = QueryOperator.OR;
  private boolean allowMultipleIndexEntriesForSameDocumentToResultTransformer;
  private Reference<Integer> skippedResults;
  private boolean debugOptionGetIndexEntires;
  private HighlightedField[] highlightedFields;
  private String[] highlighterPreTags;
  private String[] highlighterPostTags;
  private String highlighterKeyName;
  private String resultsTransformer;
  private boolean disableCaching;
  private boolean explainScores;
  private boolean showTimings;

  /**
   * @return Indicates if detailed timings should be calculated for various query parts (Lucene search, loading documents, transforming results). Default: false
   */
  public boolean isShowTimings() {
    return showTimings;
  }

  /**
   * For internal use only.
   */
  public Map<String, SortOptions> getSortHints() {
    return sortHints;
  }

  /**
   * For internal use only.
   * @param sortHints
   */
  public void setSortHints(Map<String, SortOptions> sortHints) {
    this.sortHints = sortHints;
  }

  /**
   * Indicates if detailed timings should be calculated for various query parts (Lucene search, loading documents, transforming results). Default: false
   * @param showTimings
   */
  public void setShowTimings(boolean showTimings) {
    this.showTimings = showTimings;
  }

  /**
   * Used to calculate index staleness. When set to <c>true</c> CutOff will be set to DateTime.UtcNow on server side.
   */
  public boolean isWaitForNonStaleResultsAsOfNow() {
    return waitForNonStaleResultsAsOfNow;
  }

  /**
   * CAUTION. Used by IDocumentSession ONLY. It will have NO effect if used with IDatabaseCommands.
   */
  public boolean isWaitForNonStaleResults() {
    return waitForNonStaleResults;
  }

  /**
   * CAUTION. Used by IDocumentSession ONLY. It will have NO effect if used with IDatabaseCommands.
   * @param waitForNonStaleResults
   */
  public void setWaitForNonStaleResults(boolean waitForNonStaleResults) {
    this.waitForNonStaleResults = waitForNonStaleResults;
  }

  /**
   * Used to calculate index staleness. When set to <c>true</c> CutOff will be set to DateTime.UtcNow on server side.
   * @param waitForNonStaleResultsAsOfNow
   */
  public void setWaitForNonStaleResultsAsOfNow(boolean waitForNonStaleResultsAsOfNow) {
    this.waitForNonStaleResultsAsOfNow = waitForNonStaleResultsAsOfNow;
  }

  /**
   * Name of transformer to use on query results.
   */
  public String getResultsTransformer() {
    return resultsTransformer;
  }

  /**
   * Name of transformer to use on query results.
   * @param resultsTransformer
   */
  public void setResultsTransformer(String resultsTransformer) {
    this.resultsTransformer = resultsTransformer;
  }

  /**
   * Whatever we should apply distinct operation to the query on the server side.
   */
  public boolean isDistinct() {
    return distinct;
  }

  /**
   * Whatever we should apply distinct operation to the query on the server side.
   * @param distinct
   */
  public void setDistinct(boolean distinct) {
    this.distinct = distinct;
  }

  /**
   * Array of fields containing highlighting information.
   */
  public HighlightedField[] getHighlightedFields() {
    return highlightedFields;
  }

  /**
   * Array of fields containing highlighting information.
   * @param highlightedFields
   */
  public void setHighlightedFields(HighlightedField[] highlightedFields) {
    this.highlightedFields = highlightedFields;
  }

  /**
   * Array of highlighter pre tags that will be applied to highlighting results.
   */
  public String[] getHighlighterPreTags() {
    return highlighterPreTags;
  }

  /**
   * Array of highlighter pre tags that will be applied to highlighting results.
   * @param highlighterPreTags
   */
  public void setHighlighterPreTags(String[] highlighterPreTags) {
    this.highlighterPreTags = highlighterPreTags;
  }

  /**
   * Array of highlighter post tags that will be applied to highlighting results.
   */
  public String[] getHighlighterPostTags() {
    return highlighterPostTags;
  }

  /**
   * Array of highlighter post tags that will be applied to highlighting results.
   * @param highlighterPostTags
   */
  public void setHighlighterPostTags(String[] highlighterPostTags) {
    this.highlighterPostTags = highlighterPostTags;
  }

  /**
   * Gets highligter key name.
   */
  public String getHighlighterKeyName() {
    return highlighterKeyName;
  }

  /**
   * Sets highligter key name.
   * @param highlighterKeyName
   */
  public void setHighlighterKeyName(String highlighterKeyName) {
    this.highlighterKeyName = highlighterKeyName;
  }

  /**
   * Whatever we should disable caching of query results
   */
  public boolean isDisableCaching() {
    return disableCaching;
  }

  /**
   * Whatever we should disable caching of query results
   * @param disableCaching
   */
  public void setDisableCaching(boolean disableCaching) {
    this.disableCaching = disableCaching;
  }

  /**
   * Whatever we should get the raw index entries.
   */
  public boolean isDebugOptionGetIndexEntires() {
    return debugOptionGetIndexEntires;
  }

  /**
   * Whatever a query result should contains an explanation about how docs scored against query
   */
  public boolean isExplainScores() {
    return explainScores;
  }

  /**
   * Whatever a query result should contains an explanation about how docs scored against query
   * @param explainScores
   */
  public void setExplainScores(boolean explainScores) {
    this.explainScores = explainScores;
  }

  /**
   * Whatever we should get the raw index entries.
   * @param debugOptionGetIndexEntires
   */
  public void setDebugOptionGetIndexEntires(boolean debugOptionGetIndexEntires) {
    this.debugOptionGetIndexEntires = debugOptionGetIndexEntires;
  }

  /**
   * If set to true, this property will send multiple index entries from the same document (assuming the index project them)
   * to the result transformer function. Otherwise, those entries will be consolidate an the transformer will be
   * called just once for each document in the result set
   */
  public boolean isAllowMultipleIndexEntriesForSameDocumentToResultTransformer() {
    return allowMultipleIndexEntriesForSameDocumentToResultTransformer;
  }

  /**
   * If set to true, this property will send multiple index entries from the same document (assuming the index project them)
   * to the result transformer function. Otherwise, those entries will be consolidate an the transformer will be
   * called just once for each document in the result set
   * @param allowMultipleIndexEntriesForSameDocumentToResultTransformer
   */
  public void setAllowMultipleIndexEntriesForSameDocumentToResultTransformer(
    boolean allowMultipleIndexEntriesForSameDocumentToResultTransformer) {
    this.allowMultipleIndexEntriesForSameDocumentToResultTransformer = allowMultipleIndexEntriesForSameDocumentToResultTransformer;
  }

  /**
   * For internal use only.
   */
  public Reference<Integer> getSkippedResults() {
    return skippedResults;
  }

  /**
   * For internal use only.
   * @param skippedResults
   */
  public void setSkippedResults(Reference<Integer> skippedResults) {
    this.skippedResults = skippedResults;
  }

  /**
   * Changes the default operator mode we use for queries.
   * When set to Or a query such as 'Name:John Age:18' will be interpreted as:
   *  Name:John OR Age:18
   * When set to And the query will be interpreted as:
   *  Name:John AND Age:18
   */
  public QueryOperator getDefaultOperator() {
    return defaultOperator;
  }

  /**
   * Changes the default operator mode we use for queries.
   * When set to Or a query such as 'Name:John Age:18' will be interpreted as:
   *  Name:John OR Age:18
   * When set to And the query will be interpreted as:
   *  Name:John AND Age:18
   * @param defaultOperator
   */
  public void setDefaultOperator(QueryOperator defaultOperator) {
    this.defaultOperator = defaultOperator;
  }

  /**
   * Gets or sets the cutoff etag.
   * Cutoff etag is used to check if the index has already process a document with the given
   * etag. Unlike Cutoff, which uses dates and is susceptible to clock synchronization issues between
   * machines, cutoff etag doesn't rely on both the server and client having a synchronized clock and
   * can work without it.
   * However, when used to query map/reduce indexes, it does NOT guarantee that the document that this
   * etag belong to is actually considered for the results.
   * What it does it guarantee that the document has been mapped, but not that the mapped values has been reduced.
   * Since map/reduce queries, by their nature, tend to be far less susceptible to issues with staleness, this is
   * considered to be an acceptable tradeoff.
   * If you need absolute no staleness with a map/reduce index, you will need to ensure synchronized clocks and
   * use the Cutoff date option, instead.
   * @return etag
   */
  public Etag getCutoffEtag() {
    return cutoffEtag;
  }

  /**
   * Default field to use when querying directly on the Lucene query
   */
  public String getDefaultField() {
    return defaultField;
  }

  /**
   * Default field to use when querying directly on the Lucene query
   * @param defaultField
   */
  public void setDefaultField(String defaultField) {
    this.defaultField = defaultField;
  }

  /**
   * Gets or sets the cutoff etag.
   * Cutoff etag is used to check if the index has already process a document with the given
   * etag. Unlike Cutoff, which uses dates and is susceptible to clock synchronization issues between
   * machines, cutoff etag doesn't rely on both the server and client having a synchronized clock and
   * can work without it.
   * However, when used to query map/reduce indexes, it does NOT guarantee that the document that this
   * etag belong to is actually considered for the results.
   * What it does it guarantee that the document has been mapped, but not that the mapped values has been reduced.
   * Since map/reduce queries, by their nature, tend to be far less susceptible to issues with staleness, this is
   * considered to be an acceptable tradeoff.
   * If you need absolute no staleness with a map/reduce index, you will need to ensure synchronized clocks and
   * use the Cutoff date option, instead.
   * @param cutoffEtag
   */
  public void setCutoffEtag(Etag cutoffEtag) {
    this.cutoffEtag = cutoffEtag;
  }

  /**
   * Used to calculate index staleness. Index will be considered stale if modification date of last indexed document is greater than this value.
   */
  public Date getCutoff() {
    return cutoff;
  }

  /**
   * Used to calculate index staleness. Index will be considered stale if modification date of last indexed document is greater than this value.
   * @param cutoff
   */
  public void setCutoff(Date cutoff) {
    this.cutoff = cutoff;
  }

  /**
   * Array of fields containing sorting information.
   */
  public SortedField[] getSortedFields() {
    return sortedFields;
  }

  /**
   * Array of fields containing sorting information.
   * @param sortedFields
   */
  public void setSortedFields(SortedField[] sortedFields) {
    this.sortedFields = sortedFields;
  }

  /**
   * Number of records that should be skipped.
   */
  public int getStart() {
    return start;
  }

  /**
   * Number of records that should be skipped.
   * @param start
   */
  public void setStart(int start) {
    this.start = start;
  }

  /**
   * Parameters that will be passed to transformer (if specified).
   */
  public Map<String, RavenJToken> getTransformerParameters() {
    return transformerParameters;
  }

  /**
   * Parameters that will be passed to transformer (if specified).
   * @param transformerParameters
   */
  public void setTransformerParameters(Map<String, RavenJToken> transformerParameters) {
    this.transformerParameters = transformerParameters;
  }

  /**
   * For internal use only.
   */
  public Reference<Integer> getTotalSize() {
    return totalSize;
  }

  /**
   * @return Whatever the page size was explicitly set or still at its default value
   */
  public boolean isPageSizeSet() {
    return pageSizeSet;
  }

  /**
   * Actual query that will be performed (Lucene syntax).
   */
  public String getQuery() {
    return query;
  }

  /**
   * Actual query that will be performed (Lucene syntax).
   * @param query
   */
  public void setQuery(String query) {
    this.query = query;
  }

  /**
   * Maximum number of records that will be retrieved.
   */
  public int getPageSize() {
    return pageSize;
  }

  /**
   * Maximum number of records that will be retrieved.
   * @param pageSize
   */
  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
    this.pageSizeSet = true;
  }

  /**
   * Array of fields that will be fetched.
   * Fetch order:
   * 1. Stored index fields
   * 2. Document
   */
  public String[] getFieldsToFetch() {
    return fieldsToFetch;
  }

  /**
   * Array of fields that will be fetched.
   * Fetch order:
   * 1. Stored index fields
   * 2. Document
   * @param fieldsToFetch
   */
  public void setFieldsToFetch(String[] fieldsToFetch) {
    this.fieldsToFetch = fieldsToFetch;
  }

  public String getIndexQueryUrl(String operationUrl, String index, String operationName) {
    return getIndexQueryUrl(operationUrl, index, operationName, true, true);
  }

  public String getIndexQueryUrl(String operationUrl, String index, String operationName, boolean includePageSizeEvenIfNotExplicitlySet) {
    return getIndexQueryUrl(operationUrl, index, operationName, includePageSizeEvenIfNotExplicitlySet, true);
  }

  /**
   * Gets the index query URL.
   * @param operationUrl
   * @param index
   * @param operationName
   * @param includePageSizeEvenIfNotExplicitlySet
   * @return index query url
   */
  public String getIndexQueryUrl(String operationUrl, String index, String operationName, boolean includePageSizeEvenIfNotExplicitlySet, boolean includeQuery) {
    if (operationUrl.endsWith("/"))
      operationUrl = operationUrl.substring(0, operationUrl.length() - 1);
    StringBuilder path = new StringBuilder()
    .append(operationUrl)
    .append("/")
    .append(operationName)
    .append("/")
    .append(index);

    appendQueryString(path, includePageSizeEvenIfNotExplicitlySet, includeQuery);

    return path.toString();
  }

  public String getMinimalQueryString() {
    StringBuilder sb = new StringBuilder();
    appendMinimalQueryString(sb, true);
    return sb.toString();
  }


  public String getQueryString() {
    StringBuilder sb = new StringBuilder();
    appendQueryString(sb);
    return sb.toString();
  }

  public void appendQueryString(StringBuilder path){
    appendQueryString(path, true, true);
  }

  public void appendQueryString(StringBuilder path, boolean includePageSizeEvenIfNotExplicitlySet, boolean includeQuery) {
    path.append("?");

    appendMinimalQueryString(path, includeQuery);

    if (start != 0) {
      path.append("&start=").append(start);
    }

    if (includePageSizeEvenIfNotExplicitlySet || pageSizeSet) {
      path.append("&pageSize=").append(pageSize);
    }

    if (isAllowMultipleIndexEntriesForSameDocumentToResultTransformer()) {
      path.append("&allowMultipleIndexEntriesForSameDocumentToResultTransformer=true");
    }

    if (isDistinct()) {
      path.append("&distinct=true");
    }

    if (showTimings) {
      path.append("&showTimings=true");
    }

    if (fieldsToFetch != null) {
      for (String field: fieldsToFetch) {
        if (StringUtils.isNotEmpty(field)) {
          path.append("&fetch=").append(UrlUtils.escapeDataString(field));
        }
      }
    }

    if (sortedFields != null) {
      for (SortedField field: sortedFields) {
        if (field != null) {
          path.append("&sort=").append(field.isDescending()? "-" : "").append(UrlUtils.escapeDataString(field.getField()));
        }
      }
    }

    if (sortHints != null) {
      for(Entry<String, SortOptions> sortHint: sortHints.entrySet()) {
        path.append(String.format("&SortHint%s%s=%s", sortHint.getKey().startsWith("-") ? "" : "-", UrlUtils.escapeDataString(sortHint.getKey()), sortHint.getValue()));
      }
    }


    if (StringUtils.isNotEmpty(resultsTransformer)) {
      path.append("&resultsTransformer=").append(UrlUtils.escapeDataString(resultsTransformer));
    }

    if (transformerParameters != null) {
      for (Map.Entry<String, RavenJToken> input: transformerParameters.entrySet()) {
        path.append("&tp-").append(input.getKey()).append("=").append(UrlUtils.escapeDataString(input.getValue().toString()));
      }
    }

    if (cutoff != null) {
      NetDateFormat sdf = new NetDateFormat();
      String cutOffAsString = UrlUtils.escapeDataString(sdf.format(cutoff));
      path.append("&cufOff=").append(cutOffAsString);
    }
    if (cutoffEtag != null) {
      path.append("&cutOffEtag=").append(cutoffEtag);
    }
    if (waitForNonStaleResultsAsOfNow) {
      path.append("&waitForNonStaleResultsAsOfNow=true");
    }
    if (highlightedFields != null) {
      for( HighlightedField field: highlightedFields) {
        path.append("&highlight=").append(field);
      }
    }
    if (highlighterPreTags != null) {
      for(String preTag: highlighterPreTags) {
        path.append("&preTags=").append(UrlUtils.escapeUriString(preTag));
      }
    }

    if (highlighterPostTags != null) {
      for (String postTag: highlighterPostTags) {
        path.append("&postTags=").append(UrlUtils.escapeUriString(postTag));
      }
    }

    if (StringUtils.isNotEmpty(highlighterKeyName)) {
      path.append("&highlighterKeyName=").append(UrlUtils.escapeDataString(highlighterKeyName));
    }

    if (debugOptionGetIndexEntires) {
      path.append("&debug=entries");
    }
  }

  private void appendMinimalQueryString(StringBuilder path, boolean appendQuery) {
    if (StringUtils.isNotEmpty(query) && appendQuery) {
      path.append("&query=").append(EscapingHelper.escapeLongDataString(query));
    }

    if (StringUtils.isNotEmpty(defaultField)) {
      path.append("&defaultField=").append(UrlUtils.escapeDataString(defaultField));
    }
    if (defaultOperator != QueryOperator.OR) {
      path.append("&operator=AND");
    }
    String vars = getCustomQueryStringVariables();
    if (StringUtils.isNotEmpty(vars)) {
      path.append(vars.startsWith("&") ? vars : ("&" + vars));
    }
  }

  protected String getCustomQueryStringVariables() {
    return "";
  }

  @Override
  public IndexQuery clone() {
    try {

      IndexQuery clone = new IndexQuery();

      clone.pageSizeSet = pageSizeSet;
      clone.query = query;
      clone.totalSize = totalSize;
      clone.transformerParameters = new HashMap<>();
      for (String key: transformerParameters.keySet()) {
        clone.transformerParameters.put(key, transformerParameters.get(key).cloneToken());
      }
      clone.start = start;
      clone.fieldsToFetch = fieldsToFetch.clone();
      clone.sortedFields = new SortedField[sortedFields.length];
      for (int i = 0 ; i <  sortedFields.length; i++) {
        clone.sortedFields[i] = sortedFields[i].clone();
      }
      clone.cutoff = cutoff;
      clone.cutoffEtag = cutoffEtag.clone();
      clone.defaultField = defaultField;
      clone.defaultOperator = defaultOperator;
      clone.skippedResults = new Reference<>(skippedResults.value);
      clone.debugOptionGetIndexEntires = debugOptionGetIndexEntires;
      clone.highlightedFields = new HighlightedField[highlightedFields.length];
      for (int i = 0; i < highlightedFields.length; i++) {
        clone.highlightedFields[i] = highlightedFields[i].clone();
      }
      clone.highlighterPreTags = highlighterPreTags.clone();
      clone.highlighterPostTags = highlighterPostTags.clone();
      clone.resultsTransformer = resultsTransformer;
      clone.disableCaching = disableCaching;

      return clone;
    } catch (CloneNotSupportedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public String toString() {
    return query;
  }

}
