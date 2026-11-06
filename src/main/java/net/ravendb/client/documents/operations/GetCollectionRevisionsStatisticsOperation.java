package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;

/**
 * Retrieves collection revisions statistics, providing in-depth information for each collection.
 * This includes the count of all revisions, and revisions count for each collection.
 */
public final class GetCollectionRevisionsStatisticsOperation implements IMaintenanceOperation<CollectionRevisionsStatistics> {

    public GetCollectionRevisionsStatisticsOperation() {
    }

    @Override
    public RavenCommand<CollectionRevisionsStatistics> getCommand(DocumentConventions conventions) {
        return new GetCollectionRevisionsStatisticsCommand();
    }

    private static class GetCollectionRevisionsStatisticsCommand extends RavenCommand<CollectionRevisionsStatistics> {

        public GetCollectionRevisionsStatisticsCommand() {
            super(CollectionRevisionsStatistics.class);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/revisions/collections/stats";

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
