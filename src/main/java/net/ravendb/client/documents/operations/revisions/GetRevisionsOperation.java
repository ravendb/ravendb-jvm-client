package net.ravendb.client.documents.operations.revisions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.IDocumentStore;
import net.ravendb.client.documents.commands.GetRevisionsCommand;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.operations.IOperation;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Operation to retrieve revisions of a document in the RavenDB database.
 *
 * @param <T> The type of the document for which the revisions are being retrieved.
 */
public class GetRevisionsOperation<T> implements IOperation<RevisionsResult<T>> {
    private final Class<T> _clazz;
    private final Parameters _parameters;

    /**
     * Operation to retrieve revisions of a document in the RavenDB database.
     * Initializes a new instance of the {@link GetRevisionsOperation}&lt;T&gt; class for the specified document ID.
     *
     * @param clazz The type of the document for which the revisions are being retrieved.
     * @param id   The ID of the document for which revisions are being retrieved.
     */
    public GetRevisionsOperation(Class<T> clazz, String id) {
        Parameters parameters = new Parameters();
        parameters.setId(id);

        _clazz = clazz;
        _parameters = parameters;
    }

    /**
     * Operation to retrieve revisions of a document in the RavenDB database.
     * Provides the ability to specify pagination through start index and page size.
     * Initializes a new instance of the {@link GetRevisionsOperation}&lt;T&gt; class for the specified document ID,
     * with pagination parameters to retrieve a specific subset of revisions.
     *
     * @param clazz      The type of the document for which the revisions are being retrieved.
     * @param id       The ID of the document for which revisions are being retrieved.
     * @param start    The starting index of the revisions to be retrieved.
     * @param pageSize The number of revisions to retrieve.
     */
    public GetRevisionsOperation(Class<T> clazz, String id, int start, int pageSize) {
        Parameters parameters = new Parameters();
        parameters.setId(id);
        parameters.setStart(start);
        parameters.setPageSize(pageSize);

        _clazz = clazz;
        _parameters = parameters;
    }

    /**
     * Operation to retrieve revisions of a document in the RavenDB database.
     * Provides the ability to specify pagination through start index and page size.
     * Initializes a new instance of the {@link GetRevisionsOperation}&lt;T&gt; class with the specified parameters.
     *
     * @param clazz        The type of the document for which the revisions are being retrieved.
     * @param parameters The parameters specifying the document ID and optional pagination settings.
     * @throws IllegalArgumentException Thrown when the {@code parameters} are null or invalid.
     */
    public GetRevisionsOperation(Class<T> clazz, Parameters parameters) {
        if (parameters == null) {
            throw new IllegalArgumentException("Parameters cannot be null");
        }

        parameters.validate();

        _clazz = clazz;
        _parameters = parameters;
    }

    @Override
    public RavenCommand<RevisionsResult<T>> getCommand(IDocumentStore store, DocumentConventions conventions, HttpCache cache) {
        return new GetRevisionsResultCommand<>(_clazz, _parameters.getId(), _parameters.getStart(), _parameters.getPageSize(), store.getConventions().getEntityMapper());
    }

    /**
     * Parameters for the {@link GetRevisionsOperation}&lt;T&gt; class, specifying the document ID
     * and optional pagination settings.
     */
    public static class Parameters {
        /**
         * The ID of the document for which revisions are being retrieved.
         */
        private String id;
        /**
         * Gets or sets the starting index of the revisions to be retrieved.
         * If <code>null</code>, all revisions will be retrieved from the beginning.
         */
        private Integer start;
        /**
         *  The number of revisions to retrieve. If <code>null</code>, all revisions from the starting index will be retrieved.
         */
        private Integer pageSize;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public Integer getStart() {
            return start;
        }

        public void setStart(Integer start) {
            this.start = start;
        }

        public Integer getPageSize() {
            return pageSize;
        }

        public void setPageSize(Integer pageSize) {
            this.pageSize = pageSize;
        }

        void validate() {
            if (StringUtils.isEmpty(id)) {
                throw new IllegalArgumentException("Id cannot be null");
            }
        }
    }

    private static class GetRevisionsResultCommand<T> extends RavenCommand<RevisionsResult<T>> {

        private final Class<T> _clazz;
        private final ObjectMapper _mapper;
        private final GetRevisionsCommand _cmd;

        @SuppressWarnings("unchecked")
        public GetRevisionsResultCommand(Class<T> clazz, String id, Integer start, Integer pageSize, ObjectMapper mapper) {
            super((Class<RevisionsResult<T>>)(Class<?>)RevisionsResult.class);
            _clazz = clazz;
            _mapper = mapper;
            _cmd = new GetRevisionsCommand(id, start, pageSize);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            return _cmd.createRequest(node);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                return;
            }
            ObjectNode responseNode = (ObjectNode) JsonExtensions.getDefaultMapper().readTree(response);
            if (!responseNode.has("Results")) {
                return;
            }

            ArrayNode revisions = (ArrayNode) responseNode.get("Results");
            int total = responseNode.get("TotalResults").intValue();

            List<T> results = new ArrayList<>(revisions.size());
            for (JsonNode revision : revisions) {
                if (revision == null || revision.isNull()) {
                    continue;
                }

                T entity = _mapper.treeToValue(revision, _clazz);
                results.add(entity);
            }

            RevisionsResult<T> result = new RevisionsResult<>();
            result.setResults(results);
            result.setTotalResults(total);

            this.result = result;
        }
    }
}
