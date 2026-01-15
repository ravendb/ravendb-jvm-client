package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpPatch;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;

import java.io.IOException;

/**
 * A class for executing JsonPatch operations on documents in the database
 */
public class JsonPatchOperation implements IOperation<JsonPatchResult> {
    /**
     * The id of the document on which to execute the patch operations
     */
    private String id;
    /**
     * A collection of JSON Patch operations to execute on the document.
     * <p>
     * The operations are executed sequentially in the order they are added.
     */
    private final JsonPatchDocument jsonPatchDocument;

    /**
     * Executes a collection of JSON Patch operations on a specific document in the database.
     * <p>
     * See the documentation:
     * https://ravendb.net/docs/article-page/latest/csharp/client-api/operations/patching/json-patch-syntax
     * </p>
     *
     * @param id
     *     The ID of the document on which the {@code jsonPatchDocument} operations will be executed.
     *
     * @param jsonPatchDocument
     *     A collection of JSON Patch operations to execute on the document.
     *     <p>
     *     The operations are executed sequentially in the order they are added.
     *     </p>
     */
    public JsonPatchOperation(String id, JsonPatchDocument jsonPatchDocument) {
        this.id = id;
        this.jsonPatchDocument = jsonPatchDocument;
    }

    @Override
    public RavenCommand<JsonPatchResult> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new JsonPatchCommand(conventions, id, jsonPatchDocument);
    }

    final class JsonPatchCommand extends RavenCommand<JsonPatchResult> {
        private final DocumentConventions conventions;
        private final String id;
        private final JsonPatchDocument jsonPatchDocument;

        public JsonPatchCommand(DocumentConventions conventions, String id, JsonPatchDocument jsonPatchDocument) {
            super(JsonPatchResult.class);
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
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/json-patch?id=" + urlEncode(this.id);
            HttpPatch request = new HttpPatch(url);

            ObjectMapper mapper = conventions.getEntityMapper(); // RavenDB Java client mapper

            ObjectNode root = mapper.createObjectNode();
            ArrayNode opsArray = mapper.createArrayNode();

            for (com.github.fge.jsonpatch.JsonPatchOperation op : jsonPatchDocument.getOperations()) {
                JsonNode opJson = mapper.valueToTree(op);
                opsArray.add(opJson);
            }
            root.set("Operations", opsArray);
            String json;
            try {
                json = mapper.writeValueAsString(root);
            } catch (JsonProcessingException e){
                throw new RuntimeException("Failed to serialize json patch operations", e);
            }

            StringEntity entity = new StringEntity(json, ContentType.APPLICATION_JSON);
            request.setEntity(entity);

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }
    }
}
