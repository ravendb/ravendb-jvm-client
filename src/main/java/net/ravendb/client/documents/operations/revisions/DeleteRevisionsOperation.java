package net.ravendb.client.documents.operations.revisions;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpDelete;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.core5.http.ContentType;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * This operation allows deletion of revisions.
 * <p>
 * This is an Admin operation.
 */
public final class DeleteRevisionsOperation implements IMaintenanceOperation<DeleteRevisionsOperation.Result> {

    private final Parameters _parameters;

    /**
     * Deletes the revisions of the given documents (force-created revisions are kept).
     * @param documentIds A list of document IDs whose revisions you want to delete.
     */
    public DeleteRevisionsOperation(List<String> documentIds) {
        this(documentIds, false);
    }

    /**
     * Deletes the revisions of the given documents.
     * @param documentIds A list of document IDs whose revisions you want to delete.
     * @param removeForceCreatedRevisions Indicates whether to include force-created revisions in the deletion.
     */
    public DeleteRevisionsOperation(List<String> documentIds, boolean removeForceCreatedRevisions) {
        Parameters parameters = new Parameters();
        parameters.setDocumentIds(documentIds);
        parameters.setRemoveForceCreatedRevisions(removeForceCreatedRevisions);
        parameters.validate();
        _parameters = parameters;
    }

    /**
     * Deletes the revisions of the given document (force-created revisions are kept).
     * @param documentId The ID of the document whose revisions you want to delete.
     */
    public DeleteRevisionsOperation(String documentId) {
        this(documentId, false);
    }

    /**
     * Deletes the revisions of the given document.
     * @param documentId The ID of the document whose revisions you want to delete.
     * @param removeForceCreatedRevisions Indicates whether to include force-created revisions in the deletion.
     */
    public DeleteRevisionsOperation(String documentId, boolean removeForceCreatedRevisions) {
        Parameters parameters = new Parameters();
        parameters.setDocumentIds(Collections.singletonList(documentId));
        parameters.setRemoveForceCreatedRevisions(removeForceCreatedRevisions);
        parameters.validate();
        _parameters = parameters;
    }

    /**
     * Deletes the revisions of the given documents in the given date range (force-created revisions are kept).
     * @param documentIds A list of document IDs whose revisions you want to delete.
     * @param from The start of the date range (in UTC) for the revisions to delete.
     * @param to The end of the date range (in UTC) for the revisions to delete.
     */
    public DeleteRevisionsOperation(List<String> documentIds, Date from, Date to) {
        this(documentIds, from, to, false);
    }

    /**
     * Deletes the revisions of the given documents in the given date range.
     * @param documentIds A list of document IDs whose revisions you want to delete.
     * @param from The start of the date range (in UTC) for the revisions to delete.
     * @param to The end of the date range (in UTC) for the revisions to delete.
     * @param removeForceCreatedRevisions Indicates whether to include force-created revisions in the deletion.
     */
    public DeleteRevisionsOperation(List<String> documentIds, Date from, Date to, boolean removeForceCreatedRevisions) {
        Parameters parameters = new Parameters();
        parameters.setDocumentIds(documentIds);
        parameters.setFrom(from);
        parameters.setTo(to);
        parameters.setRemoveForceCreatedRevisions(removeForceCreatedRevisions);
        parameters.validate();
        _parameters = parameters;
    }

    /**
     * Deletes the revisions of the given document in the given date range (force-created revisions are kept).
     * @param documentId The ID of the document whose revisions you want to delete.
     * @param from The start of the date range (in UTC) for the revisions to delete.
     * @param to The end of the date range (in UTC) for the revisions to delete.
     */
    public DeleteRevisionsOperation(String documentId, Date from, Date to) {
        this(documentId, from, to, false);
    }

    /**
     * Deletes the revisions of the given document in the given date range.
     * @param documentId The ID of the document whose revisions you want to delete.
     * @param from The start of the date range (in UTC) for the revisions to delete.
     * @param to The end of the date range (in UTC) for the revisions to delete.
     * @param removeForceCreatedRevisions Indicates whether to include force-created revisions in the deletion.
     */
    public DeleteRevisionsOperation(String documentId, Date from, Date to, boolean removeForceCreatedRevisions) {
        Parameters parameters = new Parameters();
        parameters.setDocumentIds(Collections.singletonList(documentId));
        parameters.setFrom(from);
        parameters.setTo(to);
        parameters.setRemoveForceCreatedRevisions(removeForceCreatedRevisions);
        parameters.validate();
        _parameters = parameters;
    }

    /**
     * Deletes specific revisions of the given document (force-created revisions are kept).
     * @param documentId The ID of the document whose revisions you want to delete.
     * @param revisionsChangeVectors A list of change vectors corresponding to the revisions you want to delete.
     */
    public DeleteRevisionsOperation(String documentId, List<String> revisionsChangeVectors) {
        this(documentId, revisionsChangeVectors, false);
    }

    /**
     * Deletes specific revisions of the given document.
     * @param documentId The ID of the document whose revisions you want to delete.
     * @param revisionsChangeVectors A list of change vectors corresponding to the revisions you want to delete.
     * @param removeForceCreatedRevisions Indicates whether to include force-created revisions in the deletion.
     */
    public DeleteRevisionsOperation(String documentId, List<String> revisionsChangeVectors, boolean removeForceCreatedRevisions) {
        Parameters parameters = new Parameters();
        parameters.setDocumentIds(Collections.singletonList(documentId));
        parameters.setRevisionsChangeVectors(revisionsChangeVectors);
        parameters.setRemoveForceCreatedRevisions(removeForceCreatedRevisions);
        parameters.validate();
        _parameters = parameters;
    }

    @Override
    public RavenCommand<Result> getCommand(DocumentConventions conventions) {
        return new DeleteRevisionsCommand(conventions, _parameters);
    }

    static final class Parameters {
        private List<String> documentIds;
        private boolean removeForceCreatedRevisions;

        // Either!
        private List<String> revisionsChangeVectors;

        // Or!
        private Date from;
        private Date to;

        public List<String> getDocumentIds() {
            return documentIds;
        }

        public void setDocumentIds(List<String> documentIds) {
            this.documentIds = documentIds;
        }

        public boolean isRemoveForceCreatedRevisions() {
            return removeForceCreatedRevisions;
        }

        public void setRemoveForceCreatedRevisions(boolean removeForceCreatedRevisions) {
            this.removeForceCreatedRevisions = removeForceCreatedRevisions;
        }

        public List<String> getRevisionsChangeVectors() {
            return revisionsChangeVectors;
        }

        public void setRevisionsChangeVectors(List<String> revisionsChangeVectors) {
            this.revisionsChangeVectors = revisionsChangeVectors;
        }

        public Date getFrom() {
            return from;
        }

        public void setFrom(Date from) {
            this.from = from;
        }

        public Date getTo() {
            return to;
        }

        public void setTo(Date to) {
            this.to = to;
        }

        void validate() {
            if (documentIds == null || documentIds.isEmpty()) {
                throw new IllegalArgumentException("request 'DocumentIds' cannot be null or empty.");
            }

            for (String id : documentIds) {
                if (StringUtils.isEmpty(id)) {
                    throw new IllegalArgumentException("request 'DocumentIds' contains null or empty ids.");
                }
            }

            if (revisionsChangeVectors == null || revisionsChangeVectors.isEmpty()) {
                if (from != null && to != null && !from.before(to)) {
                    throw new IllegalArgumentException("'To' must be greater than 'From'.");
                }
            } else {
                if (documentIds.size() > 1) {
                    throw new IllegalArgumentException("The request must include exactly one document ID when deleting specific revisions by their change-vectors.");
                }

                if (from != null) {
                    throw new IllegalArgumentException("request 'From' cannot have a value when deleting specific revisions by their change-vectors.");
                }

                if (to != null) {
                    throw new IllegalArgumentException("request 'To' cannot have a value when deleting specific revisions by their change-vectors.");
                }

                for (String cv : revisionsChangeVectors) {
                    if (StringUtils.isEmpty(cv)) {
                        throw new IllegalArgumentException("request 'RevisionsChangeVectors' contains null or empty change-vectors.");
                    }
                }
            }
        }
    }

    public static class Result {
        private long totalDeletes;

        public long getTotalDeletes() {
            return totalDeletes;
        }

        public void setTotalDeletes(long totalDeletes) {
            this.totalDeletes = totalDeletes;
        }
    }

    private static class DeleteRevisionsCommand extends RavenCommand<Result> {
        private final DocumentConventions _conventions;
        private final Parameters _parameters;

        public DeleteRevisionsCommand(DocumentConventions conventions, Parameters parameters) {
            super(Result.class);

            if (conventions == null) {
                throw new IllegalArgumentException("conventions cannot be null");
            }
            if (parameters == null) {
                throw new IllegalArgumentException("parameters cannot be null");
            }

            _conventions = conventions;
            _parameters = parameters;
        }

        @Override
        public boolean isReadRequest() {
            return false;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/admin/revisions";

            HttpDelete request = new HttpDelete(url);
            request.setEntity(new ContentProviderHttpEntity(outputStream -> {
                try (JsonGenerator generator = createSafeJsonGenerator(outputStream)) {
                    ObjectNode parameters = mapper.valueToTree(_parameters);
                    generator.writeTree(parameters);
                }
            }, ContentType.APPLICATION_JSON, _conventions));

            return request;
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, Result.class);
        }
    }
}
