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
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetRevisionsOperation<T> implements IOperation<RevisionsResult<T>> {
    private final Class<T> _clazz;
    private final Parameters _parameters;

    public GetRevisionsOperation(Class<T> clazz, String id) {
        Parameters parameters = new Parameters();
        parameters.setId(id);

        _clazz = clazz;
        _parameters = parameters;
    }

    public GetRevisionsOperation(Class<T> clazz, String id, int start, int pageSize) {
        Parameters parameters = new Parameters();
        parameters.setId(id);
        parameters.setStart(start);
        parameters.setPageSize(pageSize);

        _clazz = clazz;
        _parameters = parameters;
    }

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

    public static class Parameters {
        private String id;
        private Integer start;
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
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            return _cmd.createRequest(node, url);
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

            List<T> results = new ArrayList<T>(revisions.size());
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
