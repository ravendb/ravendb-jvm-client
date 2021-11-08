package net.ravendb.client.documents.operations.timeSeries;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidOperation;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

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
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/timeseries?docId=" + urlEncode(_documentId);

            HttpPost request = new HttpPost();

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    _operation.serialize(generator, _conventions);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

            return request;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }
    }
}
