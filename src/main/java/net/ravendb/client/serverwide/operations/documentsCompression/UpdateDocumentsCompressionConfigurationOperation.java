package net.ravendb.client.serverwide.operations.documentsCompression;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.IRaftCommand;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import net.ravendb.client.serverwide.DocumentsCompressionConfiguration;
import net.ravendb.client.util.RaftIdGenerator;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

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
        return new UpdateDocumentCompressionConfigurationCommand(_documentsCompressionConfiguration);
    }

    private static class UpdateDocumentCompressionConfigurationCommand extends RavenCommand<DocumentCompressionConfigurationResult> implements IRaftCommand {
        private DocumentsCompressionConfiguration _documentsCompressionConfiguration;

        public UpdateDocumentCompressionConfigurationCommand(DocumentsCompressionConfiguration configuration) {
            super(DocumentCompressionConfigurationResult.class);

            if (configuration == null) {
                throw new IllegalArgumentException("Configuration cannot be null");
            }

            _documentsCompressionConfiguration = configuration;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/documents-compression/config";

            HttpPost request = new HttpPost();

            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _documentsCompressionConfiguration);
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

            result = mapper.readValue(response, DocumentCompressionConfigurationResult.class);
        }

        @Override
        public String getRaftUniqueRequestId() {
            return RaftIdGenerator.newId();
        }
    }
}
