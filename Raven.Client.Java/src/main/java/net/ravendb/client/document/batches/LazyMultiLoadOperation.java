package net.ravendb.client.document.batches;

import java.util.ArrayList;
import java.util.List;

import net.ravendb.abstractions.basic.CleanCloseable;
import net.ravendb.abstractions.basic.Tuple;
import net.ravendb.abstractions.data.GetRequest;
import net.ravendb.abstractions.data.GetResponse;
import net.ravendb.abstractions.data.MultiLoadResult;
import net.ravendb.abstractions.data.QueryResult;
import net.ravendb.abstractions.json.linq.RavenJArray;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.document.sessionoperations.MultiLoadOperation;
import net.ravendb.client.shard.ShardStrategy;
import net.ravendb.client.utils.UrlUtils;

import org.apache.commons.lang.StringUtils;

public class LazyMultiLoadOperation<T> implements ILazyOperation {

  private final MultiLoadOperation loadOperation;
  private final String[] ids;
  private final String transformer;
  private final Tuple<String, Class<?>>[] includes;
  private final Class<T> clazz;
  private QueryResult queryResult;

  private Object result;
  private boolean requiresRetry;

  public LazyMultiLoadOperation(Class<T> clazz, MultiLoadOperation loadOperation, String[] ids,
    Tuple<String, Class<?>>[] includes, String transformer) {
    this.loadOperation = loadOperation;
    this.ids = ids;
    this.includes = includes;
    this.clazz = clazz;
    this.transformer = transformer;
  }

  @Override
  public QueryResult getQueryResult() {
    return queryResult;
  }

  public void setQueryResult(QueryResult queryResult) {
    this.queryResult = queryResult;
  }

  @Override
  public GetRequest createRequest() {
    String query = "?";
    if (includes != null && includes.length > 0) {
      List<String> queryTokens = new ArrayList<>();
      for (Tuple<String, Class<?>> include : includes) {
        queryTokens.add("include=" + include.getItem1());
      }
      query += StringUtils.join(queryTokens, "&");
    }
    List<String> idTokens = new ArrayList<>();
    for (String id : ids) {
      idTokens.add("id=" + UrlUtils.escapeDataString(id));
    }
    query += "&" + StringUtils.join(idTokens, "&");

    if (StringUtils.isNotEmpty(transformer)) {
      query += "&transformer=" + transformer;
    }

    GetRequest request = new GetRequest();
    request.setUrl("/queries/");
    request.setQuery(query);
    return request;
  }

  @Override
  public Object getResult() {
    return result;
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

  @SuppressWarnings("hiding")
  @Override
  public void handleResponses(GetResponse[] responses, ShardStrategy shardStrategy) {
    List<MultiLoadResult> list = new ArrayList<>();
    for (GetResponse response: responses) {
      RavenJToken result = response.getResult();
      MultiLoadResult loadResult = new MultiLoadResult();
      list.add(loadResult);

      List<RavenJObject> results = new ArrayList<>();
      for (RavenJToken token: result.value(RavenJArray.class, "Results")) {
        if (token instanceof RavenJObject) {
          results.add((RavenJObject) token);
        } else {
          results.add(null);
        }
      }
      loadResult.setResults(results);
      loadResult.setIncludes(result.value(RavenJArray.class, "Includes").values(RavenJObject.class));
    }

    int capacity = 0;
    for (MultiLoadResult r: list) {
      capacity = Math.max(capacity, r.getResults().size());
    }

    MultiLoadResult finalResult = new MultiLoadResult();
    finalResult.setIncludes(new ArrayList<RavenJObject>());
    List<RavenJObject> rList = new ArrayList<>();
    for (int i =0 ; i < capacity; i++) {
      rList.add(null);
    }
    finalResult.setResults(rList);

    for (MultiLoadResult multiLoadResult: list) {
      finalResult.getIncludes().addAll(multiLoadResult.getIncludes());
      for (int i = 0; i < multiLoadResult.getResults().size(); i++) {
        if (finalResult.getResults().get(i) == null) {
          finalResult.getResults().set(i, multiLoadResult.getResults().get(i));
        }
      }
    }

    requiresRetry = loadOperation.setResult(finalResult);

    if (!requiresRetry) {
      this.result = loadOperation.complete(clazz);
    }

  }

  @SuppressWarnings("hiding")
  @Override
  public void handleResponse(GetResponse response) {
    if (response.isForceRetry()) {
      result = null;
      requiresRetry = true;
      return;
    }

    RavenJToken result = response.getResult();
    MultiLoadResult multiLoadResult = new MultiLoadResult();
    multiLoadResult.setIncludes(result.value(RavenJArray.class, "Includes").values(RavenJObject.class));
    List<RavenJObject> results = new ArrayList<>();
    for (RavenJToken token : result.value(RavenJArray.class, "Results")) {
      if (token instanceof RavenJObject) {
        results.add((RavenJObject) token);
      } else {
        results.add(null);
      }
    }
    multiLoadResult.setResults(results);

    handleResponse(multiLoadResult);
  }

  private void handleResponse(MultiLoadResult multiLoadResult) {
    requiresRetry = loadOperation.setResult(multiLoadResult);
    if (requiresRetry == false) {
      result = loadOperation.complete(clazz);
    }
  }

  @Override
  public CleanCloseable enterContext() {
    return loadOperation.enterMultiLoadContext();
  }

}
