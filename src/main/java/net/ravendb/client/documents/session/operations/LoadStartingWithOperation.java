package net.ravendb.client.documents.session.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Defaults;
import net.ravendb.client.documents.commands.GetDocumentsCommand;
import net.ravendb.client.documents.commands.GetDocumentsResult;
import net.ravendb.client.documents.session.DocumentInfo;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class LoadStartingWithOperation {

    private static final Log logger = LogFactory.getLog(LoadStartingWithOperation.class);
    private final InMemoryDocumentSessionOperations _session;

    private String _startWith;
    private String _matches;
    private int _start;
    private int _pageSize;
    private String _exclude;
    private String _startAfter;

    private final List<String> _returnedIds = new ArrayList<>();
    private GetDocumentsResult _currentLoadResults;

    public LoadStartingWithOperation(InMemoryDocumentSessionOperations session) {
        _session = session;
    }

    public GetDocumentsCommand createRequest() {
        _session.incrementRequestCount();

        if (logger.isInfoEnabled()) {
            logger.info("Requesting documents with ids starting with '" + _startWith + "' from " + _session.storeIdentifier());
        }

        return new GetDocumentsCommand(_startWith, _startAfter, _matches, _exclude, _start, _pageSize, false);
    }

    public void withStartWith(String idPrefix) {
        withStartWith(idPrefix, null);
    }

    public void withStartWith(String idPrefix, String matches) {
        withStartWith(idPrefix, matches, 0);
    }
    public void withStartWith(String idPrefix, String matches, int start) {
        withStartWith(idPrefix, matches, start, 25);
    }
    public void withStartWith(String idPrefix, String matches, int start, int pageSize) {
        withStartWith(idPrefix, matches, start, pageSize, null);
    }

    public void withStartWith(String idPrefix, String matches, int start, int pageSize, String exclude) {
        withStartWith(idPrefix, matches, start, pageSize, exclude, null);
    }

    public void withStartWith(String idPrefix, String matches, int start, int pageSize, String exclude, String startAfter) {
        _startWith = idPrefix;
        _matches = matches;
        _start = start;
        _pageSize = pageSize;
        _exclude = exclude;
        _startAfter = startAfter;
    }

    public void setResult(GetDocumentsResult result) {
        if (_session.noTracking) {
            _currentLoadResults = result;
            return;
        }

        for (JsonNode document : result.getResults()) {
            if (document == null || document.isNull()) {
                continue;
            }
            DocumentInfo newDocumentInfo = DocumentInfo.getNewDocumentInfo((ObjectNode) document);
            _session.documentsById.add(newDocumentInfo);
            _returnedIds.add(newDocumentInfo.getId());
        }
    }

    @SuppressWarnings("unchecked")
    public <T> T[] getDocuments(Class<T> clazz) {
        int i = 0;
        T[] finalResults;

        if (_session.noTracking) {
            if (_currentLoadResults == null) {
                throw new IllegalStateException("Cannot execute getDocuments before operation execution");
            }

            finalResults = (T[]) Array.newInstance(clazz, _currentLoadResults.getResults().size());
            for (JsonNode document : _currentLoadResults.getResults()) {
                DocumentInfo newDocumentInfo = DocumentInfo.getNewDocumentInfo((ObjectNode) document);
                finalResults[i++] = _session.trackEntity(clazz, newDocumentInfo);
            }
        } else {
            finalResults = (T[]) Array.newInstance(clazz, _returnedIds.size());
            for (String id : _returnedIds) {
                finalResults[i++] = getDocument(clazz, id);
            }
        }

        return finalResults;
    }

    private <T> T getDocument(Class<T> clazz, String id) {
        if (id == null) {
            return Defaults.defaultValue(clazz);
        }

        if (_session.isDeleted(id)) {
            return Defaults.defaultValue(clazz);
        }

        DocumentInfo doc = _session.documentsById.getValue(id);
        if (doc != null) {
            return _session.trackEntity(clazz, doc);
        }

        return Defaults.defaultValue(clazz);
    }

}
