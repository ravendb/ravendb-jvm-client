package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class RemoveCompareExchangeOperation<T> implements IOperation<CmpXchgResult<T>> {

    private final Class<T> _clazz;
    private final String _key;
    private final long _index;

    public RemoveCompareExchangeOperation(Class<T> clazz, String key, long index) {
        _key = key;
        _index = index;
        _clazz = clazz;
    }

    @Override
    public RavenCommand<CmpXchgResult<T>> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new RemoveCompareExchangeCommand<T>(_clazz, _key, _index);
    }

    private static class RemoveCompareExchangeCommand<T> extends RavenCommand<CmpXchgResult<T>> {
        private final Class<T> _clazz;
        private final String _key;
        private final long _index;

        public RemoveCompareExchangeCommand(Class<T> clazz, String key, long index) {
            super((Class<CmpXchgResult<T>>) (Class<?>)CmpXchgResult.class);

            if (StringUtils.isEmpty(key)) {
                throw new IllegalArgumentException("The kye argument must have value");
            }

            _clazz = clazz;
            _key = key;
            _index = index;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/cmpxchg?key=" + _key + "&index=" + _index;

            return new HttpDelete();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = CmpXchgResult.parseFromString(_clazz, response);
        }
    }
}
