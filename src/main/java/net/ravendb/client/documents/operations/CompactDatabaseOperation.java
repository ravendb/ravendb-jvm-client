package net.ravendb.client.documents.operations;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.CompactSettings;
import net.ravendb.client.serverwide.operations.IServerOperation;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class CompactDatabaseOperation implements IServerOperation<OperationIdResult> {

    private final CompactSettings _compactSettings;

    public CompactDatabaseOperation(CompactSettings compactSettings) {
        if (compactSettings == null) {
            throw new IllegalArgumentException("CompactSettings cannot be null");
        }

        _compactSettings = compactSettings;
    }

    @Override
    public RavenCommand<OperationIdResult> getCommand(DocumentConventions conventions) {
        return new CompactDatabaseCommand(conventions, _compactSettings);
    }

    private static class CompactDatabaseCommand extends RavenCommand<OperationIdResult> {
        private final ObjectNode _compactSettings;
        private final DocumentConventions _conventions;

        public CompactDatabaseCommand(DocumentConventions conventions, CompactSettings compactSettings) {
            super(OperationIdResult.class);

            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }

            if (compactSettings == null) {
                throw new IllegalArgumentException("CompactSettings cannot be null");
            }

            _conventions = conventions;
            _compactSettings = mapper.valueToTree(compactSettings);
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/admin/compact";

            HttpPost request = new HttpPost(url);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.writeTree(_compactSettings);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }
    }
}
