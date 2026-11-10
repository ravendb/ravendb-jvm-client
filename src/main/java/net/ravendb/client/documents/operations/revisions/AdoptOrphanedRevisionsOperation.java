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
 * Operation to adopt orphaned revisions, which are revisions that belong to a deleted document but are not shown
 * in the revisions bin, due to the absence of a "Delete Revision".
 */
public final class AdoptOrphanedRevisionsOperation implements IOperation<OperationIdResult> {

    private final Parameters _parameters;

    /**
     * Parameters for the {@link AdoptOrphanedRevisionsOperation}, specifying which collections
     * to adopt orphaned revisions from.
     */
    public static final class Parameters extends RevisionsOperationParameters {
    }

    public AdoptOrphanedRevisionsOperation() {
        this(new Parameters());
    }

    /**
     * Operation to adopt orphaned revisions, which are revisions that belong to a deleted document but are not shown
     * in the revisions bin, due to the absence of a "Delete Revision".
     * @param parameters The parameters specifying the collections for which orphaned revisions will be adopted.
     */
    public AdoptOrphanedRevisionsOperation(Parameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("parameters cannot be null");
        }
        _parameters = parameters;
    }

    @Override
    public RavenCommand<OperationIdResult> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new AdoptOrphanedRevisionsCommand(_parameters, conventions);
    }

    private static class AdoptOrphanedRevisionsCommand extends RavenCommand<OperationIdResult> {
        private final Parameters _parameters;
        private final DocumentConventions _conventions;

        public AdoptOrphanedRevisionsCommand(Parameters parameters, DocumentConventions conventions) {
            super(OperationIdResult.class);

            _parameters = parameters;
            _conventions = conventions;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/revisions/orphaned/adopt";

            selectedNodeTag = _parameters.nodeTagFor(node.getDatabase());

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
