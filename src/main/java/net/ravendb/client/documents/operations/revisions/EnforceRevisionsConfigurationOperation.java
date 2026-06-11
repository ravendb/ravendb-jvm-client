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

        /**
         * Indicates whether force-created revisions should be included.
         * For more information, see
         * <a href="https://ravendb.net/docs/article-page/7.2/csharp/document-extensions/revisions/overview#force-revision-creation">the documentation</a>.
         */
        private boolean includeForceCreated;
        private String[] collections;
        private Integer maxOpsPerSecond;

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

        /**
         * Limits the number of documents processed per second.
         * Use this to throttle the operation and reduce resource consumption on large datasets.
         * Default is {@code null} (no throttling).
         * @return Maximum operations per second
         */
        public Integer getMaxOpsPerSecond() {
            return maxOpsPerSecond;
        }

        public void setMaxOpsPerSecond(Integer maxOpsPerSecond) {
            if (maxOpsPerSecond != null && maxOpsPerSecond <= 0) {
                throw new IllegalStateException("MaxOpsPerSecond must be greater than 0");
            }
            this.maxOpsPerSecond = maxOpsPerSecond;
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
