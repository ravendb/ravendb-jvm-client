package net.ravendb.client.documents.session.operations;

import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

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
        public HttpUriRequestBase createRequest(ServerNode node) {
            String pathBuilder = node.getUrl() +
                    "/databases/" +
                    node.getDatabase() +
                    "/revisions/count?" +
                    "&id=" +
                    urlEncode(_id);

            return new HttpGet(pathBuilder);
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
