package net.ravendb.client.documents.session.operations.lazy;

import net.ravendb.client.documents.commands.GetDocumentsResult;
import net.ravendb.client.documents.commands.multiGet.GetRequest;
import net.ravendb.client.documents.commands.multiGet.GetResponse;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.operations.LoadOperation;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class LazyLoadOperation<T> implements ILazyOperation {

    private final Class<T> _clazz;
    private final InMemoryDocumentSessionOperations _session;
    private final LoadOperation _loadOperation;
    private String[] _ids;
    private String[] _includes;
    private List<String> _alreadyInSession = new ArrayList<>();

    public LazyLoadOperation(Class<T> clazz, InMemoryDocumentSessionOperations session, LoadOperation loadOperation) {
        _clazz = clazz;
        _session = session;
        _loadOperation = loadOperation;
    }

    @Override
    public GetRequest createRequest() {
        StringBuilder queryBuilder = new StringBuilder("?");

        if (_includes != null) {
            for (String include : _includes) {
                queryBuilder.append("&include=").append(include);
            }
        }

        boolean hasItems = false;

        for (String id : _ids) {
            if (_session.isLoadedOrDeleted(id)) {
                _alreadyInSession.add(id);
            } else {
                hasItems = true;
                queryBuilder
                        .append("&id=")
                        .append(UrlUtils.escapeDataString(id));
            }
        }

        if (!hasItems) {
            // no need to hit the server
            result = _loadOperation.getDocuments(_clazz);
            return null;
        }

        GetRequest getRequest = new GetRequest();

        getRequest.setUrl("/docs");
        getRequest.setQuery(queryBuilder.toString());
        return getRequest;
    }

    public LazyLoadOperation<T> byId(String id) {
        if (StringUtils.isBlank(id)) {
            return this;
        }

        if (_ids == null) {
            _ids = new String[] { id };
        }

        return this;
    }

    public LazyLoadOperation<T> byIds(String[] ids) {
        _ids = Arrays.stream(ids)
                .filter(x -> !StringUtils.isBlank(x))
                .distinct()
                .toArray(String[]::new);

        return this;
    }

    public LazyLoadOperation<T> withIncludes(String[] includes) {
        _includes = includes;
        return this;
    }

    private Object result;
    private QueryResult queryResult;
    private boolean requiresRetry;

    @Override
    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public QueryResult getQueryResult() {
        return queryResult;
    }

    public void setQueryResult(QueryResult queryResult) {
        this.queryResult = queryResult;
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
        if (response.isForceRetry()) {
            result = null;
            requiresRetry = true;
            return;
        }

        try {
            GetDocumentsResult multiLoadResult = response.getResult() != null ?
                    JsonExtensions.getDefaultMapper().readValue(response.getResult(), GetDocumentsResult.class)
                    : null;
            handleResponse(multiLoadResult);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleResponse(GetDocumentsResult loadResult) {
        if (!_alreadyInSession.isEmpty()) {
            // push this to the session
            new LoadOperation(_session)
                    .byIds(_alreadyInSession)
                    .getDocuments(_clazz);
        }

        _loadOperation.setResult(loadResult);

        if (!requiresRetry) {
            result = _loadOperation.getDocuments(_clazz);
        }
    }
}
