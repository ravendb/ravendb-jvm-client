package net.ravendb.client.connection.implementation;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.EventHandler;
import net.ravendb.abstractions.basic.EventHelper;
import net.ravendb.abstractions.basic.VoidArgs;
import net.ravendb.abstractions.closure.Action0;
import net.ravendb.abstractions.closure.Action2;
import net.ravendb.abstractions.connection.WebRequestEventArgs;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.connection.CachedRequest;
import net.ravendb.client.connection.CachedRequestOp;
import net.ravendb.client.connection.CreateHttpJsonRequestParams;
import net.ravendb.client.connection.profiling.IHoldProfilingInformation;
import net.ravendb.client.connection.profiling.RequestResultArgs;
import net.ravendb.client.extensions.MultiDatabase;
import net.ravendb.client.util.SimpleCache;

import net.ravendb.java.http.client.RavenResponseContentEncoding;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.http.config.SocketConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.client.StandardHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import com.google.common.io.Closeables;
import org.apache.http.protocol.HttpProcessorBuilder;


/**
 * Create the HTTP Json Requests to the RavenDB Server
 * and manages the http cache
 */
public class HttpJsonRequestFactory implements CleanCloseable {

  private CloseableHttpClient httpClient;

  private List<EventHandler<WebRequestEventArgs>> configureRequest = new ArrayList<>();

  private List<EventHandler<RequestResultArgs>> logRequest = new ArrayList<>();

  private Action0 onDispose;

  private int maxNumberOfCachedRequests;
  private SimpleCache cache;
  private final boolean acceptGzipContent;
  protected AtomicInteger numOfCachedRequests = new AtomicInteger();
  protected int numOfCacheResets;
  private boolean disableRequestCompression;
  private boolean enableBasicAuthenticationOverUnsecuredHttpEvenThoughPasswordsWouldBeSentOverTheWireInClearTextToBeStolenByHackers;
  private ThreadLocal<Long> aggressiveCacheDuration = new ThreadLocal<>(); // in milis
  private ThreadLocal<Boolean> disableHttpCaching = new ThreadLocal<>();
  private volatile boolean disposed;
  private ThreadLocal<Long> requestTimeout=  new ThreadLocal<>();// in milis


  public HttpJsonRequestFactory(int maxNumberOfCachedRequests) {
    this(maxNumberOfCachedRequests, true);
  }

  public HttpJsonRequestFactory(int maxNumberOfCachedRequests, boolean acceptGzipContent) {
    super();
    this.acceptGzipContent = acceptGzipContent;

    PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
    cm.setDefaultMaxPerRoute(10);
    this.httpClient = HttpClients
            .custom()
            .setConnectionManager(cm)
            .disableContentCompression()
            .addInterceptorLast(new RavenResponseContentEncoding())
            .setRetryHandler(new StandardHttpRequestRetryHandler(0, false))
            .setDefaultSocketConfig(SocketConfig.custom().setTcpNoDelay(true).build()).
            build();
    this.maxNumberOfCachedRequests = maxNumberOfCachedRequests;
    resetCache(null);
  }

  public boolean isAcceptGzipContent() {
    return acceptGzipContent;
  }



  public void addConfigureRequestEventHandler(EventHandler<WebRequestEventArgs> event) {
    configureRequest.add(event);
  }


  public void addLogRequestEventHandler(EventHandler<RequestResultArgs> event) {
    logRequest.add(event);
  }

  /**
   * @return the httpClient
   */
  public CloseableHttpClient getHttpClient() {
    return httpClient;
  }

  public void cacheResponse(String url, RavenJToken data, Map<String, String> headers) {
    if (StringUtils.isEmpty(headers.get(Constants.METADATA_ETAG_FIELD))) {
      return;
    }

    RavenJToken clone = data.cloneToken();
    clone.ensureCannotBeChangeAndEnableShapshotting();

    cache.set(url, new CachedRequest(clone, new Date(), new HashMap<>(headers), MultiDatabase.getDatabaseName(url), false));
  }

  @Override
  public void close(){
    if (disposed) {
      return ;
    }
    disposed = true;
    cache.close();
    Closeables.closeQuietly(httpClient);
    if (onDispose != null) {
      onDispose.apply();
    }
  }

  @SuppressWarnings("boxing")
  public CachedRequestOp configureCaching(String url, Action2<String, String> setHeader) {
    CachedRequest cachedRequest = cache.get(url);
    if (cachedRequest == null) {
      return new CachedRequestOp(null, false);
    }
    boolean skipServerCheck = false;
    if (getAggressiveCacheDuration() != null) {
      long totalSeconds = getAggressiveCacheDuration() / 1000;
      if (totalSeconds > 0) {
        setHeader.apply("Cache-Control", "max-age=" + totalSeconds);
      }

      if (cachedRequest.isForceServerCheck() == false && (new Date().getTime() - cachedRequest.getTime().getTime() < getAggressiveCacheDuration())) { //can serve directly from local cache
        skipServerCheck = true;
      }
      cachedRequest.setForceServerCheck(false);
    }
    setHeader.apply("If-None-Match", cachedRequest.getHeaders().get(Constants.METADATA_ETAG_FIELD));
    return new CachedRequestOp(cachedRequest, skipServerCheck);
  }

  private static class SetHeader implements Action2<String, String> {

    private HttpJsonRequest request;

    public SetHeader(HttpJsonRequest request) {
      this.request = request;
    }

    @Override
    public void apply(String headerName, String value) {
      request.addOperationHeader(headerName, value);
    }

  }

  @SuppressWarnings("boxing")
  public HttpJsonRequest createHttpJsonRequest(CreateHttpJsonRequestParams createHttpJsonRequestParams) {
    if (disposed) {
      throw new IllegalStateException("Object was disposed!");
    }

    HttpJsonRequest request = new HttpJsonRequest(createHttpJsonRequestParams, this);
    request.setShouldCacheRequest(createHttpJsonRequestParams.isAvoidCachingRequest() == false
        && createHttpJsonRequestParams.getConvention().shouldCacheRequest(createHttpJsonRequestParams.getUrl()));

    if (request.isShouldCacheRequest() && !getDisableHttpCaching()) {
      CachedRequestOp cachedRequestDetails = configureCaching(createHttpJsonRequestParams.getUrl(), new SetHeader(request));
      request.setCachedRequestDetails(cachedRequestDetails.getCachedRequest());
      request.setSkipServerCheck(cachedRequestDetails.isSkipServerCheck());
    }

    //we don't configure request here as we don't have request yet! - only http client instance
    return request;
  }

  public void configureRequest(IHoldProfilingInformation owner, WebRequestEventArgs args) {
    EventHelper.invoke(configureRequest, owner, args);
  }

  @SuppressWarnings("boxing")
  public CleanCloseable disableAllCaching() {
    final Long oldAggressiveCaching = getAggressiveCacheDuration();
    final Boolean oldHttpCaching = getDisableHttpCaching();

    setAggressiveCacheDuration(null);
    setDisableHttpCaching(true);

    return new CleanCloseable() {
      @Override
      public void close() {
        setAggressiveCacheDuration(oldAggressiveCaching);
        setDisableHttpCaching(oldHttpCaching);
      }
    };
  }

  public void expireItemsFromCache(String db)
  {
    cache.forceServerCheckOfCachedItemsForDatabase(db);
    numOfCacheResets++;
  }

  public Long getAggressiveCacheDuration() {
    return aggressiveCacheDuration.get();
  }

  RavenJToken getCachedResponse(HttpJsonRequest httpJsonRequest, Map<String, String> additionalHeaders) {
    if (httpJsonRequest.getCachedRequestDetails() == null) {
      throw new IllegalStateException("Cannot get cached response from a request that has no cached information");
    }
    httpJsonRequest.setResponseStatusCode(HttpStatus.SC_NOT_MODIFIED);
    httpJsonRequest.setResponseHeaders(new HashMap<>(httpJsonRequest.getCachedRequestDetails().getHeaders()));

    if (additionalHeaders != null && additionalHeaders.containsKey(Constants.RAVEN_FORCE_PRIMARY_SERVER_CHECK)) {
      httpJsonRequest.getResponseHeaders().put(Constants.RAVEN_FORCE_PRIMARY_SERVER_CHECK, additionalHeaders.get(Constants.RAVEN_FORCE_PRIMARY_SERVER_CHECK));
    }

    incrementCachedRequests();
    return httpJsonRequest.getCachedRequestDetails().getData().cloneToken();
  }

  /**
   * The number of currently held requests in the cache
   */
  public int getCurrentCacheSize() {
    return cache.getCurrentSize();
  }

  @SuppressWarnings("boxing")
  public boolean getDisableHttpCaching() {
    Boolean value = disableHttpCaching.get();
    if (value == null) {
      return false;
    }
    return disableHttpCaching.get();
  }


  /**
   * @return the numOfCachedRequests
   */
  public int getNumOfCachedRequests() {
    return numOfCachedRequests.get();
  }

  /**
   * @return the numOfCacheResets
   */
  public int getNumOfCacheResets() {
    return numOfCacheResets;
  }



  public void incrementCachedRequests() {
    numOfCachedRequests.incrementAndGet();
  }

  public void onLogRequest(IHoldProfilingInformation sender, RequestResultArgs requestResult) {
    EventHelper.invoke(logRequest, sender, requestResult);
  }

  /**
   * @return the disableRequestCompression
   */
  public boolean isDisableRequestCompression() {
    return disableRequestCompression;
  }

  /**
   * @return the enableBasicAuthenticationOverUnsecuredHttpEvenThoughPasswordsWouldBeSentOverTheWireInClearTextToBeStolenByHackers
   */
  public boolean isEnableBasicAuthenticationOverUnsecuredHttpEvenThoughPasswordsWouldBeSentOverTheWireInClearTextToBeStolenByHackers() {
    return enableBasicAuthenticationOverUnsecuredHttpEvenThoughPasswordsWouldBeSentOverTheWireInClearTextToBeStolenByHackers;
  }

  public void removeConfigureRequestEventHandler(EventHandler<WebRequestEventArgs> event) {
    configureRequest.remove(event);
  }

  public void removeLogRequestEventHandler(EventHandler<RequestResultArgs> event) {
    logRequest.remove(event);
  }

  @SuppressWarnings("boxing")
  public void resetCache(Integer newMaxNumberOfCachedRequests) {
    if (newMaxNumberOfCachedRequests != null && newMaxNumberOfCachedRequests == maxNumberOfCachedRequests) {
      return;
    }

    if (cache != null) {
      try {
        cache.close();
      } catch (Exception e) { /*ignore */ }
    }

    if (newMaxNumberOfCachedRequests != null) {
      maxNumberOfCachedRequests = newMaxNumberOfCachedRequests;
    }

    cache = new SimpleCache(maxNumberOfCachedRequests);
    numOfCachedRequests = new AtomicInteger();
  }

  public void setAggressiveCacheDuration(Long value) {
    aggressiveCacheDuration.set(value);
  }

  public void setDisableHttpCaching(Boolean value) {
    disableHttpCaching.set(value);
  }

  /**
   * @param disableRequestCompression the disableRequestCompression to set
   */
  public void setDisableRequestCompression(boolean disableRequestCompression) {
    this.disableRequestCompression = disableRequestCompression;
  }

  /**
   *  Advanced: Don't set this unless you know what you are doing!
   *  Enable using basic authentication using http
   *  By default, RavenDB only allows basic authentication over HTTPS, setting this property to true
   *  will instruct RavenDB to make unsecured calls (usually only good for testing / internal networks).
   * @param enableBasicAuthenticationOverUnsecuredHttpEvenThoughPasswordsWouldBeSentOverTheWireInClearTextToBeStolenByHackers the enableBasicAuthenticationOverUnsecuredHttpEvenThoughPasswordsWouldBeSentOverTheWireInClearTextToBeStolenByHackers to set
   */
  public void setEnableBasicAuthenticationOverUnsecuredHttpEvenThoughPasswordsWouldBeSentOverTheWireInClearTextToBeStolenByHackers(
      boolean enableBasicAuthenticationOverUnsecuredHttpEvenThoughPasswordsWouldBeSentOverTheWireInClearTextToBeStolenByHackers) {
    this.enableBasicAuthenticationOverUnsecuredHttpEvenThoughPasswordsWouldBeSentOverTheWireInClearTextToBeStolenByHackers = enableBasicAuthenticationOverUnsecuredHttpEvenThoughPasswordsWouldBeSentOverTheWireInClearTextToBeStolenByHackers;
  }

  /**
   * @param numOfCacheResets the numOfCacheResets to set
   */
  public void setNumOfCacheResets(int numOfCacheResets) {
    this.numOfCacheResets = numOfCacheResets;
  }

  protected void updateCacheTime(HttpJsonRequest httpJsonRequest) {
    if (httpJsonRequest.getCachedRequestDetails() == null) {
      throw new IllegalStateException("Cannot update cached response from a request that has no cached information");
    }
    httpJsonRequest.getCachedRequestDetails().setTime(new Date());
  }

  public Long getRequestTimeout() {
    return requestTimeout.get();
  }


  public void setRequestTimeout(Long requestTimeout) {
    this.requestTimeout.set(requestTimeout);
  }

  public Action0 getOnDispose() {
    return onDispose;
  }

  public void setOnDispose(Action0 onDispose) {
    this.onDispose = onDispose;
  }
}
