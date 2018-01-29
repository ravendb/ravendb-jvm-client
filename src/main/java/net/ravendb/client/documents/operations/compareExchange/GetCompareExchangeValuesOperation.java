package net.ravendb.client.documents.operations.compareExchange;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.util.Map;

public class GetCompareExchangeValuesOperation<T> implements IOperation<Map<String, CompareExchangeValue<T>>> {

    private final Class<T> _clazz;
    private final String[] _keys;

    private final String _startWith;
    private final Integer _start;
    private final Integer _pageSize;

    public GetCompareExchangeValuesOperation(Class<T> clazz, String[] keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("Keys cannot be null or empty array");
        }
        _keys = keys;
        _clazz = clazz;

        _start = null;
        _pageSize = null;
        _startWith = null;
    }

    public GetCompareExchangeValuesOperation(Class<T> clazz, String startWith) {
        this(clazz, startWith, null, null);
    }

    public GetCompareExchangeValuesOperation(Class<T> clazz, String startWith, Integer start) {
        this(clazz, startWith, start, null);
    }

    public GetCompareExchangeValuesOperation(Class<T> clazz, String startWith, Integer start, Integer pageSize) {
        _startWith = startWith;
        _start = start;
        _pageSize = pageSize;
        _clazz = clazz;

        _keys = null;

    }

    @SuppressWarnings("unchecked")
    @Override
    public RavenCommand<Map<String, CompareExchangeValue<T>>> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new GetCompareExchangeValuesCommand(this, conventions);
    }

    private static class GetCompareExchangeValuesCommand<T> extends RavenCommand<Map<String, CompareExchangeValue<T>>> {
        private final GetCompareExchangeValuesOperation<T> _operation;
        private final DocumentConventions _conventions;

        @SuppressWarnings("unchecked")
        public GetCompareExchangeValuesCommand(GetCompareExchangeValuesOperation<T> operation, DocumentConventions conventions) {
            super((Class<Map<String, CompareExchangeValue<T>>>) (Class<?>)Map.class);
            _operation = operation;
            _conventions = conventions;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            StringBuilder pathBuilder = new StringBuilder(node.getUrl());

            pathBuilder.append("/databases/")
                    .append(node.getDatabase())
                    .append("/cmpxchg?");

            if (_operation._keys != null) {
                for (String key : _operation._keys) {
                    pathBuilder.append("&key=").append(UrlUtils.escapeDataString(key));
                }
            } else {
                if (StringUtils.isNotEmpty(_operation._startWith)) {
                    pathBuilder.append("&startsWith=")
                            .append(UrlUtils.escapeDataString(_operation._startWith));
                }

                if (_operation._start != null) {
                    pathBuilder.append("&start=")
                            .append(_operation._start);
                }

                if (_operation._pageSize != null) {
                    pathBuilder.append("&pageSize=")
                            .append(_operation._pageSize);
                }
            }

            url.value = pathBuilder.toString();

            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = CompareExchangeValueResultParser.getValues(_operation._clazz, response, _conventions);
        }
    }
}
