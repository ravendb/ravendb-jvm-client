package net.ravendb.client.documents.operations.indexes;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexDefinition;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class IndexHasChangedOperation implements IMaintenanceOperation<Boolean> {

    private final IndexDefinition _definition;

    public IndexHasChangedOperation(IndexDefinition definition) {
        if (definition == null) {
            throw new IllegalArgumentException("IndexDefinition cannot be null");
        }

        _definition = definition;
    }

    @Override
    public RavenCommand<Boolean> getCommand(DocumentConventions conventions) {
        return new IndexHasChangedCommand(conventions, _definition);
    }

    private static class IndexHasChangedCommand extends RavenCommand<Boolean> {

        private final ObjectNode _definition;

        public IndexHasChangedCommand(DocumentConventions conventions, IndexDefinition definition) {
            super(Boolean.class);

            _definition = mapper.valueToTree(definition);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes/has-changed";

            HttpPost request = new HttpPost();

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.writeTree(_definition);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readTree(response).get("Changed").asBoolean();
        }
    }
}
