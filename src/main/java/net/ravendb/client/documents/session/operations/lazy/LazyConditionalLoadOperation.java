package net.ravendb.client.documents.session.operations.lazy;

import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.commands.ConditionalGetDocumentsCommand;
import net.ravendb.client.documents.commands.multiGet.GetRequest;
import net.ravendb.client.documents.commands.multiGet.GetResponse;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.ConditionalLoadResult;
import net.ravendb.client.documents.session.DocumentInfo;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.util.UrlUtils;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.HttpStatus;

import java.io.IOException;

public class LazyConditionalLoadOperation<T> implements ILazyOperation {

    private final Class<T> _clazz;
    private final InMemoryDocumentSessionOperations _session;
    private final String _id;
    private final String _changeVector;

    public LazyConditionalLoadOperation(Class<T> clazz, String id, String changeVector, InMemoryDocumentSessionOperations session) {
        _clazz = clazz;
        _id = id;
        _changeVector = changeVector;
        _session = session;
    }

    @Override
    public GetRequest createRequest() {
        GetRequest request = new GetRequest();

        request.setUrl("/docs");
        request.setMethod("GET");
        request.setQuery("?id=" + UrlUtils.escapeDataString(_id));

        request.getHeaders().put("If-None-Match", '"' + _changeVector + '"');
        return request;
    }

    private Object result;
    private boolean requiresRetry;

    @Override
    public QueryResult getQueryResult() {
        throw new NotImplementedException();
    }

    @Override
    public Object getResult() {
        return result;
    }

    @Override
    public boolean isRequiresRetry() {
        return requiresRetry;
    }

    public void handleResponse(GetResponse response) {
        if (response.isForceRetry()) {
            result = null;
            requiresRetry = true;
            return;
        }

        switch (response.getStatusCode()) {
            case HttpStatus.SC_NOT_MODIFIED:
                result = ConditionalLoadResult.create(null, _changeVector); // value not changed
                return;
            case HttpStatus.SC_NOT_FOUND:
                _session.registerMissing(_id);
                result = ConditionalLoadResult.create(null, null);
                return;
        }

        try {
            if (response.getResult() != null) {
                String etag = response.getHeaders().get(Constants.Headers.ETAG);

                ConditionalGetDocumentsCommand.ConditionalGetResult res = JsonExtensions.getDefaultMapper().readValue(response.getResult(), ConditionalGetDocumentsCommand.ConditionalGetResult.class);
                DocumentInfo documentInfo = DocumentInfo.getNewDocumentInfo((ObjectNode) res.getResults().get(0));
                T r = _session.trackEntity(_clazz, documentInfo);

                result = ConditionalLoadResult.create(r, etag);
                return;
            }

            result = null;
            _session.registerMissing(_id);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
