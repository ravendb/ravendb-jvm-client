package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.io.IOException;

/**
 * Retrieves detailed collection statistics, providing in-depth information for each collection.
 * This includes the count of documents, total size, and sizes of documents, revisions, and tombstones.
 */
public class GetDetailedCollectionStatisticsOperation implements IMaintenanceOperation<DetailedCollectionStatistics> {
    /**
     * Inherits documentation from {@link GetDetailedCollectionStatisticsOperation}.
     */
    public GetDetailedCollectionStatisticsOperation() {
    }

    @Override
    public RavenCommand<DetailedCollectionStatistics> getCommand(DocumentConventions conventions) {
        return new GetDetailedCollectionStatisticsCommand();
    }

    private static class GetDetailedCollectionStatisticsCommand extends RavenCommand<DetailedCollectionStatistics> {
        public GetDetailedCollectionStatisticsCommand() {
            super(DetailedCollectionStatistics.class);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/collections/stats/detailed";

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            if (response == null) {
                throwInvalidResponse();
            }

            result = mapper.readValue(response, resultClass);
        }
    }

}
