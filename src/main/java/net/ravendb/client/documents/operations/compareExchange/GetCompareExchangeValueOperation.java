package net.ravendb.client.documents.operations.compareExchange;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetCompareExchangeValueOperation<T> implements IOperation<CompareExchangeValue<T>> {

    private final String _key;
    private final boolean _materializeMetadata;
    private final Class<T> _clazz;

    public GetCompareExchangeValueOperation(Class<T> clazz, String key) {
        this(clazz, key, true);
    }

    public GetCompareExchangeValueOperation(Class<T> clazz, String key, boolean materializeMetadata) {
        _key = key;
        _clazz = clazz;
        _materializeMetadata = materializeMetadata;
    }

    @Override
    public RavenCommand<CompareExchangeValue<T>> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new GetCompareExchangeValueCommand<>(_clazz, _key, _materializeMetadata, conventions);
    }

    private static class GetCompareExchangeValueCommand<T> extends RavenCommand<CompareExchangeValue<T>> {
        private final String _key;
        private final Class<T> _clazz;
        private final boolean _materializeMetadata;
        private final DocumentConventions _conventions;

        @SuppressWarnings("unchecked")
        public GetCompareExchangeValueCommand(Class<T> clazz, String key, boolean materializeMetadata, DocumentConventions conventions) {
            super((Class<CompareExchangeValue<T>>) (Class<?>)CompareExchangeValue.class);
            if (StringUtils.isEmpty(key)) {
                throw new IllegalArgumentException("The key argument must have value");
            }

            _key = key;
            _clazz = clazz;
            _materializeMetadata = materializeMetadata;
            _conventions = conventions;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/cmpxchg?key=" + urlEncode(_key);
            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = CompareExchangeValueResultParser.getValue(_clazz, response, _materializeMetadata, _conventions);
        }
    }
}
