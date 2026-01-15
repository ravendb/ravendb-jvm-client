package net.ravendb.client.documents.operations.compareExchange;

import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.util.RaftIdGenerator;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

/**
 * Operation to delete a compare exchange value.
 * A compare exchange is a key-value pair and serves as a distributed coordination mechanism
 * that ensures a consistent state across the cluster.
 *
 * @param <T> The type of the value associated with the compare exchange key.
 */
public class DeleteCompareExchangeValueOperation<T> implements IOperation<CompareExchangeResult<T>> {

    private final Class<T> _clazz;
    private final String _key;
    private final long _index;

    /**
     * Operation to delete a compare exchange value.
     * A compare exchange is a key-value pair and serves as a distributed coordination mechanism
     * that ensures a consistent state across the cluster.
     * Initializes a new instance of the {@link DeleteCompareExchangeValueOperation} class.
     * @param clazz The type of the value associated with the compare exchange key.
     * @param key The key of the compare exchange value to delete.
     * @param index The index of the compare exchange value to delete.
     *              The index is used for concurrency check; the operation will succeed only if it matches.
     */
    public DeleteCompareExchangeValueOperation(Class<T> clazz, String key, long index) {
        _key = key;
        _index = index;
        _clazz = clazz;
    }

    @Override
    public RavenCommand<CompareExchangeResult<T>> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new RemoveCompareExchangeCommand<>(_clazz, _key, _index, conventions);
    }

    private static class RemoveCompareExchangeCommand<T> extends RavenCommand<CompareExchangeResult<T>> implements IRaftCommand {
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
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/cmpxchg?key=" + UrlUtils.escapeDataString(_key) + "&index=" + _index;

            return new HttpDelete(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = CompareExchangeResult.parseFromString(_clazz, response, _conventions);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
