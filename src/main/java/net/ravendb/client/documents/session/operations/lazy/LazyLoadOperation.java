package net.ravendb.client.documents.session.operations.lazy;

import net.ravendb.client.documents.commands.GetDocumentsResult;
import net.ravendb.client.documents.commands.multiGet.GetRequest;
import net.ravendb.client.documents.commands.multiGet.GetResponse;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.documents.session.operations.LoadOperation;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.util.UrlUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class LazyLoadOperation<T> implements ILazyOperation {

    private final Class<T> _clazz;
    private final InMemoryDocumentSessionOperations _session;
    private final LoadOperation _loadOperation;
    private String[] _ids;
    private String[] _includes;

    public LazyLoadOperation(Class<T> clazz, InMemoryDocumentSessionOperations session, LoadOperation loadOperation) {
        _clazz = clazz;
        _session = session;
        _loadOperation = loadOperation;
    }

    @Override
    public GetRequest createRequest() {
        List<String> idsToCheckOnServer = Arrays.stream(_ids).filter(id -> !_session.isLoadedOrDeleted(id)).collect(Collectors.toList());

        StringBuilder queryBuilder = new StringBuilder("?");

        if (_includes != null) {
            for (String include : _includes) {
                queryBuilder.append("&include=").append(include);
            }
        }

        idsToCheckOnServer.forEach(id -> queryBuilder.append("&id=").append(UrlUtils.escapeDataString(id)));

        boolean hasItems = !idsToCheckOnServer.isEmpty();

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
        if (id == null) {
            return this;
        }

        if (_ids == null) {
            _ids = new String[] { id };
        }

        return this;
    }

    public LazyLoadOperation<T> byIds(String[] ids) {
        _ids = ids;

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
        _loadOperation.setResult(loadResult);

        if (!requiresRetry) {
            result = _loadOperation.getDocuments(_clazz);
        }
    }
}
