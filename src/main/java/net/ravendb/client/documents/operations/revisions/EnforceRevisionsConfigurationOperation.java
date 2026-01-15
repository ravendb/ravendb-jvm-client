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

/**
 * Operation to enforce the current revisions configuration on all existing revisions.
 * This applies the current revision configuration (rules), which are usually applied when a document is modified,
 * to all revisions at once.
 */
public final class EnforceRevisionsConfigurationOperation implements IOperation<OperationIdResult> {

    private final Parameters _parameters;
    /**
     * Parameters for the {@link EnforceRevisionsConfigurationOperation},
     * allowing specification of whether to include force-created revisions and target specific collections.
     */
    public final static class Parameters {
        /**
         * Gets or sets a value indicating whether to include force-created revisions.
         * For more information, visit <a href="https://ravendb.net/docs/article-page/6.2/csharp/document-extensions/revisions/overview#force-revision-creation">here</a>.
         */
        private boolean includeForceCreated;
        /**
         * Gets or sets the collections to which the enforcement should apply.
         * If {@code null}, the operation will apply to all collections in the database.
         */
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
    /**
     * Operation to enforce the current revisions configuration on all existing revisions.
     * This applies the current revision configuration (rules), which are usually applied when a document is modified,
     * to all revisions at once.
     * Initializes a new instance of {@link EnforceRevisionsConfigurationOperation} with default parameters.
     */
    public EnforceRevisionsConfigurationOperation() {
        this(new Parameters());
    }
    /**
     * Operation to enforce the current revisions configuration on all existing revisions.
     * This applies the current revision configuration (rules), which are usually applied when a document is modified,
     * to all revisions at once.
     * Initializes a new instance of {@link EnforceRevisionsConfigurationOperation} with the specified parameters.
     *
     * @param parameters The parameters specifying whether to include force-created revisions and the target collections.
     * @throws IllegalArgumentException Thrown when {@code parameters} is {@code null}.
     */
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
