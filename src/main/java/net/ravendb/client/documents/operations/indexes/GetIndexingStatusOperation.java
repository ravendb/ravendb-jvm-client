package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexingStatus;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetIndexingStatusOperation implements IMaintenanceOperation<IndexingStatus> {
    @Override
    public RavenCommand<IndexingStatus> getCommand(DocumentConventions conventions) {
        return new GetIndexingStatusOperation.GetIndexingStatusCommand();
    }

    private static class GetIndexingStatusCommand extends RavenCommand<IndexingStatus> {
        public GetIndexingStatusCommand() {
            super(IndexingStatus.class);
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes/status";

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }
}

