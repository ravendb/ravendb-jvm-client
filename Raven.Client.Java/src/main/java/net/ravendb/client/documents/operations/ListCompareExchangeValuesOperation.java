package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ListCompareExchangeValuesOperation implements IOperation<List<ListCompareExchangeValuesOperation.CompareExchangeItem>> {

    public static class CompareExchangeItem {
        private String key;
        private long index;
        private JsonNode value;

        public CompareExchangeItem(String key, long index, JsonNode value) {
            this.key = key;
            this.index = index;
            this.value = value;
        }

        public String getKey() {
            return key;
        }

        public long getIndex() {
            return index;
        }

        public JsonNode getValue() {
            return value;
        }
    }

    private final String _keyPrefix;
    private final Integer _page;
    private final Integer _size;

    public ListCompareExchangeValuesOperation(String keyPrefix) {
        this(keyPrefix, null, null);
    }

    public ListCompareExchangeValuesOperation(String keyPrefix, Integer page) {
        this(keyPrefix, page, null);
    }

    public ListCompareExchangeValuesOperation(String keyPrefix, Integer page, Integer size) {
        _keyPrefix = keyPrefix;
        _page = page;
        _size = size;
    }

    @Override
    public RavenCommand<List<CompareExchangeItem>> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new ListCompareExchangeValuesCommand(_keyPrefix, _page, _size);
    }

    private static class ListCompareExchangeValuesCommand extends RavenCommand<List<CompareExchangeItem>> {
        private final String _keyPrefix;
        private final Integer _page;
        private final Integer _size;

        @SuppressWarnings("unchecked")
        public ListCompareExchangeValuesCommand(String keyPrefix, Integer page, Integer size) {
            super((Class<List<CompareExchangeItem>>)(Class<?>)List.class);
            _keyPrefix = keyPrefix;
            _page = page;
            _size = size;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/cmpxchg/list?startsWith=" + _keyPrefix;

            if (_page != null) {
                url.value += "&start=" + _page;
            }

            if (_size != null) {
                url.value += "&pageSize=" + _size;
            }

            return new HttpGet();
        }

        private List<CompareExchangeItem> getResult(ArrayNode array) {
            if (array == null) {
                return Collections.emptyList();
            }

            List<CompareExchangeItem> results = new ArrayList<>();

            for (JsonNode jsonNode : array) {
                ObjectNode item = (ObjectNode) jsonNode;

                if (item == null || item.isNull()) {
                    continue;
                }

                long index = item.get("Index").asLong();
                JsonNode raw = item.get("Value");
                String key = item.get("Key").asText();

                JsonNode val = null;
                if (raw != null && raw.has("Object")) {
                    val = raw.get("Object");
                }

                results.add(new CompareExchangeItem(key, index, val));
            }

            return results;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            JsonNode jsonNode = mapper.readTree(response);
            JsonNode results = jsonNode.get("Results");
            result = getResult((ArrayNode) results);
        }
    }

}
