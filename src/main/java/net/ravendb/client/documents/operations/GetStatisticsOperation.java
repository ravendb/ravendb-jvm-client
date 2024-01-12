package net.ravendb.client.documents.operations;

import net.ravendb.client.documents.conventions.DocumentConventions;
import net.ravendb.client.http.RavenCommand;
import net.ravendb.client.http.ServerNode;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;

import java.io.IOException;
import java.time.Duration;

public class GetStatisticsOperation implements IMaintenanceOperation<DatabaseStatistics> {

    private final String _debugTag;
    private final String _nodeTag;

    public GetStatisticsOperation() {
        _debugTag = null;
        _nodeTag = null;
    }

    public GetStatisticsOperation(String debugTag) {
        this(debugTag, null);
    }

    public GetStatisticsOperation(String debugTag, String nodeTag) {
        this._debugTag = debugTag;
        this._nodeTag = nodeTag;
    }

    @Override
    public RavenCommand<DatabaseStatistics> getCommand(DocumentConventions conventions) {
        return new GetStatisticsCommand(_debugTag, _nodeTag);
    }

    public static class GetStatisticsCommand extends RavenCommand<DatabaseStatistics> {

        private String debugTag;

        public GetStatisticsCommand() {
            super(DatabaseStatistics.class);
        }

        public GetStatisticsCommand(String debugTag, String nodeTag) {
            super(DatabaseStatistics.class);
            this.debugTag = debugTag;
            this.selectedNodeTag = nodeTag;

            this.timeout = Duration.ofSeconds(15);
        }

        @Override
        public HttpUriRequestBase createRequest(ServerNode node) {
            String url = node.getUrl() + "/databases/" + node.getDatabase() + "/stats";
            if (debugTag != null) {
                url += "?" + debugTag;
            }

            return new HttpGet(url);
        }

        @Override
        public void setResponse(String response, boolean fromCache) throws IOException {
            result = mapper.readValue(response, DatabaseStatistics.class);
        }

        @Override
        public boolean isReadRequest() {
            return true;
        }
    }

}
