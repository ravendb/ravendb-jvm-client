package net.ravendb.client.connection.implementation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.closure.Action0;
import net.ravendb.abstractions.closure.Action1;
import net.ravendb.abstractions.closure.Action3;
import net.ravendb.abstractions.closure.Delegates;
import net.ravendb.abstractions.closure.Function0;
import net.ravendb.abstractions.connection.CountingStream;
import net.ravendb.abstractions.connection.ErrorResponseException;
import net.ravendb.abstractions.connection.OperationCredentials;
import net.ravendb.abstractions.connection.WebRequestEventArgs;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.HttpMethods;
import net.ravendb.abstractions.exceptions.BadRequestException;
import net.ravendb.abstractions.exceptions.IndexCompilationException;
import net.ravendb.abstractions.exceptions.JsonReaderException;
import net.ravendb.abstractions.exceptions.JsonWriterException;
import net.ravendb.abstractions.exceptions.ServerVersionNotSuppportedException;
import net.ravendb.abstractions.json.linq.JTokenType;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.abstractions.util.NetDateFormat;
import net.ravendb.client.changes.IObservable;
import net.ravendb.client.connection.CachedRequest;
import net.ravendb.client.connection.CreateHttpJsonRequestParams;
import net.ravendb.client.connection.HttpContentExtentions;
import net.ravendb.client.connection.IDocumentStoreReplicationInformer;
import net.ravendb.client.connection.ObservableLineStream;
import net.ravendb.client.connection.ServerClient.HandleReplicationStatusChangesCallback;
import net.ravendb.client.connection.profiling.IHoldProfilingInformation;
import net.ravendb.client.connection.profiling.RequestResultArgs;
import net.ravendb.client.connection.profiling.RequestStatus;
import net.ravendb.client.document.Convention;
import net.ravendb.client.document.FailoverBehaviorSet;
import net.ravendb.client.document.RemoteBulkInsertOperation.BulkInsertEntity;
import net.ravendb.java.http.client.GzipHttpEntity;
import net.ravendb.java.http.client.HttpEval;
import net.ravendb.java.http.client.HttpReset;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.StopWatch;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import com.google.common.io.Closeables;


public class HttpJsonRequest implements CleanCloseable {

  public static final int MINIMUM_SERVER_VERSION = 3000;
  public static final int CUSTOM_BUILD_VERSION = 13;

  public static final String clientVersion = Constants.VERSION;

  private final String url;
  private final HttpMethods method;

  private CloseableHttpClient httpClient;
  private Map<String, String> headers = new HashMap<>();

  private final StopWatch sp;

  private final OperationCredentials _credentials;

  private CachedRequest cachedRequestDetails;
  private final HttpJsonRequestFactory factory;
  private final IHoldProfilingInformation owner;
  private final Convention conventions;
  private boolean disabledAuthRetries;
  private String postedData;
  private boolean isRequestSendToServer;

  boolean shouldCacheRequest;
  private InputStream postedStream;
  private boolean writeCalled;

  private String primaryUrl;

  private String operationUrl;

  private Action3<Map<String, String>, String, String> handleReplicationStatusChanges = Delegates.delegate3();
  private Map<String, String> responseHeaders;

  private long size;
  private int responseStatusCode;

  private boolean skipServerCheck;

  private Long timeout;

  private CloseableHttpResponse response;

  private int contentLength = -1;

  private Map<String, String> defaultRequestHeaders = new HashMap<>();

  @SuppressWarnings("boxing")
  public HttpJsonRequest(CreateHttpJsonRequestParams requestParams, HttpJsonRequestFactory factory) {
    sp = new StopWatch();
    sp.start();
    this._credentials = requestParams.isDisableAuthentication()?null : requestParams.getCredentials();
    this.disabledAuthRetries = requestParams.isDisableAuthentication();

    this.url = requestParams.getUrl();
    this.method = requestParams.getMethod();

    if (requestParams.getTimeout() != null) {
      timeout = requestParams.getTimeout();
    } else {
      timeout = 100 * 3600L;
    }
    this.factory = factory;
    this.owner = requestParams.getOwner();
    this.conventions = requestParams.getConvention();

    httpClient = factory.getHttpClient();

    if (factory.isDisableRequestCompression() == false && requestParams.isDisableRequestCompression() == false) {
      if (method == HttpMethods.POST || method == HttpMethods.PUT || method == HttpMethods.PATCH
        || method == HttpMethods.EVAL) {
        defaultRequestHeaders.put("Content-Encoding", "gzip");
        defaultRequestHeaders.put("Content-Type", "application/json; charset=utf-8");
      }
      if (factory.isAcceptGzipContent()) {
        // Accept-Encoding Parameters are handled by HttpClient
        defaultRequestHeaders.put("Accept-Encoding", "gzip,deflate");
      }
    }
    // content type is set in RequestEntity
    headers.put("Raven-Client-Version", clientVersion);
    writeMetadata(requestParams.getMetadata());
    requestParams.updateHeaders(headers);
  }

  public void removeAuthorizationHeader() {
    defaultRequestHeaders.remove("Authorization");
  }

  public RavenJToken readResponseJson() {
    if (skipServerCheck) {
      RavenJToken result = factory.getCachedResponse(this, null);

      RequestResultArgs args = new RequestResultArgs();
      args.setDurationMilliseconds(calculateDuration());
      args.setMethod(method);
      args.setHttpResult(responseStatusCode);
      args.setStatus(RequestStatus.AGGRESSIVELY_CACHED);
      args.setResult(result.toString());
      args.setUrl(url);
      args.setPostedData(postedData);

      factory.invokeLogRequest(owner, args);

      return result;
    }

    if (writeCalled) {
      return readJsonInternal();
    }

    RavenJToken result = sendRequestInternal(new Function0<HttpUriRequest>() {

      @SuppressWarnings("synthetic-access")
      @Override
      public HttpUriRequest apply() {
        return createWebRequest(url, method);
      }
    }, true);

    if (result != null) {
      return result;
    }
    return readJsonInternal();
  }

  private RavenJToken sendRequestInternal(final Function0<HttpUriRequest> getRequestMessage, final boolean readErrorString) {
    if (isRequestSendToServer) {
      throw new IllegalStateException("Request was already sent to the server, cannot retry request.");
    }

    isRequestSendToServer = true;

    return runWithAuthRetry(new Function0<RavenJToken>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public RavenJToken apply() {
        try {
          HttpUriRequest requestMessage = getRequestMessage.apply();
          copyHeadersToHttpRequestMessage(requestMessage);
          response = httpClient.execute(requestMessage);
          setResponseHeaders(response);
          assertServerVersionSupported();
          responseStatusCode = response.getStatusLine().getStatusCode();
        } catch (IOException e) {
          throw new JsonWriterException(e);
        } finally {
          sp.stop();
        }

        return checkForErrorsAndReturnCachedResultIfAny(readErrorString);
      }
    });
  }

  @SuppressWarnings("boxing")
  private void assertServerVersionSupported() {
    String serverBuildString = responseHeaders.get(Constants.RAVEN_SERVER_BUILD);
    if (serverBuildString == null) {
      return;
    }
    try {
      int serverBuild = Integer.parseInt(serverBuildString);
      if (serverBuild >= MINIMUM_SERVER_VERSION || serverBuild == CUSTOM_BUILD_VERSION) {
        return;
      }
    } catch (NumberFormatException e) {
      // we throw every time when previous return isn't  met.
    }
    throw new ServerVersionNotSuppportedException(
      String.format("Server version %s is not supported. User server with build >= %d", serverBuildString, MINIMUM_SERVER_VERSION));
  }

  private <T> T runWithAuthRetry(Function0<T> requestOperation) {
    int retries = 0;
    while (true) {
      ErrorResponseException responseException;
      try {
        return requestOperation.apply();
      } catch (ErrorResponseException e) {
        if (++retries >= 3 || disabledAuthRetries) {
          throw e;
        }

        if (e.getStatusCode() != HttpStatus.SC_UNAUTHORIZED
          && e.getStatusCode() != HttpStatus.SC_FORBIDDEN
          && e.getStatusCode() != HttpStatus.SC_PRECONDITION_FAILED) {
          throw e;
        }
        responseException = e;
      }
      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN) {
        handleForbiddenResponse(response);
        throw responseException;
      }
      if (handleUnauthorizedResponse(response) == false) {
        throw responseException;
      }
    }
  }

  private void copyHeadersToHttpRequestMessage(HttpUriRequest httpRequestMessage) {
    for (Map.Entry<String, String> kvp : headers.entrySet()) {
      httpRequestMessage.setHeader(kvp.getKey(), kvp.getValue());
    }
  }

  private void setResponseHeaders(HttpResponse response) {
    responseHeaders = new HashMap<>();
    for (Header h : response.getAllHeaders()) {
      responseHeaders.put(h.getName(), h.getValue());
    }
  }

  private RavenJToken checkForErrorsAndReturnCachedResultIfAny(boolean readErrorString) {
    if (response.getStatusLine().getStatusCode() <= 299) {
      return null;
    }

    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED ||
      response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_FOUND ||
      response.getStatusLine().getStatusCode() == HttpStatus.SC_CONFLICT) {
      RequestResultArgs requestResultArgs = new RequestResultArgs();
      requestResultArgs.setDurationMilliseconds(calculateDuration());
      requestResultArgs.setMethod(method);
      requestResultArgs.setHttpResult(response.getStatusLine().getStatusCode());
      requestResultArgs.setStatus(RequestStatus.ERROR_ON_SERVER);
      requestResultArgs.setResult(response.getStatusLine().getReasonPhrase());
      requestResultArgs.setUrl(url);
      requestResultArgs.setPostedData(postedData);

      factory.invokeLogRequest(owner, requestResultArgs);

      throw ErrorResponseException.fromResponseMessage(response, readErrorString);
    }

    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_MODIFIED && cachedRequestDetails != null) {
      factory.updateCacheTime(this);
      RavenJToken result = factory.getCachedResponse(this, responseHeaders);
      handleReplicationStatusChanges.apply(responseHeaders, primaryUrl, operationUrl);

      RequestResultArgs requestResultArgs = new RequestResultArgs();
      requestResultArgs.setDurationMilliseconds(calculateDuration());
      requestResultArgs.setMethod(method);
      requestResultArgs.setStatus(RequestStatus.CACHED);
      requestResultArgs.setResult(result.toString());
      requestResultArgs.setUrl(url);
      requestResultArgs.setPostedData(postedData);
      factory.invokeLogRequest(owner, requestResultArgs);

      return result;
    }

    HttpEntity httpEntity = response.getEntity();
    String readToEnd = "";
    if (httpEntity != null) {
      try {
        readToEnd = IOUtils.toString(httpEntity.getContent(), "UTF-8");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }

    RequestResultArgs requestResultArgs = new RequestResultArgs();
    requestResultArgs.setDurationMilliseconds(calculateDuration());
    requestResultArgs.setMethod(method);
    requestResultArgs.setHttpResult(response.getStatusLine().getStatusCode());
    requestResultArgs.setStatus(RequestStatus.CACHED);
    requestResultArgs.setResult(readToEnd);
    requestResultArgs.setUrl(url);
    requestResultArgs.setPostedData(postedData);
    factory.invokeLogRequest(owner, requestResultArgs);

    if (StringUtils.isBlank(readToEnd)) {
      throw ErrorResponseException.fromResponseMessage(response, true);
    }

    RavenJObject ravenJObject;
    try {
      ravenJObject = RavenJObject.parse(readToEnd);
    } catch (Exception e) {
      throw new ErrorResponseException(response, readToEnd, e);
    }

    if (ravenJObject.containsKey("IndexDefinitionProperty")) {
      IndexCompilationException ex = new IndexCompilationException(ravenJObject.value(String.class, "Message"));
      ex.setIndexDefinitionProperty(ravenJObject.value(String.class, "IndexDefinitionProperty"));
      ex.setProblematicText(ravenJObject.value(String.class, "ProblematicText"));
      throw ex;
    }

    if (response.getStatusLine().getStatusCode() == HttpStatus.SC_BAD_REQUEST
      && ravenJObject.containsKey("Message")) {
      throw new BadRequestException(ravenJObject.value(String.class, "Message"), ErrorResponseException.fromResponseMessage(response));
    }

    if (ravenJObject.containsKey("Error")) {
      StringBuilder sb = new StringBuilder();
      for (Entry<String, RavenJToken> prop : ravenJObject) {
        if ("Error".equals(prop.getKey())) {
          continue;
        }
        sb.append(prop.getKey()).append(": ").append(prop.getValue().toString());
      }

      sb.append("\n");
      sb.append(ravenJObject.value(String.class, "Error"));
      sb.append("\n");

      throw new ErrorResponseException(response, sb.toString(), readToEnd);
    }
    throw new ErrorResponseException(response, readToEnd);
  }

  public byte[] readResponseBytes() throws IOException {
    sendRequestInternal(new Function0<HttpUriRequest>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public HttpUriRequest apply() {
        return createWebRequest(url, method);
      }
    }, false);

    if (response.getEntity() == null) {
      return new byte[0];
    }
    return IOUtils.toByteArray(response.getEntity().getContent());
  }

  public void executeRequest() {
    readResponseJson();
  }

  private boolean handleUnauthorizedResponse(HttpResponse unauthorizedResponse) {
    if (conventions.getHandleUnauthorizedResponse() == null) return false;

    Action1<HttpRequest> handleUnauthorizedResponse = conventions.handleUnauthorizedResponse(unauthorizedResponse, _credentials);
    if (handleUnauthorizedResponse == null) return false;

    recreateHttpClient(handleUnauthorizedResponse);
    return true;
  }

  protected void handleForbiddenResponse(HttpResponse forbiddenResponse) {
    if (conventions.getHandleForbiddenResponse() == null) return;
    conventions.handleForbiddenResponse(forbiddenResponse);
  }

  @SuppressWarnings("unused")
  private void recreateHttpClient(Action1<HttpRequest> configureHttpClient) {
    sp.reset();
    sp.start();
    Closeables.closeQuietly(response);

    isRequestSendToServer = false;

    try {
      if (postedStream != null) {
        postedStream.reset();
      }
    } catch (IOException e) {
      throw new RuntimeException("Unable to reset input stream", e);
    }
  }

  private RavenJToken readJsonInternal() {
    handleReplicationStatusChanges.apply(responseHeaders, primaryUrl, operationUrl);

    try (InputStream responseStream = response.getEntity() != null
      ? response.getEntity().getContent() : new ByteArrayInputStream(new byte[0])) {
      CountingStream countingStream = new CountingStream(responseStream);
      RavenJToken data = RavenJToken.tryLoad(countingStream);

      size = countingStream.getNumberOfReadBytes();

      if (HttpMethods.GET == method && shouldCacheRequest) {
        factory.cacheResponse(url, data, responseHeaders);
      }

      RequestResultArgs args = new RequestResultArgs();
      args.setDurationMilliseconds(calculateDuration());
      args.setMethod(method);
      args.setHttpResult(responseStatusCode);
      args.setStatus(RequestStatus.SEND_TO_SERVER);
      args.setResult((data != null) ? data.toString() : "");
      args.setUrl(url);
      args.setPostedData(postedData);

      factory.invokeLogRequest(owner, args);

      return data;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public HttpJsonRequest addOperationHeaders(Map<String, String> operationsHeaders) {
    for (Entry<String, String> header : operationsHeaders.entrySet()) {
      headers.put(header.getKey(), header.getValue());
    }
    return this;
  }

  public HttpJsonRequest addOperationHeader(String key, String value) {
    headers.put(key, value);
    return this;
  }

  @SuppressWarnings("unused")
  public HttpJsonRequest addReplicationStatusHeaders(String thePrimaryUrl, String currentUrl,
    IDocumentStoreReplicationInformer replicationInformer, FailoverBehaviorSet failoverBehavior,
    HandleReplicationStatusChangesCallback handleReplicationStatusChangesCallback) {

    if (thePrimaryUrl.equalsIgnoreCase(currentUrl)) {
      return this;
    }
    if (replicationInformer.getFailureCount(thePrimaryUrl).longValue() <= 0) {
      return this; // not because of failover, no need to do this.
    }

    Date lastPrimaryCheck = replicationInformer.getFailureLastCheck(thePrimaryUrl);
    headers.put(Constants.RAVEN_CLIENT_PRIMARY_SERVER_URL, toRemoteUrl(thePrimaryUrl));

    NetDateFormat sdf = new NetDateFormat();
    headers.put(Constants.RAVEN_CLIENT_PRIMARY_SERVER_LAST_CHECK, sdf.format(lastPrimaryCheck));

    primaryUrl = thePrimaryUrl;
    operationUrl = currentUrl;

    this.handleReplicationStatusChanges = handleReplicationStatusChangesCallback;
    return this;
  }

  private static String toRemoteUrl(String thePrimaryUrl) {
    try {
      URIBuilder uriBuilder = new URIBuilder(thePrimaryUrl);
      if ("localhost".equals(uriBuilder.getHost()) || "127.0.0.1".equals(uriBuilder.getHost())) {
        uriBuilder.setHost(InetAddress.getLocalHost().getHostName());
      }
      return uriBuilder.toString();
    } catch (URISyntaxException e) {
      throw new RuntimeException("Invalid URI:" + thePrimaryUrl, e);
    } catch (UnknownHostException e) {
      throw new RuntimeException("Unable to fetch hostname", e);
    }
  }

  public double calculateDuration() {
    return sp.getTime();
  }

  @SuppressWarnings("boxing")
  private void writeMetadata(RavenJObject metadata) {
    if (metadata == null || metadata.getCount() == 0) {
      return;
    }

    for (Entry<String, RavenJToken> prop : metadata) {
      if (prop.getValue() == null) {
        continue;
      }

      if (prop.getValue().getType() == JTokenType.OBJECT || prop.getValue().getType() == JTokenType.ARRAY) {
        continue;
      }

      String headerName = prop.getKey();
      if (Constants.METADATA_ETAG_FIELD.equals(headerName)) {
        headerName = "If-None-Match";
      }

      String value = prop.getValue().value(Object.class).toString();

      switch (headerName) {
        case "Content-Length":
          contentLength = prop.getValue().value(int.class);
          break;
        default:
          headers.put(headerName, value);
      }
    }
  }

  public IObservable<String> serverPull() {
    return runWithAuthRetry(new Function0<IObservable<String>>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public IObservable<String> apply() {
        try {
          HttpUriRequest httpRequestMessage = createWebRequest(url, method);
          response = httpClient.execute(httpRequestMessage);
          setResponseHeaders(response);
          assertServerVersionSupported();
          checkForErrorsAndReturnCachedResultIfAny(true);

          final ObservableLineStream observableLineStream = new ObservableLineStream(response.getEntity().getContent(), new Action0() {
            @Override
            public void apply() {
              Closeables.closeQuietly(response);
            }
          });
          observableLineStream.start();
          return observableLineStream;
        } catch (IOException e) {
          Closeables.closeQuietly(response);
          throw new RuntimeException(e.getMessage(), e);
        }
      }
    });
  }

  public void write(final InputStream streamToWrite) {
    postedStream = streamToWrite;
    writeCalled = true;

    sendRequestInternal(new Function0<HttpUriRequest>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public HttpUriRequest apply() {
        HttpEntityEnclosingRequestBase requestMethod = (HttpEntityEnclosingRequestBase) createWebRequest(url, method);
        InputStreamEntity innerEntity = new InputStreamEntity(postedStream, contentLength);
        HttpContentExtentions.setContentType(innerEntity, headers);
        HttpEntity entity = new GzipHttpEntity(innerEntity);
        innerEntity.setChunked(true);
        requestMethod.setEntity(entity);
        return requestMethod;
      }
    }, true);
  }

  public void write(final String data) {
    postedData = data;
    writeCalled = true;

    sendRequestInternal(new Function0<HttpUriRequest>() {
      @SuppressWarnings("synthetic-access")
      @Override
      public HttpUriRequest apply() {
        HttpUriRequest request = createWebRequest(url, method);
        HttpEntityEnclosingRequestBase requestMethod = (HttpEntityEnclosingRequestBase) request;
        HttpEntity entity = null;
        StringEntity innerEntity = new StringEntity(data, ContentType.APPLICATION_JSON);
        innerEntity.setChunked(true);
        if (factory.isDisableRequestCompression()) {
          entity = innerEntity;
        } else {
          entity = new GzipHttpEntity(innerEntity);
        }
        requestMethod.setEntity(entity);
        return request;
      }
    }, true);
  }

  public CloseableHttpResponse executeRawResponse(String data) throws IOException {
    return executeRawResponseInternal(new StringEntity(data));
  }

  public CloseableHttpResponse executeRawResponse() {
    return executeRawResponseInternal(null);
  }

  public CloseableHttpResponse executeRawResponseInternal(final HttpEntity content) {
    response = runWithAuthRetry(new Function0<CloseableHttpResponse>() {
      @SuppressWarnings({"synthetic-access", "hiding"})
      @Override
      public CloseableHttpResponse apply() {
        HttpUriRequest rawRequestMessage = createWebRequest(url, method);
        if (content != null) {
          ((HttpEntityEnclosingRequestBase) rawRequestMessage).setEntity(content);
        }
        copyHeadersToHttpRequestMessage(rawRequestMessage);

        try {
          CloseableHttpResponse response = httpClient.execute(rawRequestMessage);
          if (response.getStatusLine().getStatusCode() >= 300 &&
            (response.getStatusLine().getStatusCode() == HttpStatus.SC_PRECONDITION_FAILED ||
            response.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN ||
            response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED)) {
            throw new ErrorResponseException(response, "Failed request");
          }
          return response;
        } catch (IOException e) {
          throw new JsonReaderException(e);
        }
      }
    });

    responseStatusCode = response.getStatusLine().getStatusCode();
    return response;
  }

  @SuppressWarnings({"hiding", "boxing"})
  private HttpUriRequest createWebRequest(String url, HttpMethods method) {

    HttpUriRequest baseMethod = null;

    switch (method) {
      case GET:
        baseMethod = new HttpGet(url);
        break;
      case POST:
        baseMethod = new HttpPost(url);
        if (owner != null) {
          HttpRequestBase requestBase = (HttpRequestBase) baseMethod;
          RequestConfig requestConfig =  RequestConfig.custom().setExpectContinueEnabled(owner.isExpect100Continue()).build();
          requestBase.setConfig(requestConfig);
        }
        break;
      case PUT:
        baseMethod = new HttpPut(url);
        if (owner != null) {
          HttpRequestBase requestBase = (HttpRequestBase) baseMethod;
          RequestConfig requestConfig = RequestConfig.custom().setExpectContinueEnabled(owner.isExpect100Continue()).build();
          requestBase.setConfig(requestConfig);
        }
        break;
      case DELETE:
        baseMethod = new HttpDelete(url);
        break;
      case PATCH:
        baseMethod = new HttpPatch(url);
        break;
      case HEAD:
        baseMethod = new HttpHead(url);
        break;
      case RESET:
        baseMethod = new HttpReset(url);
        break;
      case EVAL:
        baseMethod = new HttpEval(url);
        break;
      default:
        throw new IllegalArgumentException("Unknown method: " + method);
    }

    if (defaultRequestHeaders != null) {
      for (Map.Entry<String, String> kvp: defaultRequestHeaders.entrySet()) {
        baseMethod.setHeader(kvp.getKey(), kvp.getValue());
      }
    }
    setTimeout((HttpRequestBase) baseMethod, timeout);
    factory.configureRequest(owner, new WebRequestEventArgs(baseMethod, _credentials));
    return baseMethod;
  }

  public static Map<String, String> extractHeaders(Header[] httpResponseHeaders) {
    Map<String, String> result = new HashMap<>();
    for (Header header : httpResponseHeaders) {
      result.put(header.getName(), header.getValue());
    }
    return result;
  }

  public void setTimeout(HttpRequestBase requestBase, long timeoutInMilis) {
    RequestConfig requestConfig = requestBase.getConfig();
    if (requestConfig == null) {
      requestConfig = RequestConfig.DEFAULT;
    }

    requestConfig = RequestConfig.copy(requestConfig).setSocketTimeout((int) timeoutInMilis).setConnectTimeout((int) timeoutInMilis).build();
    requestBase.setConfig(requestConfig);
  }

  @Override
  public void close() {
    Closeables.closeQuietly(response);
  }

  public Map<String, String> getResponseHeaders() {
    return responseHeaders;
  }

  public CachedRequest getCachedRequestDetails() {
    return cachedRequestDetails;
  }

  public String getUrl() {
    return url;
  }

  public HttpMethods getMethod() {
    return method;
  }

  public long getSize() {
    return size;
  }

  public int getResponseStatusCode() {
    return responseStatusCode;
  }

  public boolean isShouldCacheRequest() {
    return shouldCacheRequest;
  }

  public boolean isSkipServerCheck() {
    return skipServerCheck;
  }

  public void setSkipServerCheck(boolean skipServerCheck) {
    this.skipServerCheck = skipServerCheck;
  }

  public void setCachedRequestDetails(CachedRequest cachedRequestDetails) {
    this.cachedRequestDetails = cachedRequestDetails;
  }

  public Long getTimeout() {
    return timeout;
  }

  public CloseableHttpResponse getResponse() {
    return response;
  }

  public void setShouldCacheRequest(boolean shouldCacheRequest) {
    this.shouldCacheRequest = shouldCacheRequest;
  }

  public void setResponseStatusCode(int responseStatusCode) {
    this.responseStatusCode = responseStatusCode;
  }

  public void setResponseHeaders(Map<String, String> responseHeaders) {
    this.responseHeaders = responseHeaders;
  }

  public HttpResponse executeRawRequest(final BulkInsertEntity bulkInsertEntity) {
    response = runWithAuthRetry(new Function0<CloseableHttpResponse>() {
      @SuppressWarnings({"synthetic-access", "hiding"})
      @Override
      public CloseableHttpResponse apply() {
        HttpUriRequest rawRequestMessage = createWebRequest(url, method);
        ((HttpEntityEnclosingRequestBase)rawRequestMessage).setEntity(bulkInsertEntity);
        copyHeadersToHttpRequestMessage(rawRequestMessage);

        try {
          CloseableHttpResponse response = httpClient.execute(rawRequestMessage);
          if (response.getStatusLine().getStatusCode() >= 300 &&
            (response.getStatusLine().getStatusCode() == HttpStatus.SC_PRECONDITION_FAILED ||
             response.getStatusLine().getStatusCode() == HttpStatus.SC_FORBIDDEN ||
             response.getStatusLine().getStatusCode() == HttpStatus.SC_UNAUTHORIZED)) {
            throw new ErrorResponseException(response, "Failed request");
          }
          return response;
        } catch (IOException e) {
          throw new JsonWriterException(e);
        }
      }
    });

    responseStatusCode = response.getStatusLine().getStatusCode();
    return response;
  }

}
