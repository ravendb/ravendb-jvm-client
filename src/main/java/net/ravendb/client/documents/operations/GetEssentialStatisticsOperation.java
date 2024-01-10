package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.classic.methods.HttpGet;

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
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/stats/essential";

            return new HttpGet(url);
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
