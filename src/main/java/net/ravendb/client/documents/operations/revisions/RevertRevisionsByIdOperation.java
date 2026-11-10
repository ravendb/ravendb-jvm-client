package net.ravendb.client.documents.operations.revisions;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IVoidOperation;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.http.VoidRavenCommand;
import net.ravendb.client.json.ContentProviderHttpEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Client operation to revert documents to specified revisions based on provided change vectors.
 */
public class RevertRevisionsByIdOperation implements IVoidOperation {

    private final Map<String, String> _idToChangeVector;

    /**
     * Client operation to revert documents to specified revisions based on provided change vectors.
     * @param idToChangeVector A map where each key is the ID of the document to revert and each value is the
     *                         change vector of the revision to which the document should be reverted.
     */
    public RevertRevisionsByIdOperation(Map<String, String> idToChangeVector) {
        if (idToChangeVector == null) {
            throw new IllegalArgumentException("idToChangeVector cannot be null.");
        }

        if (idToChangeVector.isEmpty()) {
            throw new IllegalArgumentException("idToChangeVector must contain at least one item.");
        }

        _idToChangeVector = idToChangeVector;
    }

    /**
     * Client operation to revert a document to a specified revision based on provided change vector.
     * @param id The ID of the document to revert.
     * @param changeVector The change vector of the revision to which the document should be reverted.
     */
    public RevertRevisionsByIdOperation(String id, String changeVector) {
        if (StringUtils.isEmpty(id)) {
            throw new IllegalArgumentException("id cannot be null or empty.");
        }

        if (StringUtils.isEmpty(changeVector)) {
            throw new IllegalArgumentException("cv cannot be null or empty.");
        }

        _idToChangeVector = new HashMap<>(Collections.singletonMap(id, changeVector));
    }

    @Override
    public VoidRavenCommand getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new RevertRevisionsByIdCommand(conventions, _idToChangeVector);
    }

    private static class RevertRevisionsByIdCommand extends VoidRavenCommand {
        private final DocumentConventions _conventions;
        private final Map<String, String> _idToChangeVector;

        public RevertRevisionsByIdCommand(DocumentConventions conventions, Map<String, String> idToChangeVector) {
            _conventions = conventions;
            _idToChangeVector = idToChangeVector;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/revisions/revert/docs";

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeFieldName("IdToChangeVector");
                    generator.writeStartObject();
                    for (Map.Entry<String, String> kvp : _idToChangeVector.entrySet()) {
                        generator.writeStringField(kvp.getKey(), kvp.getValue());
                    }
                    generator.writeEndObject();
                    generator.writeEndObject();
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }
    }
}
