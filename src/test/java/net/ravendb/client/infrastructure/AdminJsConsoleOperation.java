package net.ravendb.client.infrastructure;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.operations.IServerOperation;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class AdminJsConsoleOperation implements IServerOperation<JsonNode> {

    private final String _script;

    public AdminJsConsoleOperation(String script) {
        _script = script;
    }

    @Override
    public RavenCommand<JsonNode> getCommand(DocumentConventions conventions) {
        return new AdminJsConsoleCommand(conventions, _script);
    }

    private static class AdminJsConsoleCommand extends RavenCommand<JsonNode> {
        private final String _script;
        private final DocumentConventions _conventions;


        public AdminJsConsoleCommand(DocumentConventions conventions, String script) {
            super(JsonNode.class);
            _script = script;
            _conventions = conventions;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/admin/console?serverScript=true";

            HttpPost request = new HttpPost();

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeStartObject();
                    generator.writeFieldName("Script");
                    generator.writeString(_script);
                    generator.writeEndObject();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

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
