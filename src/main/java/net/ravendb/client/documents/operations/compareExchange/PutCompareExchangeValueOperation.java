package net.ravendb.client.documents.operations.compareExchange;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.documents.session.EntityToJson;
import net.ravendb.client.documents.session.IMetadataDictionary;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.RaftIdGenerator;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPut;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PutCompareExchangeValueOperation<T> implements IOperation<CompareExchangeResult<T>> {

    private final String _key;
    private final T _value;
    private final long _index;
    private final IMetadataDictionary _metadata;

    public PutCompareExchangeValueOperation(String key, T value, long index) {
        this(key, value, index, null);
    }

    public PutCompareExchangeValueOperation(String key, T value, long index, IMetadataDictionary metadata) {
        _key = key;
        _value = value;
        _index = index;
        _metadata = metadata;
    }

    @Override
    public RavenCommand<CompareExchangeResult<T>> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new PutCompareExchangeValueCommand<>(_key, _value, _index, _metadata, conventions);
    }

    private static class PutCompareExchangeValueCommand<T> extends RavenCommand<CompareExchangeResult<T>> implements IRaftCommand {
        private final String _key;
        private final T _value;
        private final long _index;
        private final DocumentConventions _conventions;
        private final IMetadataDictionary _metadata;

        @SuppressWarnings("unchecked")
        public PutCompareExchangeValueCommand(String key, T value, long index, IMetadataDictionary metadata, DocumentConventions conventions) {
            super((Class<CompareExchangeResult<T>>) (Class<?>)CompareExchangeResult.class);

            if (StringUtils.isEmpty(key)) {
                throw new IllegalArgumentException("The key argument must have value");
            }

            if (index < 0) {
                throw new IllegalStateException("Index must be a non-negative number");
            }

            _key = key;
            _value = value;
            _index = index;
            _metadata = metadata;
            _conventions = ObjectUtils.firstNonNull(conventions, DocumentConventions.defaultConventions);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/cmpxchg?key="  + UrlUtils.escapeDataString(_key) + "&index=" + _index;

            Map<String, T> tuple = new HashMap<>();
            tuple.put(Constants.CompareExchange.OBJECT_FIELD_NAME, _value);

            ObjectNode json = EntityToJson.convertEntityToJson(tuple, _conventions, null, false);

            if (_metadata != null) {
                ObjectNode metadata = CompareExchangeSessionValue.prepareMetadataForPut(_key, _metadata, _conventions);
                json.set(Constants.Documents.Metadata.KEY, metadata);
            }

            HttpPut httpPut = new HttpPut(url);
            httpPut.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeTree(json);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON, _conventions));
            return httpPut;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = (CompareExchangeResult<T>) CompareExchangeResult.parseFromString(_value.getClass(), response, _conventions);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
