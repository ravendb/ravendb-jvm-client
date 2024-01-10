package net.ravendb.client.documents.operations.revisions;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.documents.operations.OperationIdResult;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;

public class EnforceRevisionsConfigurationOperation implements IOperation<OperationIdResult> {

    private final Parameters _parameters;

    public static class Parameters {
        private boolean includeForceCreated;
        private String[] collections;

        public boolean isIncludeForceCreated() {
            return includeForceCreated;
        }

        public void setIncludeForceCreated(boolean includeForceCreated) {
            this.includeForceCreated = includeForceCreated;
        }

        public String[] getCollections() {
            return collections;
        }

        public void setCollections(String[] collections) {
            this.collections = collections;
        }
    }

    public EnforceRevisionsConfigurationOperation() {
        this(new Parameters());
    }

    public EnforceRevisionsConfigurationOperation(Parameters parameters) {
        _parameters = parameters;
    }

    @Override
    public RavenCommand<OperationIdResult> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new EnforceRevisionsConfigurationCommand(_parameters, conventions);
    }

    private static class EnforceRevisionsConfigurationCommand extends RavenCommand<OperationIdResult> {
        private final Parameters _parameters;
        private final DocumentConventions _conventions;

        public EnforceRevisionsConfigurationCommand(Parameters parameters, DocumentConventions conventions) {
            super(OperationIdResult.class);

            _parameters = parameters;
            _conventions = conventions;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            StringBuilder pathBuilder = new StringBuilder(node.getUrl())
                    .append("/databases/")
                    .append(node.getDatabase())
                    .append("/admin/revisions/config/enforce");

            String url = pathBuilder.toString();

            HttpPost request = new HttpPost(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    generator.getCodec().writeValue(generator, _parameters);
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
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, OperationIdResult.class);
        }
    }
}
