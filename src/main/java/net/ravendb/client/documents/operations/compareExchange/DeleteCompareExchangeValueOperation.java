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
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class DeleteCompareExchangeValueOperation<T> implements IOperation<CompareExchangeResult<T>> {

    private final Class<T> _clazz;
    private final String _key;
    private final long _index;

    public DeleteCompareExchangeValueOperation(Class<T> clazz, String key, long index) {
        _key = key;
        _index = index;
        _clazz = clazz;
    }

    @Override
    public RavenCommand<CompareExchangeResult<T>> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new RemoveCompareExchangeCommand<>(_clazz, _key, _index, conventions);
    }

    private static class RemoveCompareExchangeCommand<T> extends RavenCommand<CompareExchangeResult<T>> {
        private final Class<T> _clazz;
        private final String _key;
        private final long _index;
        private final DocumentConventions _conventions;

        @SuppressWarnings("unchecked")
        public RemoveCompareExchangeCommand(Class<T> clazz, String key, long index, DocumentConventions conventions) {
            super((Class<CompareExchangeResult<T>>) (Class<?>)CompareExchangeResult.class);

            if (StringUtils.isEmpty(key)) {
                throw new IllegalArgumentException("The kye argument must have value");
            }

            _clazz = clazz;
            _key = key;
            _index = index;
            _conventions = conventions;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/cmpxchg?key=" + UrlUtils.escapeDataString(_key) + "&index=" + _index;

            return new HttpDelete();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = CompareExchangeResult.parseFromString(_clazz, response, _conventions);
        }
    }
}
