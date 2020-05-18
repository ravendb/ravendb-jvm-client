package net.ravendb.client.infrastructure;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class AdminJsConsoleDatabaseOperation implements IMaintenanceOperation<JsonNode> {

    private final String _script;

    public AdminJsConsoleDatabaseOperation(String script) {
        _script = script;
    }

    @Override
    public RavenCommand<JsonNode> getCommand(DocumentConventions conventions) {
        return new AdminJsConsoleDatabaseCommand(_script);
    }

    private static class AdminJsConsoleDatabaseCommand extends RavenCommand<JsonNode> {
        private final String _script;

        public AdminJsConsoleDatabaseCommand(String script) {
            super(JsonNode.class);
            _script = script;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/console?database=" + urlEncode(node.getDatabase());

            HttpPost request = new HttpPost();

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeFieldName("Script");
                    generator.writeString(_script);
                    generator.writeEndObject();
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

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = mapper.readValue(response, ObjectNode.class).get("Result");
        }
    }
}
