package net.ravendb.client.documents.session.operations.lazy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import net.ravendb.client.documents.commands.GetRevisionsCommand;
import net.ravendb.client.documents.commands.multiGet.GetRequest;
import net.ravendb.client.documents.commands.multiGet.GetResponse;
import net.ravendb.client.documents.queries.QueryResult;
import net.ravendb.client.documents.session.operations.GetRevisionOperation;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.json.JsonArrayResult;
import net.ravendb.client.primitives.UseSharpEnum;

import java.io.IOException;

public class LazyRevisionOperation<T> implements ILazyOperation {
    private Class<T> _clazz;
    private final GetRevisionOperation _getRevisionOperation;
    private Mode _mode;

    private Object result;
    private QueryResult queryResult;
    private boolean requiresRetry;

    @UseSharpEnum
    public enum Mode {
        SINGLE,
        MULTI,
        MAP,
        LIST_OF_METADATA
    }

    public LazyRevisionOperation(Class<T> clazz, GetRevisionOperation getRevisionOperation, Mode mode) {
        _clazz = clazz;
        _getRevisionOperation = getRevisionOperation;
        _mode = mode;
    }

    @Override
    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    @Override
    public QueryResult getQueryResult() {
        return queryResult;
    }

    public void setQueryResult(QueryResult queryResult) {
        this.queryResult = queryResult;
    }

    @Override
    public boolean isRequiresRetry() {
        return requiresRetry;
    }

    public void setRequiresRetry(boolean requiresRetry) {
        this.requiresRetry = requiresRetry;
    }

    @Override
    public GetRequest createRequest() {
        GetRevisionsCommand getRevisionsCommand = _getRevisionOperation.getCommand();
        StringBuilder sb = new StringBuilder("?");
        getRevisionsCommand.getRequestQueryString(sb);
        GetRequest getRequest = new GetRequest();
        getRequest.setMethod("GET");
        getRequest.setUrl("/revisions");
        getRequest.setQuery(sb.toString());
        return getRequest;
    }

    @Override
    public void handleResponse(GetResponse response) {
        try {
            if (response.getResult() == null) {
                return;
            }
            ObjectNode responseAsNode = (ObjectNode) JsonExtensions.getDefaultMapper().readTree(response.getResult());
            ArrayNode jsonArray = (ArrayNode) responseAsNode.get("Results");

            JsonArrayResult jsonArrayResult = new JsonArrayResult();
            jsonArrayResult.setResults(jsonArray);
            _getRevisionOperation.setResult(jsonArrayResult);

            switch (_mode) {
                case SINGLE:
                    result = _getRevisionOperation.getRevision(_clazz);
                    break;
                case MULTI:
                    result = _getRevisionOperation.getRevisionsFor(_clazz);
                    break;
                case MAP:
                    result = _getRevisionOperation.getRevisions(_clazz);
                    break;
                case LIST_OF_METADATA:
                    result = _getRevisionOperation.getRevisionsMetadataFor();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid mode: " + _mode);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
