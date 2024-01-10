package net.ravendb.client.documents.operations.compareExchange;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;
import java.util.Map;

public class GetCompareExchangeValuesOperation<T> implements IOperation<Map<String, CompareExchangeValue<T>>> {

    private final Class<T> _clazz;
    private final String[] _keys;

    private final String _startWith;
    private final Integer _start;
    private final Integer _pageSize;

    private final boolean _materializeMetadata;

    public GetCompareExchangeValuesOperation(Class<T> clazz, String[] keys) {
        this(clazz, keys, true);
    }

    public GetCompareExchangeValuesOperation(Class<T> clazz, String[] keys, boolean materializeMetadata) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("Keys cannot be null or empty array");
        }
        _keys = keys;
        _materializeMetadata = materializeMetadata;
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
        _materializeMetadata = true;

        _keys = null;

    }

    @SuppressWarnings("unchecked")
    @Override
    public RavenCommand<Map<String, CompareExchangeValue<T>>> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new GetCompareExchangeValuesCommand(this, _materializeMetadata, conventions);
    }

    private static class GetCompareExchangeValuesCommand<T> extends RavenCommand<Map<String, CompareExchangeValue<T>>> {
        private final GetCompareExchangeValuesOperation<T> _operation;
        private final boolean _materializeMetadata;
        private final DocumentConventions _conventions;

        @SuppressWarnings("unchecked")
        public GetCompareExchangeValuesCommand(GetCompareExchangeValuesOperation<T> operation, boolean materializeMetadata, DocumentConventions conventions) {
            super((Class<Map<String, CompareExchangeValue<T>>>) (Class<?>)Map.class);
            _operation = operation;
            _materializeMetadata = materializeMetadata;
            _conventions = conventions;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
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

            String url = pathBuilder.toString();

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = CompareExchangeValueResultParser.getValues(_operation._clazz, response, _materializeMetadata, _conventions);
        }
    }
}
