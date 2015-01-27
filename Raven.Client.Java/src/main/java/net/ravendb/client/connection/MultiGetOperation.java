package net.ravendb.client.connection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.ravendb.abstractions.closure.Action2;
import net.ravendb.abstractions.closure.Function3;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.Etag;
import net.ravendb.abstractions.data.GetRequest;
import net.ravendb.abstractions.data.GetResponse;
import net.ravendb.abstractions.json.linq.RavenJArray;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.connection.implementation.HttpJsonRequest;
import net.ravendb.client.connection.implementation.HttpJsonRequestFactory;
import net.ravendb.client.connection.profiling.IHoldProfilingInformation;
import net.ravendb.client.connection.profiling.RequestResultArgs;
import net.ravendb.client.connection.profiling.RequestStatus;
import net.ravendb.client.document.DocumentConvention;
import net.ravendb.client.exceptions.ConflictException;
import net.ravendb.imports.json.JsonConvert;

import org.apache.http.HttpStatus;


public class MultiGetOperation {
  private IHoldProfilingInformation holdProfilingInformation;
  private DocumentConvention convention;
  private String url;
  private GetRequest[] requests;
  private String requestUri;
  private boolean allRequestsCanBeServedFromAggressiveCache;
  private CachedRequest[] cachedData;

  public String getRequestUri() {
    return requestUri;
  }

  public MultiGetOperation(IHoldProfilingInformation holdProfilingInformation, DocumentConvention convention, String url, GetRequest[] requests) {
    this.holdProfilingInformation = holdProfilingInformation;
    this.convention = convention;
    this.url = url;
    this.requests = requests;

    requestUri = url + "/multi_get";
    if (convention.isUseParallelMultiGet())
    {
      requestUri += "?parallel=yes";
    }
  }

  private static class SetHeaders implements Action2<String, String> {
    private GetRequest getRequest;

    public SetHeaders(GetRequest getRequest) {
      this.getRequest = getRequest;
    }

    @Override
    public void apply(String headerName, String value) {
      getRequest.getHeaders().put(headerName, value);
    }

  }

  @SuppressWarnings("boxing")
  public GetRequest[] preparingForCachingRequest(HttpJsonRequestFactory jsonRequestFactory) {
    cachedData = new CachedRequest[requests.length];
    GetRequest[] requestsForServer = Arrays.copyOf(requests, requests.length);
    if (jsonRequestFactory.getDisableHttpCaching() == false && convention.shouldCacheRequest(requestUri)) {
      for (int i = 0; i < requests.length; i++) {
        GetRequest request = requests[i];
        CachedRequestOp cachingConfiguration = jsonRequestFactory.configureCaching(url + request.getUrlAndQuery(), new SetHeaders(request));
        cachedData[i] = cachingConfiguration.getCachedRequest();
        if (cachingConfiguration.isSkipServerCheck())
          requestsForServer[i] = null;
      }
    }
    boolean allNull = true;
    for(GetRequest request : requestsForServer) {
      allNull &= request == null;
    }
    allRequestsCanBeServedFromAggressiveCache = allNull;
    return requestsForServer;
  }

  public boolean canFullyCache(HttpJsonRequestFactory jsonRequestFactory, HttpJsonRequest httpJsonRequest, String postedData)
  {
    if (allRequestsCanBeServedFromAggressiveCache) { // can be fully served from aggressive cache
      RequestResultArgs args = new RequestResultArgs();

      args.setDurationMilliseconds(httpJsonRequest.calculateDuration());
      args.setMethod(httpJsonRequest.getMethod());
      args.setHttpResult(0);
      args.setStatus(RequestStatus.AGGRESSIVELY_CACHED);
      args.setResult("");
      args.setUrl(httpJsonRequest.getUrl());
      args.setPostedData(postedData);

      jsonRequestFactory.invokeLogRequest(holdProfilingInformation, args);
      return true;
    }
    return false;
  }

  public GetResponse[] handleCachingResponse(GetResponse[] responses, HttpJsonRequestFactory jsonRequestFactory) {
    boolean hasCachedRequests = false;
    RequestStatus[] requestStatuses = new RequestStatus[responses.length];
    for (int i = 0; i < responses.length; i++) {
      if (responses[i] == null || responses[i].getStatus() == HttpStatus.SC_NOT_MODIFIED) {
        hasCachedRequests = true;

        requestStatuses[i] = responses[i] == null ? RequestStatus.AGGRESSIVELY_CACHED : RequestStatus.CACHED;
        if (responses[i] == null) {
          responses[i] = new GetResponse();
          responses[i].setStatus(0);
        }

        for (String header: cachedData[i].getHeaders().keySet()) {
          responses[i].getHeaders().put(header, cachedData[i].getHeaders().get(header));
        }
        responses[i].setResult(cachedData[i].getData().cloneToken());
        jsonRequestFactory.incrementCachedRequests();
      } else {
        requestStatuses[i] = responses[i].isRequestHasErrors() ? RequestStatus.ERROR_ON_SERVER : RequestStatus.SEND_TO_SERVER;

        Map<String, String> nameValueCollection = new HashMap<>();
        for (Map.Entry<String, String> header: responses[i].getHeaders().entrySet()) {
          nameValueCollection.put(header.getKey(), header.getValue());
        }
        jsonRequestFactory.cacheResponse(url + requests[i].getUrlAndQuery(), responses[i].getResult(), nameValueCollection);
      }
    }

    if (hasCachedRequests == false || convention.isDisableProfiling() || holdProfilingInformation.getProfilingInformation().getRequests().size() == 0)
      return responses;

    List<RequestResultArgs> profilingRequests = holdProfilingInformation.getProfilingInformation().getRequests();
    RequestResultArgs lastRequest = profilingRequests.get(profilingRequests.size() - 1);
    for (int i = 0; i < requestStatuses.length; i++) {
      lastRequest.getAdditionalInformation().put("NestedRequestStatus-" + i, requestStatuses[i].toString());
    }
    lastRequest.setResult(JsonConvert.serializeObject(responses));

    return responses;
  }

  @SuppressWarnings("boxing")
  public void tryResolveConflictOrCreateConcurrencyException(GetResponse[] responses,
    Function3<String, RavenJObject, Etag, ConflictException> tryResolveConflictOrCreateConcurrencyException) {

    for (GetResponse response: responses) {
      if (response == null) {
        continue;
      }
      if (response.isRequestHasErrors() && response.getStatus() != HttpStatus.SC_CONFLICT) {
        continue;
      }

      RavenJObject result = (RavenJObject) response.getResult();
      if (result == null) {
        continue;
      }

      if (result.containsKey("Results")) {
        RavenJToken resultsAsToken = result.get("Results");
        if (resultsAsToken == null || !(resultsAsToken instanceof RavenJArray)) {
          continue;
        }

        RavenJArray results = (RavenJArray) resultsAsToken;

        for (RavenJToken value : results) {
          if (value == null || !(value instanceof RavenJObject)) {
            return;
          }
          RavenJObject docResult = (RavenJObject) value;

          RavenJToken metadata = docResult.get(Constants.METADATA);
          if (metadata == null) {
            return;
          }

          if (metadata.value(int.class, "@Http-Status-Code") != HttpStatus.SC_CONFLICT) {
            return ;
          }

          String id = metadata.value(String.class, "@id");
          Etag etag = HttpExtensions.etagHeaderToEtag(metadata.value(String.class, "@etag"));
          tryResolveConflictOrCreateConcurrencyExceptionForSingleDocument(
            tryResolveConflictOrCreateConcurrencyException,
            id, etag, docResult, response);

        }
        continue;
      }

      if (result.containsKey("Conflicts")) {
        String id = response.getHeaders().get(Constants.DOCUMENT_ID_FIELD_NAME);
        Etag etag = HttpExtensions.getEtagHeader(response);

        tryResolveConflictOrCreateConcurrencyExceptionForSingleDocument(
          tryResolveConflictOrCreateConcurrencyException,
          id, etag, result, response);
      }
    }
  }

  @SuppressWarnings("static-method")
  private void tryResolveConflictOrCreateConcurrencyExceptionForSingleDocument(
    Function3<String, RavenJObject, Etag, ConflictException> tryResolveConflictOrCreateConcurrencyException, String id,
    Etag etag, RavenJObject docResult, GetResponse response) {

    ConflictException conflictException = tryResolveConflictOrCreateConcurrencyException.apply(id, docResult, etag);

    if (conflictException != null) {
      throw conflictException;
    }

    response.setStatus(HttpStatus.SC_OK);
    response.setForceRetry(true);
  }

}
