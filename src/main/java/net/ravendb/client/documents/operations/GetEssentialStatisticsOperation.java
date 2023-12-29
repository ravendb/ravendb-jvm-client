package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import net.ravendb.client.primitives.Reference;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.IOException;

public class GetEssentialStatisticsOperation implements IMaintenanceOperation<EssentialDatabaseStatistics> {
    @Override
    public RavenCommand<EssentialDatabaseStatistics> getCommand(DocumentConventions conventions) {
        return new GetEssentialStatisticsCommand();
    }

    private static class GetEssentialStatisticsCommand extends RavenCommand<EssentialDatabaseStatistics> {
        public GetEssentialStatisticsCommand() {
            super(EssentialDatabaseStatistics.class);
        }

        @Override
        public HttpRequestBase createRequest(ServerNode node, Reference<String> url) {
            url.value = node.getUrl() + "/databases/" + node.getDatabase() + "/stats/essential";

            return new HttpGet();
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = mapper.readValue(response, resultClass);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }
}
