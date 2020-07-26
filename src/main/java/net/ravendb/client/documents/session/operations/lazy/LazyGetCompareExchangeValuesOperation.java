package net.ravendb.client.documents.session.operations.lazy;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.commands.multiGet.GetRequest;
import net.ravendb.client.documents.commands.multiGet.GetResponse;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValueResultParser;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.ClusterTransactionOperationsBase;
import net.ravendb.client.exceptions.RavenException;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

public class LazyGetCompareExchangeValuesOperation<T> implements ILazyOperation {
    private final ClusterTransactionOperationsBase _clusterSession;
    private final Class<T> _clazz;
    private final DocumentConventions _conventions;
    private final String _startsWith;
    private final int _start;
    private final int _pageSize;
    private final String[] _keys;
    private Object result;
    private boolean requiresRetry;

    public LazyGetCompareExchangeValuesOperation(ClusterTransactionOperationsBase clusterSession,
                                                 Class<T> clazz, DocumentConventions conventions, String[] keys) {
        if (keys == null || keys.length == 0) {
            throw new IllegalArgumentException("Keys cannot be null or empty");
        }

        if (clusterSession == null) {
            throw new IllegalArgumentException("ClusterSession cannot be null");
        }

        if (conventions == null) {
            throw new IllegalArgumentException("Conventions cannot be null");
        }

        _clusterSession = clusterSession;
        _clazz = clazz;
        _conventions = conventions;
        _keys = keys;

        _start = 0;
        _pageSize = 0;
        _startsWith = null;
    }

    public LazyGetCompareExchangeValuesOperation(ClusterTransactionOperationsBase clusterSession,
                                                 Class<T> clazz, DocumentConventions conventions, String startsWith,
                                                 int start, int pageSize) {
        if (clusterSession == null) {
            throw new IllegalArgumentException("ClusterSession cannot be null");
        }

        if (conventions == null) {
            throw new IllegalArgumentException("Conventions cannot be null");
        }

        this._clazz = clazz;
        this._clusterSession = clusterSession;
        this._conventions = conventions;
        this._startsWith = startsWith;
        this._start = start;
        this._pageSize = pageSize;

        this._keys = null;
    }


    @Override
    public Object getResult() {
        return result;
    }

    @Override
    public QueryResult getQueryResult() {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public boolean isRequiresRetry() {
        return requiresRetry;
    }

    @Override
    public GetRequest createRequest() {
        StringBuilder pathBuilder = null;

        if (_keys != null) {
            for (String key : _keys) {
                if (_clusterSession.isTracked(key)) {
                    continue;
                }

                if (pathBuilder == null) {
                    pathBuilder = new StringBuilder("?");
                }

                pathBuilder.append("&key=").append(UrlUtils.escapeDataString(key));
            }
        } else {
            pathBuilder = new StringBuilder("?");

            if (StringUtils.isNotEmpty(_startsWith)) {
                pathBuilder.append("&startsWith=").append(UrlUtils.escapeDataString(_startsWith));
            }

            pathBuilder.append("&start=").append(_start);
            pathBuilder.append("&pageSize=").append(_pageSize);
        }

        if (pathBuilder == null) {
            result = _clusterSession.getCompareExchangeValuesFromSessionInternal(_clazz, _keys, new Reference<>());
            return null;
        }

        GetRequest request = new GetRequest();
        request.setUrl("/cmpxchg");
        request.setMethod("GET");
        request.setQuery(pathBuilder.toString());

        return request;
    }

    @Override
    public void handleResponse(GetResponse response) {
        if (response.isForceRetry()) {
            result = null;
            requiresRetry = true;
            return;
        }

        try {
            if (response.getResult() != null) {

                if (_clusterSession.getSession().noTracking) {
                    Map<String, CompareExchangeValue<T>> result = new TreeMap<>(String::compareToIgnoreCase);
                    for (Map.Entry<String, CompareExchangeValue<ObjectNode>> kvp : CompareExchangeValueResultParser.getValues(ObjectNode.class, response.getResult(), false, _conventions).entrySet()) {

                        if (kvp.getValue() == null) {
                            result.put(kvp.getKey(), _clusterSession.registerMissingCompareExchangeValue(kvp.getKey()).getValue(_clazz, _conventions));
                            continue;
                        }

                        result.put(kvp.getKey(), _clusterSession.registerCompareExchangeValue(kvp.getValue()).getValue(_clazz, _conventions));
                    }

                    this.result = result;
                    return;
                }

                for (Map.Entry<String, CompareExchangeValue<ObjectNode>> kvp :
                        CompareExchangeValueResultParser.getValues(ObjectNode.class, response.getResult(), false, _conventions).entrySet()) {
                    if (kvp.getValue() == null) {
                        continue;
                    }

                    _clusterSession.registerCompareExchangeValue(kvp.getValue());
                }
            }

            if (_keys != null) {
                for (String key : _keys) {
                    if (_clusterSession.isTracked(key)) {
                        continue;
                    }

                    _clusterSession.registerMissingCompareExchangeValue(key);
                }
            }


            result = _clusterSession.getCompareExchangeValuesFromSessionInternal(_clazz, _keys, new Reference<>());
        } catch (IOException e) {
            throw new RavenException("Unable to get compare exchange values: " + String.join(", ", _keys));
        }
    }
}
