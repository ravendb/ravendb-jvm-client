package net.ravendb.client.document.batches;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.data.Constants;
import net.ravendb.abstractions.data.GetRequest;
import net.ravendb.abstractions.data.GetResponse;
import net.ravendb.abstractions.data.JsonDocument;
import net.ravendb.abstractions.data.QueryResult;
import net.ravendb.abstractions.json.linq.RavenJArray;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.client.RavenPagingInformation;
import net.ravendb.client.connection.SerializationHelper;
import net.ravendb.client.document.InMemoryDocumentSessionOperations;
import net.ravendb.client.shard.ShardStrategy;
import net.ravendb.client.utils.UrlUtils;


public class LazyStartsWithOperation<T> implements ILazyOperation {
  private final String keyPrefix;
  private final String matches;
  private final String exclude;
  private final int start;
  private final int pageSize;
  private final InMemoryDocumentSessionOperations sessionOperations;
  private final RavenPagingInformation pagingInformation;
  private final String skipAfter;
  private final Class<T> clazz;
  private QueryResult queryResult;

  private Object result;
  private boolean requiresRetry;

  public LazyStartsWithOperation(Class<T> clazz, String keyPrefix, String matches, String exclude, int start, int pageSize, InMemoryDocumentSessionOperations sessionOperations, RavenPagingInformation pagingInformation, String skipAfter) {
    this.clazz = clazz;
    this.keyPrefix = keyPrefix;
    this.matches = matches;
    this.exclude = exclude;
    this.start = start;
    this.pageSize = pageSize;
    this.sessionOperations = sessionOperations;
    this.pagingInformation = pagingInformation;
    this.skipAfter = skipAfter;
  }

  @SuppressWarnings("boxing")
  @Override
  public GetRequest createRequest() {
    int actualStart = start;
    boolean nextPage = pagingInformation != null && pagingInformation.isForPreviousPage(start, pageSize);
    if (nextPage) {
      actualStart = pagingInformation.getNextPageStart();
    }

    GetRequest getRequest = new GetRequest();
    getRequest.setUrl("/docs");
    getRequest.setQuery(String.format("startsWith=%s&matches=%s&exclude=%s&start=%d&pageSize=%d&next-page=%s&skipAfter=%s", UrlUtils.escapeDataString(keyPrefix),
        UrlUtils.escapeDataString(matches != null ? matches : ""),
        UrlUtils.escapeDataString(exclude != null ? exclude : ""),
        actualStart, pageSize, nextPage ? "true" : "false", skipAfter));
    return getRequest;
  }

  @Override
  public Object getResult() {
    return result;
  }

  @Override
  public QueryResult getQueryResult() {
    return queryResult;
  }


  public void setQueryResult(QueryResult queryResult) {
    this.queryResult = queryResult;
  }

  public void setResult(Object result) {
    this.result = result;
  }

  @Override
  public boolean isRequiresRetry() {
    return requiresRetry;
  }

  public void setRequiresRetry(boolean requiresRetry) {
    this.requiresRetry = requiresRetry;
  }

  @Override
  public void handleResponse(GetResponse response) {
    if (response.isRequestHasErrors()) {
      result = null;
      requiresRetry = false;
      return;
    }

    List<JsonDocument> jsonDocuments = SerializationHelper.ravenJObjectsToJsonDocuments(((RavenJArray)response.getResult()).values(RavenJObject.class));

    List<Object> resultList = new ArrayList<>();
    for (JsonDocument doc: jsonDocuments) {
      resultList.add(sessionOperations.trackEntity(clazz, doc));
    }

    if (pagingInformation !=null) {
      try {
        int nextPageStart = Integer.parseInt(response.getHeaders().get(Constants.NEXT_PAGE_START));
        pagingInformation.fill(start, pageSize, nextPageStart);
      } catch (NumberFormatException e) {
        // ignore
      }
    }

    result = resultList.toArray();
  }


  @SuppressWarnings("unchecked")
  @Override
  public void handleResponses(GetResponse[] responses, ShardStrategy shardStrategy) {
    boolean anyError = false;
    for (GetResponse r: responses) {
      if (r.isRequestHasErrors()) {
        anyError = true;
        break;
      }
    }
    if (anyError) {
      this.result = null;
      requiresRetry = true;
      return;
    }

    List<JsonDocument> jsonDocuments = new ArrayList<>();
    for (GetResponse response : responses) {
      List<JsonDocument> documents = SerializationHelper.ravenJObjectsToJsonDocuments(response.getResult());
      for (JsonDocument d: documents) {
        for (JsonDocument oldDoc: jsonDocuments) {
          if (d.getKey().equals(oldDoc.getKey())) {
            throw new IllegalStateException("Found document with id: " + d.getKey() + " on more than a single shard, which is not allowed. Document keys have to be unique cluster-wide.");
          }
        }
      }

      jsonDocuments.addAll(documents);
    }

    T[] finalResult = (T[]) Array.newInstance(clazz, jsonDocuments.size());
    this.result = finalResult;

    int i = 0;
    for (JsonDocument doc: jsonDocuments) {
      Object entity = sessionOperations.trackEntity(clazz, doc);
      finalResult[i] = (T) entity;
      i++;
    }
  }

  @Override
  public CleanCloseable enterContext() {
    return null;
  }

  @SuppressWarnings("hiding")
  public void handleEmbeddedResponse(Object result) {
    this.result = result;
  }


}
