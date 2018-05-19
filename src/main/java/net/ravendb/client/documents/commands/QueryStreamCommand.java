package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.core.JsonGenerator;
import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.queries.IndexQuery;
import net.ravendb.client.extensions.JsonExtensions;
import net.ravendb.client.http.*;
import net.ravendb.client.json.ContentProviderHttpEntity;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;

import java.io.IOException;

public class QueryStreamCommand extends RavenCommand<StreamResultResponse> {

    private final DocumentConventions _conventions;
    private final IndexQuery _indexQuery;

    public QueryStreamCommand(DocumentConventions conventions, IndexQuery query) {
        super(StreamResultResponse.class);

        if (conventions == null) {
            throw new IllegalArgumentException("Conventions cannot be null");
        }

        if (query == null) {
            throw new IllegalArgumentException("Query cannot be null");
        }

        _conventions = conventions;
        _indexQuery = query;

        responseType = RavenCommandResponseType.EMPTY;
    }

    @Override
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        HttpPost request = new HttpPost();

        request.setEntity(new ContentProviderHttpEntity(outputStream -> {
            try (JsonGenerator generator = mapper.getFactory().createGenerator(outputStream)) {
                JsonExtensions.writeIndexQuery(generator, _conventions, _indexQuery);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, ContentType.APPLICATION_JSON));

        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/streams/queries";

        return request;
    }

    @Override
    public ResponseDisposeHandling processResponse(HttpCache cache, CloseableHttpResponse response, String url) {
        try {
            StreamResultResponse streamResponse = new StreamResultResponse();
            streamResponse.setResponse(response);
            streamResponse.setStream(response.getEntity().getContent());
            setResult(streamResponse);

            return ResponseDisposeHandling.MANUALLY;
        } catch (IOException e) {
            throw new RuntimeException("Unable to process stream response: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean isReadRequest() {
        return true;
    }
}
