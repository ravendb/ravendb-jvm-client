package net.ravendb.client.documents.operations.compareExchange;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValue;
import net.ravendb.client.documents.operations.compareExchange.CompareExchangeValueResultParser;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetCompareExchangeValueOperation<T> implements IOperation<CompareExchangeValue<T>> {

    private final String _key;
    private final Class<T> _clazz;

    public GetCompareExchangeValueOperation(Class<T> clazz, String key) {
        _key = key;
        _clazz = clazz;
    }

    @Override
    public RavenCommand<CompareExchangeValue<T>> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new GetCompareExchangeValueCommand<>(_clazz, _key, conventions);
    }

    private static class GetCompareExchangeValueCommand<T> extends RavenCommand<CompareExchangeValue<T>> {
        private final String _key;
        private final Class<T> _clazz;
        private final DocumentConventions _conventions;

        @SuppressWarnings("unchecked")
        public GetCompareExchangeValueCommand(Class<T> clazz, String key, DocumentConventions conventions) {
            super((Class<CompareExchangeValue<T>>) (Class<?>)CompareExchangeValue.class);
            if (StringUtils.isEmpty(key)) {
                throw new IllegalArgumentException("The key argument must have value");
            }

            _key = key;
            _clazz = clazz;
            _conventions = conventions;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/cmpxchg?key=" + urlEncode(_key);
            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = CompareExchangeValueResultParser.getValue(_clazz, response, _conventions);
        }
    }
}
