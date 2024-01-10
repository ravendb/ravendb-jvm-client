package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.databind.node.ArrayNode;
import net.ravendb.client.Constants;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ResponseDisposeHandling;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;

import java.io.IOException;

public class ConditionalGetDocumentsCommand extends RavenCommand<ConditionalGetDocumentsCommand.ConditionalGetResult> {

    private final String _changeVector;
    private final String _id;

    public ConditionalGetDocumentsCommand(String id, String changeVector) {
        super(ConditionalGetDocumentsCommand.ConditionalGetResult.class);

        _changeVector = changeVector;
        _id = id;
    }

    @Override
    public HttpUriRequestBase createRequest(ServerNode node) {
        String url = node.getUrl() + "/databases/" + node.getDatabase() + "/docs?id=" + urlEncode(_id);

        HttpGet request = new HttpGet(url);
        request.setHeader(Constants.Headers.IF_NONE_MATCH, '"' + _changeVector + '"');

        return request;
    }

    @Override
    public void setResponse(String response, boolean fromCache) throws IOException {
        if (response == null) {
            result = null;
            return;
        }

        result = mapper.readValue(response, ConditionalGetResult.class);
    }

    @Override
    public ResponseDisposeHandling processResponse(HttpCache cache, CloseableHttpResponse response, String url) {
        if (response.getCode() == HttpStatus.SC_NOT_MODIFIED) {
            return ResponseDisposeHandling.AUTOMATIC;
        }

        ResponseDisposeHandling result = super.processResponse(cache, response, url);
        this.result.setChangeVector(response.getFirstHeader("ETag").getValue());
        return result;
    }

    /**
     * Here we explicitly do _NOT_ want to have caching
     * by the Request Executor, we want to manage it ourselves
     */
    @Override
    public boolean isReadRequest() {
        return false;
    }

    public static class ConditionalGetResult {
        private ArrayNode results;
        private String changeVector;

        public ArrayNode getResults() {
            return results;
        }

        public void setResults(ArrayNode results) {
            this.results = results;
        }

        public String getChangeVector() {
            return changeVector;
        }

        public void setChangeVector(String changeVector) {
            this.changeVector = changeVector;
        }
    }
}
