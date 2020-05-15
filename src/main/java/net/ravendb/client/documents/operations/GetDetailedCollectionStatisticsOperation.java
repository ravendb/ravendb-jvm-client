package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetDetailedCollectionStatisticsOperation implements IMaintenanceOperation<DetailedCollectionStatistics> {
    @Override
    public RavenCommand<DetailedCollectionStatistics> getCommand(DocumentConventions conventions) {
        return new GetDetailedCollectionStatisticsCommand(conventions);
    }

    private static class GetDetailedCollectionStatisticsCommand extends RavenCommand<DetailedCollectionStatistics> {
        private final DocumentConventions _conventions;

        public GetDetailedCollectionStatisticsCommand(DocumentConventions conventions) {
            super(DetailedCollectionStatistics.class);

            if (conventions == null) {
                throw new IllegalArgumentException("Conventions cannot be null");
            }

            _conventions = conventions;
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/collections/stats/detailed";

            return new HttpGet();
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
