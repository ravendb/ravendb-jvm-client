package net.ravendb.client.documents.session.operations.lazy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.commands.GetDocumentsResult;
import net.ravendb.client.documents.commands.multiGet.GetRequest;
import net.ravendb.client.documents.commands.multiGet.GetResponse;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.DocumentInfo;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.ObjectUtils;

import java.io.IOException;
import java.util.TreeMap;

public class LazyStartsWithOperation<T> implements ILazyOperation {

    private final Class<T> _clazz;
    private final String _idPrefix;
    private final String _matches;
    private final String _exclude;
    private final int _start;
    private final int _pageSize;
    private final InMemoryDocumentSessionOperations _sessionOperations;
    private final String _startAfter;

    public LazyStartsWithOperation(Class<T> clazz, String idPrefix, String matches, String exclude, int start, int pageSize, InMemoryDocumentSessionOperations sessionOperations, String startAfter) {
        _clazz = clazz;
        _idPrefix = idPrefix;
        _matches = matches;
        _exclude = exclude;
        _start = start;
        _pageSize = pageSize;
        _sessionOperations = sessionOperations;
        _startAfter = startAfter;
    }

    @Override
    public GetRequest createRequest() {
        GetRequest request = new GetRequest();
        request.setUrl("/docs");
        request.setQuery(String.format("?startsWith=%s&matches=%s&exclude=%s&start=%d&pageSize=%d&startAfter=%s",
                UrlUtils.escapeDataString(_idPrefix),
                UrlUtils.escapeDataString(ObjectUtils.firstNonNull(_matches, "")),
                UrlUtils.escapeDataString(ObjectUtils.firstNonNull(_exclude, "")),
                _start,
                _pageSize,
                ObjectUtils.firstNonNull(_startAfter, "")
        ));
        return request;
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
        try {
            GetDocumentsResult getDocumentResult = JsonExtensions.getDefaultMapper().readValue(response.getResult(), GetDocumentsResult.class);

            TreeMap<String, Object> finalResults = new TreeMap<>(String::compareToIgnoreCase);

            for (JsonNode document : getDocumentResult.getResults()) {
                DocumentInfo newDocumentInfo = DocumentInfo.getNewDocumentInfo((ObjectNode) document);
                _sessionOperations.documentsById.add(newDocumentInfo);

                if (newDocumentInfo.getId() == null) {
                    continue; // is this possible?
                }

                if (_sessionOperations.isDeleted(newDocumentInfo.getId())) {
                    finalResults.put(newDocumentInfo.getId(), null);
                    continue;
                }

                DocumentInfo doc = _sessionOperations.documentsById.getValue(newDocumentInfo.getId());
                if (doc != null) {
                    finalResults.put(newDocumentInfo.getId(), _sessionOperations.trackEntity(_clazz, doc));
                    continue;
                }

                finalResults.put(newDocumentInfo.getId(), null);
            }

            result = finalResults;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
