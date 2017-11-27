package net.ravendb.client.documents.session.operations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.Constants;
import net.ravendb.client.documents.commands.GetRevisionsCommand;
import net.ravendb.client.documents.session.DocumentInfo;
import net.ravendb.client.documents.session.InMemoryDocumentSessionOperations;

import java.util.ArrayList;
import java.util.List;

public class GetRevisionOperation {

    private final InMemoryDocumentSessionOperations _session;
    private final String _id;
    private final Integer _start;
    private final Integer _pageSize;

    private ArrayNode _result;

    public GetRevisionOperation(InMemoryDocumentSessionOperations session, String id, Integer start, Integer pageSize) {
        if (session == null) {
            throw new IllegalArgumentException("Session cannot be null");
        }
        if (id == null) {
            throw new IllegalArgumentException("Id cannot be null");
        }
        _session = session;
        _id = id;
        _start = start;
        _pageSize = pageSize;
    }

    public GetRevisionsCommand createRequest() {
        return new GetRevisionsCommand(_id, _start, _pageSize);
    }

    public void setResult(ArrayNode result) {
        _result = result;
    }

    public <T> List<T> complete(Class<T> clazz) {
        int resultsCount = _result.get("Results").size();
        ArrayList<T> results = new ArrayList<>(resultsCount);
        for (int i = 0; i < resultsCount; i++) {
            ObjectNode document = (ObjectNode) _result.get("Results").get(i);
            ObjectNode metadata = null;
            String id = null;
            if (document.has(Constants.Documents.Metadata.KEY)) {
                metadata = (ObjectNode) document.get(Constants.Documents.Metadata.KEY);
                JsonNode idNode = metadata.get(Constants.Documents.Metadata.ID);
                if (idNode != null) {
                    id = idNode.asText();
                }
            }

            T entity = (T)_session.convertToEntity(clazz, id, document);
            results.add(entity);

            String changeVector = null;
            if (metadata != null && metadata.has(Constants.Documents.Metadata.CHANGE_VECTOR)) {
                JsonNode changeVectorNode = metadata.get(Constants.Documents.Metadata.CHANGE_VECTOR);
                if (changeVectorNode != null) {
                    changeVector = changeVectorNode.asText();
                }
            }

            DocumentInfo documentInfo = new DocumentInfo();
            documentInfo.setId(id);
            documentInfo.setChangeVector(changeVector);
            documentInfo.setDocument(document);
            documentInfo.setEntity(entity);
            _session.documentsByEntity.put(entity, documentInfo);
        }

        return results;
    }
}
