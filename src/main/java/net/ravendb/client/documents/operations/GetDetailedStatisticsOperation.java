package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.classic.methods.HttpGet;

import java.io.IOException;

public class GetDetailedStatisticsOperation implements IMaintenanceOperation<DetailedDatabaseStatistics> {

    private final String _debugTag;

    public GetDetailedStatisticsOperation() {
        this(null);
    }

    public GetDetailedStatisticsOperation(String debugTag) {
        _debugTag = debugTag;
    }

    @Override
    public RavenCommand<DetailedDatabaseStatistics> getCommand(DocumentConventions conventions) {
         return new GetDetailedStatisticsCommand(_debugTag);
    }

    private static class GetDetailedStatisticsCommand extends RavenCommand<DetailedDatabaseStatistics> {
        private final String _debugTag;

        public GetDetailedStatisticsCommand(String debugTag) {
            super(DetailedDatabaseStatistics.class);
            _debugTag = debugTag;
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/stats/detailed";

            if (_debugTag != null) {
                url += "?" + _debugTag;
            }

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = mapper.readValue(response, DetailedDatabaseStatistics.class);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }
}
