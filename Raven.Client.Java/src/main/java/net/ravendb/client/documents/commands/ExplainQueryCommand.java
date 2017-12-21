package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class ExplainQueryCommand extends RavenCommand<ExplainQueryCommand.ExplainQueryResult[]> {

    public static class ExplainQueryResult {
        private String index;
        private String reason;

        public String getIndex() {
            return index;
        }

        public void setIndex(String index) {
            this.index = index;
        }

        public String getReason() {
            return reason;
        }

        public void setReason(String reason) {
            this.reason = reason;
        }
    }

    private final DocumentConventions _conventions;
    private final IndexQuery _indexQuery;

    public ExplainQueryCommand(DocumentConventions conventions, IndexQuery indexQuery) {
        super(ExplainQueryCommand.ExplainQueryResult[].class);
        if (conventions == null) {
            throw new IllegalArgumentException("Conventions cannot be null");
        }

        if (indexQuery == null) {
            throw new IllegalArgumentException("IndexQuery cannot be null");
        }

        _conventions = conventions;
        _indexQuery = indexQuery;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        String path = node.getUrl() + "/databases/" +
                node.getDatabase() +
                "/queries?debug=explain";

        HttpPost request = new HttpPost();

        request.setEntity(new ContentProviderHttpEntity(outputStream -> {
            try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                JsonExtensions.writeIndexQuery(generator, _conventions, _indexQuery);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, ContentType.APPLICATION_JSON));

        url.value = path;
        return request;
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            result = null;
            return;
        }

        JsonNode jsonNode = mapper.readTree(response);
        JsonNode results = jsonNode.get("Results");
        if (results == null || results.isNull()) {
            throwInvalidResponse();
            return;
        }

        result = mapper.treeToValue(results, resultClass);
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
