package net.ravendb.client.documents.commands;

import com.fasterxml.jackson.databind.node.ArrayNode;
import net.ravendb.client.http.HttpCache;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ResponseDisposeHandling;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

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
    public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
        url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/docs?id=" + urlEncode(_id);

        HttpGet request = new HttpGet();
        request.setHeader("If-None-Match", '"' + _changeVector + '"');

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
        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_NOT_MODIFIED) {
            return ResponseDisposeHandling.AUTOMATIC;
        }

        ResponseDisposeHandling result = super.processResponse(cache, response, url);
        throw new NotImplementedException(); //TODO: Result.ChangeVector = response.Headers.ETag.Tag;
        /* TODO
        this.result.setChangeVector(response.getFirstHeader("ETag").getValue()); //TODO: check me!
        return result; */
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
