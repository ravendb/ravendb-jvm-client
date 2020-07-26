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

import java.io.IOException;

public class LazyGetCompareExchangeValueOperation<T> implements ILazyOperation {

    private final ClusterTransactionOperationsBase _clusterSession;
    private final Class<T> _clazz;
    private final DocumentConventions _conventions;
    private final String _key;

    private Object result;
    private boolean requiresRetry;

    public LazyGetCompareExchangeValueOperation(ClusterTransactionOperationsBase clusterSession,
                                                Class<T> clazz, DocumentConventions conventions, String key) {
        if (clusterSession == null) {
            throw new IllegalArgumentException("Cluster Session cannot be null");
        }
        if (clazz == null) {
            throw new IllegalArgumentException("Clazz cannot be null");
        }
        if (conventions == null) {
            throw new IllegalArgumentException("Conventions cannot be null");
        }
        if (key == null) {
            throw new IllegalArgumentException("Key cannot be null");
        }

        _clusterSession = clusterSession;
        _clazz = clazz;
        _conventions = conventions;
        _key = key;
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
        if (_clusterSession.isTracked(_key)) {
            result  = _clusterSession.getCompareExchangeValueFromSessionInternal(_clazz, _key, new Reference<>());
            return null;
        }

        GetRequest request = new GetRequest();
        request.setUrl("/cmpxchg");
        request.setMethod("GET");
        String queryBuilder = "?key=" + UrlUtils.escapeDataString(_key);
        request.setQuery(queryBuilder);

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
                CompareExchangeValue<ObjectNode> value =
                        CompareExchangeValueResultParser.getValue(ObjectNode.class, response.getResult(), false, _conventions);

                if (_clusterSession.getSession().noTracking) {
                    if (value == null) {
                        result = _clusterSession.registerMissingCompareExchangeValue(_key).getValue(_clazz, _conventions);
                        return;
                    }

                    result = _clusterSession.registerCompareExchangeValue(value).getValue(_clazz, _conventions);
                    return;
                }

                if (value != null) {
                    _clusterSession.registerCompareExchangeValue(value);
                }
            }

            if (!_clusterSession.isTracked(_key)) {
                _clusterSession.registerMissingCompareExchangeValue(_key);
            }

            Reference<Boolean> notTrackedRef = new Reference<>();
            result = _clusterSession.getCompareExchangeValueFromSessionInternal(_clazz, _key, notTrackedRef);
        } catch (IOException e) {
            throw new RavenException("Unable to get compare exchange value: " + _key, e);
        }
    }
}
