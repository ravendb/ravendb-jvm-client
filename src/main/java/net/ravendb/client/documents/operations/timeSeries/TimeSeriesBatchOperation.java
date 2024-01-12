package net.ravendb.client.documents.operations.timeSeries;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidOperation;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

public class TimeSeriesBatchOperation implements IVoidOperation {
    private final String _documentId;
    private final TimeSeriesOperation _operation;

    public TimeSeriesBatchOperation(String documentId, TimeSeriesOperation operation) {
        if (documentId == null) {
            throw new IllegalArgumentException("Document id cannot be null");
        }
        if (operation == null) {
            throw new IllegalArgumentException("Operation cannot be null");
        }

        _documentId = documentId;
        _operation = operation;
    }

    @Override
    public VoidRavenCommand getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new TimeSeriesBatchCommand(_documentId, _operation, conventions);
    }

    private static class TimeSeriesBatchCommand extends VoidRavenCommand {
        private final String _documentId;
        private final TimeSeriesOperation _operation;
        private final DocumentConventions _conventions;

        public TimeSeriesBatchCommand(String documentId, TimeSeriesOperation operation, DocumentConventions conventions) {
            super();
            _documentId = documentId;
            _operation = operation;
            _conventions = conventions;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/timeseries?docId=" + urlEncode(_documentId);

            HttpPost request = new HttpPost(url);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    _operation.serialize(generator, _conventions);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }
    }
}
