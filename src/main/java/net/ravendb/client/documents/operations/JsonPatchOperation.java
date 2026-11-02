package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.batches.JsonPatchDocument;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.util.UrlUtils;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

/**
 * Executes a collection of JsonPatch operations on a specific document in the database.
 * The operations are executed sequentially by the order of their addition to the document.
 */
public class JsonPatchOperation implements IOperation<JsonPatchResult> {

    private final String id;
    private final JsonPatchDocument jsonPatchDocument;

    /**
     * @param id the id of the document on which to execute the operations
     * @param jsonPatchDocument the JsonPatch operations to execute, in order
     */
    public JsonPatchOperation(String id, JsonPatchDocument jsonPatchDocument) {
        this.id = id;
        this.jsonPatchDocument = jsonPatchDocument;
    }

    public String getId() {
        return id;
    }

    public JsonPatchDocument getJsonPatchDocument() {
        return jsonPatchDocument;
    }

    @Override
    public RavenCommand<JsonPatchResult> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new JsonPatchCommand(conventions, id, jsonPatchDocument);
    }

    private static class JsonPatchCommand extends RavenCommand<JsonPatchResult> {
        private final DocumentConventions conventions;
        private final String id;
        private final JsonPatchDocument jsonPatchDocument;

        public JsonPatchCommand(DocumentConventions conventions, String id, JsonPatchDocument jsonPatchDocument) {
            super(JsonPatchResult.class);

            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }

            this.conventions = conventions;
            this.id = id;
            this.jsonPatchDocument = jsonPatchDocument;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/json-patch?id=" + UrlUtils.escapeDataString(id);

            HttpPatch request = new HttpPatch(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    jsonPatchDocument.serialize(generator, conventions.getEntityMapper());
                }
            }, ContentType.APPLICATION_JSON, conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }

            result = mapper.readValue(response, resultClass);
        }
    }
}
