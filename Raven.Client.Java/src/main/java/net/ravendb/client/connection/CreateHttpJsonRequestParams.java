package net.ravendb.client.connection;

import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.ConventionBase;
import net.ravendb.client.connection.profiling.IHoldProfilingInformation;
import net.ravendb.client.delegates.RequestCachePolicy;
import net.ravendb.client.metrics.IRequestTimeMetric;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class CreateHttpJsonRequestParams implements Serializable {

  private int operationHeadersHash;
  private Map<String, List<String>> operationsHeadersCollection;
  private Map<String, String> operationsHeadersDictionary = new HashMap<>();
  private IHoldProfilingInformation owner;
  private String url;
  private String urlCached;
  private boolean avoidCachingRequest;
  private HttpMethods method;
  private RavenJObject metadata;
  private ConventionBase convention;
  private OperationCredentials credentials;
  private boolean disableRequestCompression;
  private Long timeout;
  private boolean disableAuthentication;
  private RequestCachePolicy shouldCacheRequest;
  private IRequestTimeMetric requestTimeMetric;

  public CreateHttpJsonRequestParams(IHoldProfilingInformation owner, String url, HttpMethods method, RavenJObject metadata, OperationCredentials credentials, ConventionBase convention, IRequestTimeMetric requestTimeMetric, Long timeout) {
    this.owner = owner;
    this.url = url;
    this.method = method;
    this.metadata = metadata;
    this.credentials = credentials;
    this.convention = convention;
    this.requestTimeMetric = requestTimeMetric;
    this.timeout = timeout;
    this.operationsHeadersCollection = new HashMap<>();
    this.shouldCacheRequest = convention != null ? convention.getShouldCacheRequest() : new RequestCachePolicy() {
      @Override
      public Boolean shouldCacheRequest(String url) {
        return false;
      }
    };
  }

  public CreateHttpJsonRequestParams(IHoldProfilingInformation owner, String url, HttpMethods method, OperationCredentials credentials, ConventionBase convention, IRequestTimeMetric requestTimeMetric, Long timeout) {
    this(owner, url, method, new RavenJObject(), credentials, convention, requestTimeMetric, timeout);
  }

  public CreateHttpJsonRequestParams(IHoldProfilingInformation owner, String url, HttpMethods method, OperationCredentials credentials, RequestCachePolicy shouldCacheRequest, IRequestTimeMetric requestTimeMetric, Long timeout) {
    this(owner, url, method, new RavenJObject(), credentials, null, requestTimeMetric, timeout);
    this.shouldCacheRequest = shouldCacheRequest;
  }

  /**
   * Adds the operation headers.
   * @param operationsHeaders
   */
  public CreateHttpJsonRequestParams addOperationHeaders(Map<String, String> operationsHeaders) {
    urlCached = null;
    operationsHeadersDictionary = operationsHeaders;
    for (Entry<String, String> operationsHeader : operationsHeaders.entrySet()) {
      operationHeadersHash = (operationHeadersHash * 397) ^ operationsHeader.getKey().hashCode();
      if (operationsHeader.getValue() != null) {
        operationHeadersHash = (operationHeadersHash * 397) ^ operationsHeader.getKey().hashCode();
      }
    }
    return this;
  }

  /**
   * Adds the operation headers.
   * @param operationsHeaders
   */
  public CreateHttpJsonRequestParams addOperationHeadersMultiMap(Map<String, List<String>> operationsHeaders) {
    urlCached = null;
    operationsHeadersCollection = operationsHeaders;
    for (String operationsHeader : operationsHeadersCollection.keySet()) {
      operationHeadersHash = (operationHeadersHash * 397) ^ operationsHeader.hashCode();
      List<String> values = operationsHeaders.get(operationsHeader);
      if (values == null) {
        continue;
      }
      for (String header : values) {
        if (header != null) {
          operationHeadersHash = (operationHeadersHash * 397) ^ header.hashCode();
        }
      }
    }
    return this;
  }

  private String generateUrl() {
    if (operationHeadersHash == 0) {
      return url;
    }
    return url + (url.contains("?") ? "&" : "?") + "operationHeadersHash=" + operationHeadersHash;
  }

  /**
   * @return the convention
   */
  public ConventionBase getConvention() {
    return convention;
  }

  /**
   * @return the metadata
   */
  public RavenJObject getMetadata() {
    return metadata;
  }

  /**
   * @return the method
   */
  public HttpMethods getMethod() {
    return method;
  }


  /**
   * @return the owner
   */
  public IHoldProfilingInformation getOwner() {
    return owner;
  }

  public OperationCredentials getCredentials() {
    return credentials;
  }

  public void setCredentials(OperationCredentials credentials) {
    this.credentials = credentials;
  }

  /**
   * @return the url
   */
  public String getUrl() {
    if (urlCached != null) {
      return urlCached;
    }
    urlCached = generateUrl();
    return urlCached;
  }

  /**
   * @return the avoidCachingRequest
   */
  public boolean isAvoidCachingRequest() {
    return avoidCachingRequest;
  }

  /**
   * @return the disableRequestCompression
   */
  public boolean isDisableRequestCompression() {
    return disableRequestCompression;
  }

  /**
   * @param avoidCachingRequest the avoidCachingRequest to set
   */
  public CreateHttpJsonRequestParams setAvoidCachingRequest(boolean avoidCachingRequest) {
    this.avoidCachingRequest = avoidCachingRequest;
    return this;
  }

  /**
   * @param convention the convention to set
   */
  public void setConvention(ConventionBase convention) {
    this.convention = convention;
  }

  /**
   * @param disableRequestCompression the disableRequestCompression to set
   */
  public void setDisableRequestCompression(boolean disableRequestCompression) {
    this.disableRequestCompression = disableRequestCompression;
  }

  /**
   * @param metadata the metadata to set
   */
  public void setMetadata(RavenJObject metadata) {
    this.metadata = metadata;
  }

  /**
   * @param method the method to set
   */
  public void setMethod(HttpMethods method) {
    this.method = method;
  }

  /**
   * @param owner the owner to set
   */
  public void setOwner(IHoldProfilingInformation owner) {
    this.owner = owner;
  }

  public void updateHeaders(Map<String, String> headers) {
    if (operationsHeadersDictionary != null) {
      for (Entry<String, String> kvp : operationsHeadersDictionary.entrySet()) {
        headers.put(kvp.getKey(), kvp.getValue());
      }
    }
    if (operationsHeadersCollection != null) {
      for (Entry<String, List<String>> header : operationsHeadersCollection.entrySet()) {
        headers.put(header.getKey(), StringUtils.join(header.getValue(), ","));
      }
    }
  }

  public RequestCachePolicy getShouldCacheRequest() {
    return shouldCacheRequest;
  }

  public void setShouldCacheRequest(RequestCachePolicy shouldCacheRequest) {
    this.shouldCacheRequest = shouldCacheRequest;
  }

  public Long getTimeout() {
    return timeout;
  }

  public void setTimeout(Long timeout) {
    this.timeout = timeout;
  }

  public boolean isDisableAuthentication() {
    return disableAuthentication;
  }

  public void setDisableAuthentication(boolean disableAuthentication) {
    this.disableAuthentication = disableAuthentication;
  }

  public IRequestTimeMetric getRequestTimeMetric() {
    return requestTimeMetric;
  }

  public void setRequestTimeMetric(IRequestTimeMetric requestTimeMetric) {
    this.requestTimeMetric = requestTimeMetric;
  }
}
