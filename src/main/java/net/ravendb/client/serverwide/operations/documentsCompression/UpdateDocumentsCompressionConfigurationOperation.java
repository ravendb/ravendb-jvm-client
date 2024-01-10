package net.ravendb.client.serverwide.operations.documentsCompression;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.serverwide.DocumentsCompressionConfiguration;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class UpdateDocumentsCompressionConfigurationOperation implements IMaintenanceOperation<DocumentCompressionConfigurationResult> {
    private final DocumentsCompressionConfiguration _documentsCompressionConfiguration;

    public UpdateDocumentsCompressionConfigurationOperation(DocumentsCompressionConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("Configuration cannot be null");
        }
        _documentsCompressionConfiguration = configuration;
    }

    @Override
    public RavenCommand<DocumentCompressionConfigurationResult> getCommand(DocumentConventions conventions) {
        return new UpdateDocumentCompressionConfigurationCommand(conventions, _documentsCompressionConfiguration);
    }

    private static class UpdateDocumentCompressionConfigurationCommand extends RavenCommand<DocumentCompressionConfigurationResult> implements IRaftCommand {
        private DocumentsCompressionConfiguration _documentsCompressionConfiguration;
        private final DocumentConventions _conventions;

        public UpdateDocumentCompressionConfigurationCommand(DocumentConventions conventions, DocumentsCompressionConfiguration configuration) {
            super(DocumentCompressionConfigurationResult.class);

            if (configuration == null) {
                throw new IllegalArgumentException("Configuration cannot be null");
            }

            _conventions = conventions;
            _documentsCompressionConfiguration = configuration;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/documents-compression/config";

            HttpPost request = new HttpPost(url);

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _documentsCompressionConfiguration);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, DocumentCompressionConfigurationResult.class);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
