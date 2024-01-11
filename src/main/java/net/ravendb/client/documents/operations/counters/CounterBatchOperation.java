package net.ravendb.client.documents.operations.counters;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class CounterBatchOperation implements IOperation<CountersDetail> {

    private final CounterBatch _counterBatch;

    public CounterBatchOperation(CounterBatch counterBatch) {
        _counterBatch = counterBatch;
    }

    @Override
    public RavenCommand<CountersDetail> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new CounterBatchCommand(conventions, _counterBatch);
    }

    private static class CounterBatchCommand extends RavenCommand<CountersDetail> {
        private final DocumentConventions _conventions;
        private final CounterBatch _counterBatch;

        public CounterBatchCommand(DocumentConventions conventions, CounterBatch counterBatch) {
            super(CountersDetail.class);

            if (counterBatch == null) {
                throw new IllegalArgumentException("CounterBatch cannot be null");
            }
            _conventions = conventions;
            _counterBatch = counterBatch;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/counters";

            HttpPost request = new HttpPost(url);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    _counterBatch.serialize(generator);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }

            result = mapper.readValue(response, CountersDetail.class);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }
    }
}
