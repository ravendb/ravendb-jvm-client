package net.ravendb.abstractions.data;

import java.util.Map;

import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.utils.UrlUtils;

import org.apache.commons.lang.StringUtils;

public class MoreLikeThisQuery {
  public final static int DEFAULT_MAXIMUM_NUMBER_OF_TOKENS_PARSED = 5000;
  public final static int DEFAULT_MINIMUM_TERM_FREQUENCY = 2;
  public final static int DEFAULT_MINIMUM_DOCUMENT_FREQUENCY = 5;
  public final static int DEFAULT_MAXIMUM_DOCUMENT_FREQUENCY = Integer.MAX_VALUE;
  public final static boolean DEFAULT_BOOST = false;
  public final static float DEFAULT_BOOST_FACTOR = 1;
  public final static int DEFAULT_MINIMUM_WORD_LENGTH = 0;
  public final static int DEFAULT_MAXIMUM_WORD_LENGTH = 0;
  public final static int DEFAULT_MAXIMUM_QUERY_TERMS = 25;

  private Integer minimumTermFrequency;
  private Integer minimumDocumentFrequency;
  private Integer maximumDocumentFrequency;
  private Integer maximumDocumentFrequencyPercentage;
  private Boolean boost;
  private Float boostFactor;
  private Integer minimumWordLength;
  private Integer maximumWordLength;
  private Integer maximumQueryTerms;
  private Integer maximumNumberOfTokensParsed;
  private String stopWordsDocumentId;
  private String[] fields;
  private String documentId;
  private String indexName;
  private Map<String, String> mapGroupFields;
  private String resultsTransformer;
  private String[] includes;
  private Map<String, RavenJToken> transformerParameters;

  /**
   * Array of paths under which document Ids can be found. All found documents will be returned with the query results.
   */
  public String[] getIncludes() {
    return includes;
  }

  /**
   * Array of paths under which document Ids can be found. All found documents will be returned with the query results.
   * @param includes
   */
  public void setIncludes(String[] includes) {
    this.includes = includes;
  }

  /**
   * Parameters that will be passed to transformer.
   */
  public Map<String, RavenJToken> getTransformerParameters() {
    return transformerParameters;
  }

  /**
   * Parameters that will be passed to transformer.
   * @param transformerParameters
   */
  public void setTransformerParameters(Map<String, RavenJToken> transformerParameters) {
    this.transformerParameters = transformerParameters;
  }

  /**
   * Transformer to use on the query results.
   */
  public String getResultsTransformer() {
    return resultsTransformer;
  }

  /**
   * Transformer to use on the query results.
   * @param resultsTransformer
   */
  public void setResultsTransformer(String resultsTransformer) {
    this.resultsTransformer = resultsTransformer;
  }

  /**
   * Boost terms in query based on score. Default is false.
   */
  public Boolean getBoost() {
    return boost;
  }

  /**
   * Boost factor when boosting based on score. Default is 1.
   */
  public Float getBoostFactor() {
    return boostFactor;
  }

  /**
   * The document id to use as the basis for comparison
   */
  public String getDocumentId() {
    return documentId;
  }

  /**
   * The fields to compare
   */
  public String[] getFields() {
    return fields;
  }

  /**
   * The name of the index to use for this operation
   */
  public String getIndexName() {
    return indexName;
  }

  /**
   * Values for the the mapping group fields to use as the basis for comparison
   */
  public Map<String, String> getMapGroupFields() {
    return mapGroupFields;
  }

  /**
   * Ignore words which occur in more than this many documents. Default is {@link Integer#MAX_VALUE}
   */
  public Integer getMaximumDocumentFrequency() {
    return maximumDocumentFrequency;
  }

  /**
   * Ignore words which occur in more than this percentage of documents.
   */
  public Integer getMaximumDocumentFrequencyPercentage() {
    return maximumDocumentFrequencyPercentage;
  }

  /**
   * The maximum number of tokens to parse in each example doc field that is not stored with TermVector support. Default is 5000.
   */
  public Integer getMaximumNumberOfTokensParsed() {
    return maximumNumberOfTokensParsed;
  }

  /**
   * A Query with no more than this many terms. Default is 25.
   */
  public Integer getMaximumQueryTerms() {
    return maximumQueryTerms;
  }

  /**
   * Ignore words greater than this length or if 0 then this has no effect. Default is 0.
   */
  public Integer getMaximumWordLength() {
    return maximumWordLength;
  }

  /**
   * Ignore words which do not occur in at least this many documents. Default is 5.
   */
  public Integer getMinimumDocumentFrequency() {
    return minimumDocumentFrequency;
  }

  /**
   * Ignore terms with less than this frequency in the source doc. Default is 2.
   */
  public Integer getMinimumTermFrequency() {
    return minimumTermFrequency;
  }

  /**
   * Ignore words less than this length or if 0 then this has no effect. Default is 0.
   */
  public Integer getMinimumWordLength() {
    return minimumWordLength;
  }

  /**
   * The document id containing the custom stop words
   */
  public String getStopWordsDocumentId() {
    return stopWordsDocumentId;
  }

  /**
   * Boost terms in query based on score. Default is false.
   * @param boost
   */
  public void setBoost(Boolean boost) {
    this.boost = boost;
  }

  /**
   * Boost factor when boosting based on score. Default is 1.
   * @param boostFactor
   */
  public void setBoostFactor(Float boostFactor) {
    this.boostFactor = boostFactor;
  }

  /**
   * The document id to use as the basis for comparison
   * @param documentId
   */
  public void setDocumentId(String documentId) {
    this.documentId = documentId;
  }

  /**
   * The fields to compare
   * @param fields
   */
  public void setFields(String[] fields) {
    this.fields = fields;
  }

  /**
   * The name of the index to use for this operation
   * @param indexName
   */
  public void setIndexName(String indexName) {
    this.indexName = indexName;
  }

  /**
   * Values for the the mapping group fields to use as the basis for comparison
   * @param mapGroupFields
   */
  public void setMapGroupFields(Map<String, String> mapGroupFields) {
    this.mapGroupFields = mapGroupFields;
  }

  /**
   * Ignore words which occur in more than this many documents. Default is {@link Integer#MAX_VALUE}
   * @param maximumDocumentFrequency
   */
  public void setMaximumDocumentFrequency(Integer maximumDocumentFrequency) {
    this.maximumDocumentFrequency = maximumDocumentFrequency;
  }

  /**
   * Ignore words which occur in more than this percentage of documents.
   * @param maximumDocumentFrequencyPercentage
   */
  public void setMaximumDocumentFrequencyPercentage(Integer maximumDocumentFrequencyPercentage) {
    this.maximumDocumentFrequencyPercentage = maximumDocumentFrequencyPercentage;
  }

  /**
   * The maximum number of tokens to parse in each example doc field that is not stored with TermVector support. Default is 5000.
   * @param maximumNumberOfTokensParsed
   */
  public void setMaximumNumberOfTokensParsed(Integer maximumNumberOfTokensParsed) {
    this.maximumNumberOfTokensParsed = maximumNumberOfTokensParsed;
  }

  /**
   * A Query with no more than this many terms. Default is 25.
   * @param maximumQueryTerms
   */
  public void setMaximumQueryTerms(Integer maximumQueryTerms) {
    this.maximumQueryTerms = maximumQueryTerms;
  }

  /**
   * Ignore words greater than this length or if 0 then this has no effect. Default is 0.
   * @param maximumWordLength
   */
  public void setMaximumWordLength(Integer maximumWordLength) {
    this.maximumWordLength = maximumWordLength;
  }

  /**
   * Ignore words which do not occur in at least this many documents. Default is 5.
   * @param minimumDocumentFrequency
   */
  public void setMinimumDocumentFrequency(Integer minimumDocumentFrequency) {
    this.minimumDocumentFrequency = minimumDocumentFrequency;
  }

  /**
   * Ignore terms with less than this frequency in the source doc. Default is 2.
   * @param minimumTermFrequency
   */
  public void setMinimumTermFrequency(Integer minimumTermFrequency) {
    this.minimumTermFrequency = minimumTermFrequency;
  }

  /**
   * Ignore words less than this length or if 0 then this has no effect. Default is 0.
   * @param minimumWordLength
   */
  public void setMinimumWordLength(Integer minimumWordLength) {
    this.minimumWordLength = minimumWordLength;
  }

  /**
   * The document id containing the custom stop words
   * @param stopWordsDocumentId
   */
  public void setStopWordsDocumentId(String stopWordsDocumentId) {
    this.stopWordsDocumentId = stopWordsDocumentId;
  }

  public String getRequestUri() {
    if (StringUtils.isEmpty(indexName)) {
      throw new IllegalArgumentException("Index name cannot be null or empty");
    }

    StringBuilder uri = new StringBuilder();

    String pathSuffix = "";

    if (!mapGroupFields.isEmpty()) {
      String separator = "";
      for (String key: mapGroupFields.keySet()) {
        pathSuffix = pathSuffix + separator + key + '=' + mapGroupFields.get(key);
        separator = ";";
      }
    } else {
      if(documentId == null) {
        throw new IllegalArgumentException("DocumentId cannot be null");
      }

      pathSuffix = documentId;
    }

    uri.append(String.format("/morelikethis/?index=%s&docid=%s&", UrlUtils.escapeUriString(indexName), UrlUtils.escapeDataString(pathSuffix)));
    if (fields != null) {
      for (String field: fields) {
        uri.append("fields=").append(field).append("&");
      }
    }
    if (boost != null && boost != DEFAULT_BOOST) {
      uri.append("boost=true&");
    }
    if (boostFactor != null && !boostFactor.equals(DEFAULT_BOOST_FACTOR)) {
      uri.append(String.format(Constants.getDefaultLocale(), "boostFactor=%.4f&", boostFactor));
    }
    if (maximumQueryTerms != null && !maximumQueryTerms.equals(DEFAULT_MAXIMUM_QUERY_TERMS)) {
      uri.append("maxQueryTerms=").append(maximumQueryTerms).append("&");
    }
    if (maximumNumberOfTokensParsed != null && !maximumNumberOfTokensParsed.equals(DEFAULT_MAXIMUM_NUMBER_OF_TOKENS_PARSED)) {
      uri.append("maxNumTokens=").append(maximumNumberOfTokensParsed).append("&");
    }
    if (maximumWordLength != null && !maximumWordLength.equals(DEFAULT_MAXIMUM_WORD_LENGTH)) {
      uri.append("maxWordLen=").append(maximumWordLength).append("&");
    }
    if (minimumDocumentFrequency != null && !minimumDocumentFrequency.equals(DEFAULT_MINIMUM_DOCUMENT_FREQUENCY)) {
      uri.append("minDocFreq=").append(minimumDocumentFrequency).append("&");
    }
    if (maximumDocumentFrequency != null && !maximumDocumentFrequency.equals(DEFAULT_MAXIMUM_DOCUMENT_FREQUENCY)) {
      uri.append("maxDocFreq=").append(maximumDocumentFrequency).append("&");
    }
    if (maximumDocumentFrequencyPercentage != null) {
      uri.append("maxDocFreqPct=").append(maximumDocumentFrequencyPercentage).append("&");
    }
    if (minimumTermFrequency != null && !minimumTermFrequency.equals(DEFAULT_MINIMUM_TERM_FREQUENCY)) {
      uri.append("minTermFreq=").append(minimumTermFrequency).append("&");
    }
    if (minimumWordLength != null && !minimumWordLength.equals(DEFAULT_MINIMUM_WORD_LENGTH)) {
      uri.append("minWordLen=").append(minimumWordLength).append("&");
    }
    if (stopWordsDocumentId != null) {
      uri.append("stopWords=").append(stopWordsDocumentId).append("&");
    }
    if (StringUtils.isNotEmpty(resultsTransformer)) {
      uri.append("&resultsTransformer=").append(UrlUtils.escapeDataString(resultsTransformer));
    }

    if (transformerParameters != null) {
      for (Map.Entry<String, RavenJToken> input: transformerParameters.entrySet()) {
        uri.append("&tp-").append(input.getKey()).append("=").append(input.getValue());
      }
    }

    if (includes != null && includes.length > 0) {
      for (String include : includes) {
        uri.append("&include=").append(include);
      }
    }

    return uri.toString();
  }


}
