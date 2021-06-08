package net.ravendb.client.documents.session.operations;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetRevisionsCountOperation {
    private final String _docId;

    public GetRevisionsCountOperation(String docId) {
        _docId = docId;
    }

    public RavenCommand<Long> createRequest() {
        return new GetRevisionsCountCommand(_docId);
    }

    private static class GetRevisionsCountCommand extends RavenCommand<Long> {
        private final String _id;

        public GetRevisionsCountCommand(String id) {
            super(Long.class);

            if (id == null) {
                throw new IllegalArgumentException("Id cannot be null");
            }

            _id = id;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            String pathBuilder = node.getUrl() +
                    "/databases/" +
                    node.getDatabase() +
                    "/revisions/count?" +
                    "&id=" +
                    urlEncode(_id);
            url.value = pathBuilder;
            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                result = 0L;
                return;
            }

            result = mapper.readValue(response, DocumentRevisionsCount.class).getRevisionsCount();
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }

    public static class DocumentRevisionsCount {
        private long revisionsCount;

        public long getRevisionsCount() {
            return revisionsCount;
        }

        public void setRevisionsCount(long revisionsCount) {
            this.revisionsCount = revisionsCount;
        }
    }
}
