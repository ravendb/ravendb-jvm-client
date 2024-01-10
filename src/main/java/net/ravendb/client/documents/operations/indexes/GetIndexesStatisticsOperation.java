package net.ravendb.client.documents.operations.indexes;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.documents.indexes.IndexStats;
import net.ravendb.client.documents.operations.IMaintenanceOperation;
import net.ravendb.client.documents.operations.ResultsResponse;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

public class GetIndexesStatisticsOperation implements IMaintenanceOperation<IndexStats[]> {
    @Override
    public RavenCommand<IndexStats[]> getCommand(DocumentConventions conventions) {
        return new GetIndexesStatisticsCommand();
    }

    private static class GetIndexesStatisticsCommand extends RavenCommand<IndexStats[]> {
        public GetIndexesStatisticsCommand() {
            super(IndexStats[].class);
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/indexes/stats";

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, ResultsResponse.GetIndexStatisticsResponse.class).getResults();
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }
}
