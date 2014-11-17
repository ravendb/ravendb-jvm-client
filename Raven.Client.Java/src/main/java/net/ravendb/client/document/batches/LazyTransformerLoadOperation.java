package net.ravendb.client.document.batches;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.StringUtils;

import net.ravendb.abstractions.data.GetRequest;
import net.ravendb.abstractions.data.GetResponse;
import net.ravendb.abstractions.data.MultiLoadResult;
import net.ravendb.abstractions.data.QueryResult;
import net.ravendb.abstractions.json.linq.RavenJArray;
import net.ravendb.abstractions.json.linq.RavenJObject;
import net.ravendb.abstractions.json.linq.RavenJToken;
import net.ravendb.client.document.sessionoperations.LoadTransformerOperation;
import net.ravendb.client.utils.UrlUtils;


public class LazyTransformerLoadOperation<T> implements ILazyOperation {

  private Class<T> clazz;
  private String[] ids;
  private String transformer;
  private Map<String, RavenJToken> transformerParameters;
  private LoadTransformerOperation loadTransformerOperation;
  private boolean singleResult;
  private Object result;
  private boolean requiresRetry;
  private QueryResult queryResult;


  @Override
  public QueryResult getQueryResult() {
    return queryResult;
  }


  public void setQueryResult(QueryResult queryResult) {
    this.queryResult = queryResult;
  }

  public LazyTransformerLoadOperation(Class<T> clazz, String[] ids, String transformer, Map<String, RavenJToken> transformerParameters, LoadTransformerOperation loadTransformerOperation, boolean singleResult) {
    this.clazz = clazz;
    this.ids = ids;
    this.transformer = transformer;
    this.transformerParameters = transformerParameters;
    this.loadTransformerOperation = loadTransformerOperation;
    this.singleResult = singleResult;
  }

  @Override
  public GetRequest createRequest() {
    List<String> tokens = new ArrayList<>();
    for (String id: ids) {
      tokens.add("id=" + UrlUtils.escapeDataString(id));
    }
    String query = "?" + StringUtils.join(tokens, "&");
    if (StringUtils.isNotEmpty(transformer)) {
      query += "&transformer=" + transformer;

      if (transformerParameters != null) {
        for (Entry<String, RavenJToken> entry: transformerParameters.entrySet()) {
          query += "&tp-" + entry.getKey() + "=" + entry.getValue().toString();
        }
      }
    }

    return new GetRequest("/queries/", query);
  }

  @Override
  public Object getResult() {
    return result;
  }

  @Override
  public boolean isRequiresRetry() {
    return requiresRetry;
  }

  @Override
  public void handleResponse(GetResponse response) {
    if (response.isRequestHasErrors()) {
      throw new IllegalStateException("Got bad status code: " + response.getStatus());
    }
    MultiLoadResult multiLoadResult = new MultiLoadResult();
    multiLoadResult.setIncludes(response.getResult().value(RavenJArray.class, "Includes").values(RavenJObject.class));
    multiLoadResult.setResults(response.getResult().value(RavenJArray.class, "Results").values(RavenJObject.class));
    handleResponse(multiLoadResult);
  }

  @Override
  public AutoCloseable enterContext() {
    return null;
  }

  private void handleResponse(MultiLoadResult multiLoadResult) {
    T[] complete = loadTransformerOperation.complete(clazz, multiLoadResult);
    if (singleResult) {
      result = complete.length >0 ? complete[0] : null;
      return ;
    }
    result = complete;
  }
}
